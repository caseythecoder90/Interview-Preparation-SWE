package urlshortener.service;

import urlshortener.codec.Base62Codec;
import urlshortener.model.ShortenedUrl;
import urlshortener.model.UrlStats;
import urlshortener.validation.UrlValidator;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ============================================================
 * COUNTER-BASED BASE62 URL SHORTENER — Primary Implementation
 * ============================================================
 *
 * THE interview answer for "Design a URL Shortener."
 *
 * Approach:
 *   1. Maintain a monotonically increasing counter (AtomicLong)
 *   2. On each shorten(), increment the counter
 *   3. Encode the counter value as Base62 → that's the short code
 *   4. Store the mapping: shortCode → ShortenedUrl
 *
 * Why this is the best approach:
 *   - GUARANTEED unique: each counter value is used exactly once
 *   - No collision detection needed (unlike hash or random)
 *   - O(1) shorten, O(1) resolve
 *   - Thread-safe via AtomicLong (lock-free CAS)
 *
 * Data Structures:
 *   - AtomicLong counter: generates unique IDs (thread-safe, lock-free)
 *   - ConcurrentHashMap<String, ShortenedUrl>: code → URL mapping
 *   - ConcurrentHashMap<String, String>: URL → code (deduplication)
 *
 * Clock abstraction:
 *   - Injected via constructor for testability
 *   - Tests can use Clock.fixed() to control time
 *   - Production uses Clock.systemUTC()
 */
public class CounterBase62Shortener implements UrlShortener {

    // Monotonically increasing counter — each value produces a unique code.
    // AtomicLong uses CAS (Compare-And-Swap), not locks — lock-free thread safety.
    private final AtomicLong counter;

    // Short code → ShortenedUrl mapping (primary lookup)
    private final ConcurrentHashMap<String, ShortenedUrl> codeToUrl;

    // Original URL → short code mapping (deduplication)
    // Prevents creating multiple codes for the same URL
    private final ConcurrentHashMap<String, String> urlToCode;

    // Injected clock for testability
    private final Clock clock;

    // Minimum code length (zero-padded for uniform URLs)
    private final int minCodeLength;

    /**
     * Creates a counter-based shortener.
     *
     * @param startCounter  initial counter value (allows pre-seeding for distributed systems)
     * @param minCodeLength minimum Base62 code length (padded with leading zeros)
     * @param clock         clock for time operations (inject Clock.fixed() for tests)
     */
    public CounterBase62Shortener(long startCounter, int minCodeLength, Clock clock) {
        this.counter = new AtomicLong(startCounter);
        this.minCodeLength = minCodeLength;
        this.clock = clock;
        this.codeToUrl = new ConcurrentHashMap<>();
        this.urlToCode = new ConcurrentHashMap<>();
    }

    /** Convenience: start at 1, 6-char codes, system clock. */
    public CounterBase62Shortener() {
        this(1, 6, Clock.systemUTC());
    }

    /** Convenience: custom clock for testing. */
    public CounterBase62Shortener(Clock clock) {
        this(1, 6, clock);
    }

    /**
     * Shortens a URL using the counter-based approach.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Validate and normalize the URL</li>
     *   <li>Check deduplication map — if URL was already shortened, return existing code</li>
     *   <li>Atomically increment counter → unique ID</li>
     *   <li>Encode ID as Base62 → short code</li>
     *   <li>Store both mappings (code→URL and URL→code)</li>
     * </ol>
     *
     * <p>Time Complexity: O(1) — atomic increment + two ConcurrentHashMap puts
     * <p>Space Complexity: O(1) per URL — one entry in each map
     */
    @Override
    public String shorten(String originalUrl) {
        return shorten(originalUrl, null);
    }

    @Override
    public String shorten(String originalUrl, Duration ttl) {
        // 1. Validate
        String normalized = UrlValidator.normalize(originalUrl);

        // 2. Deduplication: check if this URL was already shortened
        // Note: in a distributed system, this would be a distributed cache lookup
        String existingCode = urlToCode.get(normalized);
        if (existingCode != null) {
            ShortenedUrl existing = codeToUrl.get(existingCode);
            // Return existing code only if it hasn't expired
            if (existing != null && !existing.isExpired(Instant.now(clock))) {
                return existingCode;
            }
            // Expired — fall through to create a new one
        }

        // 3. Generate unique ID via atomic counter (lock-free, thread-safe)
        long id = counter.getAndIncrement();

        // 4. Encode to Base62
        String shortCode = Base62Codec.encode(id, minCodeLength);

        // 5. Create the ShortenedUrl and store mappings
        Instant now = Instant.now(clock);
        ShortenedUrl entry = new ShortenedUrl(shortCode, normalized, now, ttl);

        codeToUrl.put(shortCode, entry);
        urlToCode.put(normalized, shortCode);

        return shortCode;
    }

    /**
     * Resolves a short code to the original URL.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Look up the code in codeToUrl map — O(1)</li>
     *   <li>Check expiration (lazy eviction)</li>
     *   <li>Record the hit (atomic increment)</li>
     *   <li>Return the original URL</li>
     * </ol>
     *
     * <p>Time Complexity: O(1) — single HashMap lookup
     * <p>Space Complexity: O(1)
     */
    @Override
    public Optional<String> resolve(String shortCode) {
        ShortenedUrl entry = codeToUrl.get(shortCode);
        if (entry == null) return Optional.empty();

        // Lazy expiration check — don't return expired entries,
        // but also clean them up when we encounter them
        Instant now = Instant.now(clock);
        if (entry.isExpired(now)) {
            codeToUrl.remove(shortCode);
            urlToCode.remove(entry.originalUrl());
            return Optional.empty();
        }

        entry.recordHit(now);
        return Optional.of(entry.originalUrl());
    }

    @Override
    public Optional<UrlStats> getStats(String shortCode) {
        ShortenedUrl entry = codeToUrl.get(shortCode);
        if (entry == null) return Optional.empty();
        return Optional.of(UrlStats.from(entry, Instant.now(clock)));
    }

    @Override
    public boolean delete(String shortCode) {
        ShortenedUrl removed = codeToUrl.remove(shortCode);
        if (removed != null) {
            urlToCode.remove(removed.originalUrl());
            return true;
        }
        return false;
    }

    @Override
    public int size() {
        return codeToUrl.size();
    }

    @Override
    public String strategyName() {
        return "Counter + Base62";
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== Counter Base62 URL Shortener Demo ===\n");

        CounterBase62Shortener shortener = new CounterBase62Shortener();

        // Shorten some URLs
        String[] urls = {
                "https://www.google.com/search?q=java",
                "https://github.com/user/repo",
                "https://example.com/very/long/path/to/page",
                "https://www.google.com/search?q=java"  // duplicate
        };

        System.out.println("--- Shorten URLs ---");
        for (String url : urls) {
            String code = shortener.shorten(url);
            System.out.printf("  %s → %s%n", url, code);
        }

        // Note: the duplicate URL returns the same code!
        System.out.println("\n--- Resolve ---");
        String code = shortener.shorten("https://example.com");
        System.out.println("  Code: " + code);
        System.out.println("  Resolved: " + shortener.resolve(code).orElse("NOT FOUND"));

        // Resolve a few times for hit count
        shortener.resolve(code);
        shortener.resolve(code);
        System.out.println("  Stats: " + shortener.getStats(code).orElse(null));

        // Non-existent code
        System.out.println("  Unknown: " + shortener.resolve("ZZZZZZ").orElse("NOT FOUND"));

        System.out.println("\n  Total shortened: " + shortener.size());
        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   shorten(): O(1) — atomic increment + hash map put        │
 * │   resolve(): O(1) — hash map get                           │
 * │   delete():  O(1) — hash map remove                        │
 * │   Space:     O(n) — two maps, n entries each               │
 * │                                                             │
 * │ WHY COUNTER-BASED IS THE BEST INTERVIEW ANSWER             │
 * │   1. Zero collisions — counter values are unique by def.   │
 * │   2. No retry logic — unlike hash or random approaches     │
 * │   3. Thread-safe — AtomicLong uses CAS, not locks          │
 * │   4. Predictable performance — always O(1), no retries     │
 * │                                                             │
 * │ SCALING TO BILLIONS — SYSTEM DESIGN TALKING POINTS         │
 * │                                                             │
 * │ Q: "How would you scale this to multiple servers?"         │
 * │ A: "Counter range partitioning:                            │
 * │    Server 1: counters 1 to 1B                              │
 * │    Server 2: counters 1B+1 to 2B                           │
 * │    Each server gets a pre-allocated range from ZooKeeper   │
 * │    or a centralized ID generator (Twitter Snowflake).      │
 * │    No coordination needed within a range → fully parallel."│
 * │                                                             │
 * │ Q: "How would you store billions of URLs?"                │
 * │ A: "Distributed key-value store:                           │
 * │    - DynamoDB / Cassandra for the code→URL mapping         │
 * │    - Shard by short code (consistent hashing)              │
 * │    - Read replicas for high read throughput                 │
 * │    - Redis/Memcached as a caching layer for hot URLs       │
 * │    - 80/20 rule: 20% of URLs get 80% of traffic"          │
 * │                                                             │
 * │ Q: "How would you handle 100K reads/sec?"                 │
 * │ A: "Multi-layer caching:                                   │
 * │    1. CDN cache for the redirect (TTL = 5 min)            │
 * │    2. Redis cache for code→URL lookup                     │
 * │    3. Database for cache misses                             │
 * │    Most reads hit the cache → < 1ms latency"              │
 * │                                                             │
 * │ Q: "Sequential codes are predictable/guessable."          │
 * │ A: "XOR the counter with a secret before encoding:        │
 * │    code = base62(counter XOR secret). Still bijective     │
 * │    (1-to-1) so no collisions, but codes look random.      │
 * │    Better: use a Feistel cipher for stronger shuffling."   │
 * │                                                             │
 * │ DEDUPLICATION TRADEOFF                                      │
 * │   With (our approach):                                      │
 * │     ✓ Same URL → same code → consistent analytics         │
 * │     ✗ Extra reverse map (URL→code) doubles memory         │
 * │     ✗ normalize() must be deterministic                    │
 * │   Without:                                                  │
 * │     ✓ Simpler, less memory                                │
 * │     ✗ Same URL gets multiple codes → fragmented analytics │
 * └─────────────────────────────────────────────────────────────┘
 */

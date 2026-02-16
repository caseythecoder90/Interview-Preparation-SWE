package urlshortener.test;

import urlshortener.expiration.ExpirationReaper;
import urlshortener.model.ShortenedUrl;
import urlshortener.model.UrlStats;
import urlshortener.service.CounterBase62Shortener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * FULL LIFECYCLE INTEGRATION TEST
 * ============================================================
 *
 * Tests the complete URL shortener flow:
 *   1. Shorten a URL
 *   2. Resolve it multiple times
 *   3. Check analytics
 *   4. Test expiration (TTL)
 *   5. Test reaper cleanup
 *   6. Verify deletion
 */
public class FullLifecycleTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testFullLifecycle() {
        System.out.println("\n=== Test 1: Full Lifecycle ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        // Step 1: Shorten
        String code = s.shorten("https://www.example.com/long/path/to/resource?q=test");
        check("Shorten returns code", code != null && code.length() >= 6);
        check("Size is 1", s.size() == 1);

        // Step 2: Resolve multiple times
        for (int i = 0; i < 5; i++) {
            Optional<String> url = s.resolve(code);
            check("Resolve #" + (i + 1) + " succeeds", url.isPresent());
        }

        // Step 3: Check analytics
        Optional<UrlStats> stats = s.getStats(code);
        check("Stats available", stats.isPresent());
        check("Hit count is 5", stats.get().hitCount() == 5);
        check("Not expired", !stats.get().isExpired());

        // Step 4: Delete
        check("Delete succeeds", s.delete(code));
        check("Resolve after delete fails", s.resolve(code).isEmpty());
        check("Size is 0", s.size() == 0);
    }

    static void testDeduplicationLifecycle() {
        System.out.println("\n=== Test 2: Deduplication Lifecycle ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        // Shorten same URL with different formatting
        String c1 = s.shorten("https://example.com");
        String c2 = s.shorten("HTTPS://EXAMPLE.COM");
        String c3 = s.shorten("https://example.com/");
        String c4 = s.shorten("https://example.com:443");

        check("All 4 variants → same code", c1.equals(c2) && c2.equals(c3) && c3.equals(c4));
        check("Size is 1 (not 4)", s.size() == 1);

        // Resolve any code should work
        check("Resolve works", s.resolve(c1).isPresent());
    }

    static void testMultipleUrlsLifecycle() {
        System.out.println("\n=== Test 3: Multiple URLs Lifecycle ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String[] urls = {
                "https://google.com",
                "https://github.com",
                "https://stackoverflow.com",
                "https://example.com"
        };

        String[] codes = new String[urls.length];
        for (int i = 0; i < urls.length; i++) {
            codes[i] = s.shorten(urls[i]);
        }

        check("4 unique codes", s.size() == 4);

        // Resolve each
        for (int i = 0; i < urls.length; i++) {
            Optional<String> resolved = s.resolve(codes[i]);
            check("Resolve " + codes[i] + " → correct URL",
                    resolved.isPresent() && resolved.get().contains(urls[i].split("://")[1]));
        }

        // Delete one
        s.delete(codes[0]);
        check("After deleting first, size is 3", s.size() == 3);
        check("Deleted code returns empty", s.resolve(codes[0]).isEmpty());
        check("Others still work", s.resolve(codes[1]).isPresent());
    }

    static void testExpirationReaper() throws InterruptedException {
        System.out.println("\n=== Test 4: Expiration Reaper ===");

        // Directly test the reaper with a manual map
        ConcurrentHashMap<String, ShortenedUrl> map = new ConcurrentHashMap<>();
        Clock clock = Clock.systemUTC();

        Instant now = Instant.now();
        // 2 entries with 1-second TTL, 1 with no TTL
        map.put("expire1", new ShortenedUrl("expire1", "https://a.com", now, Duration.ofSeconds(1)));
        map.put("expire2", new ShortenedUrl("expire2", "https://b.com", now, Duration.ofSeconds(1)));
        map.put("persist", new ShortenedUrl("persist", "https://c.com", now));

        check("3 entries before", map.size() == 3);

        // Wait for expiration
        Thread.sleep(1200);

        ExpirationReaper reaper = new ExpirationReaper(map, null, clock, 300);
        int reaped = reaper.sweep();

        check("Reaped 2 expired entries", reaped == 2);
        check("1 entry remains", map.size() == 1);
        check("Persistent entry survives", map.containsKey("persist"));
        check("Total reaped counter is 2", reaper.totalReaped() == 2);

        reaper.shutdown();
    }

    static void testStatsTracking() {
        System.out.println("\n=== Test 5: Stats Tracking ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String code = s.shorten("https://example.com");

        // Check initial stats
        UrlStats initial = s.getStats(code).orElseThrow();
        check("Initial hit count is 0", initial.hitCount() == 0);
        check("Created time is set", initial.createdAt() != null);

        // Access 10 times
        for (int i = 0; i < 10; i++) {
            s.resolve(code);
        }

        UrlStats after = s.getStats(code).orElseThrow();
        check("Hit count is 10 after 10 resolves", after.hitCount() == 10);
        check("Last accessed is after creation",
                after.lastAccessed().isAfter(after.createdAt()) ||
                        after.lastAccessed().equals(after.createdAt()));
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Full Lifecycle Integration — Test Suite    ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testFullLifecycle();
        testDeduplicationLifecycle();
        testMultipleUrlsLifecycle();
        testExpirationReaper();
        testStatsTracking();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("══════════════════════════════════════════════");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — URL SHORTENER SYSTEM DESIGN              │
 * │                                                             │
 * │ SCALING TO BILLIONS (30-min system design discussion)      │
 * │                                                             │
 * │ READ PATH (redirect — 100:1 read:write ratio):            │
 * │   Client → CDN (cache 301) → Load Balancer →              │
 * │   App Server → Redis Cache → Database (fallback)           │
 * │   Target: < 10ms p99 latency for redirects                │
 * │                                                             │
 * │ WRITE PATH (shorten):                                      │
 * │   Client → Load Balancer → App Server →                   │
 * │   ID Generator → Database write → Cache write              │
 * │   Target: < 50ms p99 latency                              │
 * │                                                             │
 * │ DATABASE:                                                   │
 * │   Primary: DynamoDB or Cassandra (NoSQL, write-optimized)  │
 * │   Schema: {shortCode (PK), originalUrl, created, expires}  │
 * │   Shard by shortCode hash for even distribution            │
 * │                                                             │
 * │ CACHING:                                                    │
 * │   Redis cluster: code → URL (most reads hit cache)         │
 * │   TTL = 24h on cache entries (balance freshness vs load)   │
 * │   Hot URLs (top 20%): 80% of traffic → cache hit rate ~99% │
 * │                                                             │
 * │ ID GENERATION AT SCALE:                                     │
 * │   Option 1: Twitter Snowflake (64-bit IDs, timestamp+node) │
 * │   Option 2: Counter range pre-allocation from ZooKeeper    │
 * │   Option 3: UUID-based (no coordination, but longer codes) │
 * │                                                             │
 * │ ANALYTICS:                                                  │
 * │   Real-time: Kafka → Flink → dashboard                    │
 * │   Batch: click events → S3 → Spark → analytics DB         │
 * │   Store: timestamp, geo, referrer, device per click        │
 * │                                                             │
 * │ CAPACITY ESTIMATION (back-of-envelope):                    │
 * │   100M new URLs/month × 12 months × 5 years = 6B URLs    │
 * │   62^7 = 3.5T codes → 7-char codes are sufficient         │
 * │   Each URL entry ≈ 500 bytes → 6B × 500B = 3TB storage   │
 * │   Read QPS: 10B redirects/month ÷ 2.5M sec = 4K QPS      │
 * │   Peak: 4K × 5 = 20K QPS → easily handled by Redis cache │
 * └─────────────────────────────────────────────────────────────┘
 */

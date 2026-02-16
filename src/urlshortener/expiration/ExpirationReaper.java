package urlshortener.expiration;

import urlshortener.model.ShortenedUrl;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * EXPIRATION REAPER — Background TTL Cleanup
 * ============================================================
 *
 * Two-phase expiration strategy:
 *
 * 1. LAZY EVICTION (on access):
 *    When resolve() finds an expired entry, it removes it immediately.
 *    This handles hot entries efficiently — no wasted work checking
 *    entries nobody is accessing.
 *    → Already implemented in each shortener's resolve() method
 *
 * 2. ACTIVE EVICTION (background sweep):
 *    A scheduled thread periodically scans ALL entries and removes
 *    expired ones. This catches cold entries that nobody accesses
 *    but still consume memory.
 *    → Implemented here
 *
 * Why both?
 *   - Lazy alone: expired entries that are never accessed stay forever (memory leak)
 *   - Active alone: expired entries are returned between sweeps (incorrect behavior)
 *   - Together: correct behavior (lazy) + no memory leaks (active)
 *
 * This is the same pattern Redis uses for key expiration.
 */
public class ExpirationReaper {

    private final ConcurrentHashMap<String, ShortenedUrl> codeToUrl;
    private final ConcurrentHashMap<String, String> urlToCode;  // may be null
    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger totalReaped;

    /**
     * Creates a reaper that periodically cleans expired entries.
     *
     * @param codeToUrl       the code → URL map to clean
     * @param urlToCode       the URL → code map to clean (null if not used)
     * @param clock           clock for time checks
     * @param intervalSeconds how often to sweep (in seconds)
     */
    public ExpirationReaper(
            ConcurrentHashMap<String, ShortenedUrl> codeToUrl,
            ConcurrentHashMap<String, String> urlToCode,
            Clock clock,
            long intervalSeconds) {

        this.codeToUrl = codeToUrl;
        this.urlToCode = urlToCode;
        this.clock = clock;
        this.totalReaped = new AtomicInteger(0);

        // Use a daemon thread so it doesn't prevent JVM shutdown
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "expiration-reaper");
            t.setDaemon(true);
            return t;
        });

        // Schedule periodic sweep
        scheduler.scheduleAtFixedRate(
                this::sweep,
                intervalSeconds,  // initial delay
                intervalSeconds,  // period
                TimeUnit.SECONDS
        );
    }

    /**
     * Performs a single sweep, removing all expired entries.
     *
     * <p>Time Complexity: O(n) where n = total entries
     *    (must check each entry's expiration time)
     *
     * <p>Thread Safety: ConcurrentHashMap.entrySet() iteration is
     *    weakly consistent — it may or may not reflect concurrent updates,
     *    but it will not throw ConcurrentModificationException.
     */
    public int sweep() {
        Instant now = Instant.now(clock);
        int reaped = 0;

        for (Map.Entry<String, ShortenedUrl> entry : codeToUrl.entrySet()) {
            ShortenedUrl url = entry.getValue();
            if (url.isExpired(now)) {
                // Remove from both maps
                codeToUrl.remove(entry.getKey());
                if (urlToCode != null) {
                    urlToCode.remove(url.originalUrl());
                }
                reaped++;
            }
        }

        totalReaped.addAndGet(reaped);
        return reaped;
    }

    /**
     * Returns the total number of entries reaped since creation.
     */
    public int totalReaped() {
        return totalReaped.get();
    }

    /**
     * Stops the background sweep thread.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Expiration Reaper Demo ===\n");

        ConcurrentHashMap<String, ShortenedUrl> map = new ConcurrentHashMap<>();
        Clock clock = Clock.systemUTC();

        // Add some entries with 1-second TTL
        Instant now = Instant.now();
        map.put("abc", new ShortenedUrl("abc", "https://example.com", now,
                java.time.Duration.ofSeconds(1)));
        map.put("def", new ShortenedUrl("def", "https://google.com", now,
                java.time.Duration.ofSeconds(1)));
        map.put("ghi", new ShortenedUrl("ghi", "https://github.com", now, null));  // no TTL

        System.out.println("  Entries before: " + map.size());  // 3

        // Wait for expiration
        Thread.sleep(1200);

        // Manual sweep
        ExpirationReaper reaper = new ExpirationReaper(map, null, clock, 60);
        int reaped = reaper.sweep();
        System.out.println("  Reaped: " + reaped);              // 2
        System.out.println("  Entries after: " + map.size());    // 1 (only "ghi" survives)

        reaper.shutdown();
        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ TWO-PHASE EXPIRATION (Redis pattern)                       │
 * │                                                             │
 * │   Phase 1 — LAZY (on access):                              │
 * │     resolve("abc") → check isExpired() → if yes, remove   │
 * │     ✓ No wasted work on cold entries                       │
 * │     ✗ Cold expired entries stay in memory forever          │
 * │                                                             │
 * │   Phase 2 — ACTIVE (background sweep):                     │
 * │     Every N seconds: scan all entries, remove expired      │
 * │     ✓ Catches cold entries                                 │
 * │     ✗ O(n) sweep — must not block the main path           │
 * │                                                             │
 * │   Together: correct + no memory leaks                      │
 * │                                                             │
 * │ Q: "How does Redis implement key expiration?"              │
 * │ A: "Exactly this pattern:                                  │
 * │    1. Lazy: on every read, check TTL                       │
 * │    2. Active: 10 times/sec, sample 20 random keys,         │
 * │       if >25% expired, repeat. Probabilistic — O(1)        │
 * │       per sweep, not O(n). Adapts to expiration rate."     │
 * │                                                             │
 * │ Q: "O(n) sweep is expensive — how to optimize?"           │
 * │ A: "Use a min-heap sorted by expiresAt. The sweep only    │
 * │    needs to check the heap root until it finds a non-      │
 * │    expired entry — O(k) where k = expired entries.         │
 * │    Alternatively: Redis-style random sampling."            │
 * └─────────────────────────────────────────────────────────────┘
 */

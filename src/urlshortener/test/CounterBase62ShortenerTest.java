package urlshortener.test;

import urlshortener.model.UrlStats;
import urlshortener.service.CounterBase62Shortener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class CounterBase62ShortenerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testBasicShortenResolve() {
        System.out.println("\n=== Test 1: Basic Shorten and Resolve ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String code = s.shorten("https://example.com");
        check("Code is non-null", code != null);
        check("Code length >= 6", code.length() >= 6);

        Optional<String> resolved = s.resolve(code);
        check("Resolves to original URL", resolved.isPresent() &&
                resolved.get().equals("https://example.com"));
    }

    static void testDeduplication() {
        System.out.println("\n=== Test 2: Deduplication ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String c1 = s.shorten("https://example.com");
        String c2 = s.shorten("https://example.com");
        check("Same URL → same code", c1.equals(c2));
        check("Size is 1 (not 2)", s.size() == 1);

        // Different URL → different code
        String c3 = s.shorten("https://other.com");
        check("Different URL → different code", !c1.equals(c3));
        check("Size is 2", s.size() == 2);
    }

    static void testNormalizationDedup() {
        System.out.println("\n=== Test 3: Normalization-Based Dedup ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String c1 = s.shorten("https://example.com/");
        String c2 = s.shorten("HTTPS://EXAMPLE.COM");
        String c3 = s.shorten("https://example.com:443");
        check("Trailing slash dedup", c1.equals(c2));
        check("Port 443 dedup", c2.equals(c3));
    }

    static void testResolveNonExistent() {
        System.out.println("\n=== Test 4: Resolve Non-Existent ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        check("Non-existent returns empty", s.resolve("ZZZZZZ").isEmpty());
    }

    static void testDelete() {
        System.out.println("\n=== Test 5: Delete ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String code = s.shorten("https://example.com");
        check("Exists before delete", s.resolve(code).isPresent());

        boolean deleted = s.delete(code);
        check("Delete returns true", deleted);
        check("Gone after delete", s.resolve(code).isEmpty());
        check("Size is 0", s.size() == 0);

        check("Delete non-existent returns false", !s.delete("ZZZZZZ"));
    }

    static void testStats() {
        System.out.println("\n=== Test 6: Stats / Hit Count ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        String code = s.shorten("https://example.com");
        s.resolve(code);
        s.resolve(code);
        s.resolve(code);

        Optional<UrlStats> stats = s.getStats(code);
        check("Stats present", stats.isPresent());
        check("Hit count is 3", stats.get().hitCount() == 3);
        check("Original URL matches", stats.get().originalUrl().equals("https://example.com"));
    }

    static void testExpiration() throws InterruptedException {
        System.out.println("\n=== Test 7: Expiration (TTL) ===");

        // Use a fixed clock that we can reason about
        Instant baseTime = Instant.parse("2024-06-15T12:00:00Z");
        Clock fixedClock = Clock.fixed(baseTime, ZoneOffset.UTC);
        CounterBase62Shortener s = new CounterBase62Shortener(fixedClock);

        String code = s.shorten("https://example.com", Duration.ofSeconds(5));
        check("Before expiry — resolves", s.resolve(code).isPresent());

        // Advance clock past TTL
        Clock expiredClock = Clock.fixed(baseTime.plusSeconds(6), ZoneOffset.UTC);
        CounterBase62Shortener s2 = new CounterBase62Shortener(1000, 6, expiredClock);
        // Copy the entry manually by shortening again with TTL in past
        String code2 = s2.shorten("https://example.com", Duration.ofSeconds(-1));
        check("Expired entry returns empty", s2.resolve(code2).isEmpty());
    }

    static void testInvalidUrl() {
        System.out.println("\n=== Test 8: Invalid URL ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        boolean caught = false;
        try { s.shorten("not-a-url"); } catch (IllegalArgumentException e) { caught = true; }
        check("Invalid URL throws", caught);
    }

    static void testUniqueCodes() {
        System.out.println("\n=== Test 9: Unique Codes (1000 URLs) ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(s.shorten("https://example.com/page" + i));
        }
        check("1000 URLs → 1000 unique codes", codes.size() == 1000);
    }

    static void testThreadSafety() throws InterruptedException {
        System.out.println("\n=== Test 10: Thread Safety ===");
        CounterBase62Shortener s = new CounterBase62Shortener();

        int numThreads = 20;
        int urlsPerThread = 500;  // 10K unique URLs total
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);
        Set<String> allCodes = java.util.Collections.synchronizedSet(new HashSet<>());

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < urlsPerThread; i++) {
                        String url = "https://example.com/t" + threadId + "/p" + i;
                        String code = s.shorten(url);
                        allCodes.add(code);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executor.shutdown();

        int expected = numThreads * urlsPerThread;
        check("All " + expected + " codes unique (got " + allCodes.size() + ")",
                allCodes.size() == expected);
        check("Size matches", s.size() == expected);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Counter Base62 Shortener — Test Suite      ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicShortenResolve();
        testDeduplication();
        testNormalizationDedup();
        testResolveNonExistent();
        testDelete();
        testStats();
        testExpiration();
        testInvalidUrl();
        testUniqueCodes();
        testThreadSafety();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("══════════════════════════════════════════════");
    }
}

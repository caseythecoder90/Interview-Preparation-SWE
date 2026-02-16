package urlshortener.test;

import urlshortener.service.CounterBase62Shortener;
import urlshortener.service.HashBasedShortener;
import urlshortener.service.RandomShortener;
import urlshortener.service.UrlShortener;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ShortenerComparisonTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("    PASS: " + testName); passed++; }
        else           { System.out.println("    FAIL: " + testName); failed++; }
    }

    static void testAllShortenAndResolve() {
        System.out.println("\n=== Test 1: All Strategies — Shorten and Resolve ===");
        UrlShortener[] shorteners = {
                new CounterBase62Shortener(),
                new HashBasedShortener(),
                new RandomShortener()
        };

        for (UrlShortener s : shorteners) {
            String name = s.strategyName();
            String code = s.shorten("https://example.com");
            Optional<String> resolved = s.resolve(code);
            check(name + " — shorten returns code", code != null && !code.isEmpty());
            check(name + " — resolve returns URL", resolved.isPresent() &&
                    resolved.get().equals("https://example.com"));
        }
    }

    static void testAllDeduplication() {
        System.out.println("\n=== Test 2: All Strategies — Deduplication ===");
        UrlShortener[] shorteners = {
                new CounterBase62Shortener(),
                new HashBasedShortener(),
                new RandomShortener()
        };

        for (UrlShortener s : shorteners) {
            String c1 = s.shorten("https://example.com");
            String c2 = s.shorten("https://example.com");
            check(s.strategyName() + " — same URL same code", c1.equals(c2));
        }
    }

    static void testAllNonExistent() {
        System.out.println("\n=== Test 3: All Strategies — Non-Existent Resolve ===");
        UrlShortener[] shorteners = {
                new CounterBase62Shortener(),
                new HashBasedShortener(),
                new RandomShortener()
        };

        for (UrlShortener s : shorteners) {
            check(s.strategyName() + " — missing returns empty",
                    s.resolve("ZZZZZZZZZZ").isEmpty());
        }
    }

    static void testCodeCharacteristics() {
        System.out.println("\n=== Test 4: Code Characteristics ===");

        CounterBase62Shortener counter = new CounterBase62Shortener();
        HashBasedShortener hash = new HashBasedShortener();
        RandomShortener random = new RandomShortener();

        // Counter produces sequential-looking codes
        String c1 = counter.shorten("https://example.com/1");
        String c2 = counter.shorten("https://example.com/2");
        System.out.println("    Counter codes: " + c1 + ", " + c2 + " (sequential)");
        check("Counter codes are different", !c1.equals(c2));

        // Hash produces hex codes
        String h1 = hash.shorten("https://example.com/1");
        boolean allHex = h1.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
        System.out.println("    Hash code: " + h1 + " (hex)");
        check("Hash code is hex", allHex);

        // Random produces alphanumeric codes
        String r1 = random.shorten("https://example.com/1");
        System.out.println("    Random code: " + r1 + " (alphanumeric)");
        check("Random code is alphanumeric", r1.matches("[0-9a-zA-Z]+"));
    }

    static void testPerformanceComparison() {
        System.out.println("\n=== Test 5: Performance Comparison ===");

        UrlShortener[] shorteners = {
                new CounterBase62Shortener(),
                new HashBasedShortener(),
                new RandomShortener()
        };

        int numUrls = 10_000;

        for (UrlShortener s : shorteners) {
            // Shorten benchmark
            long start = System.nanoTime();
            for (int i = 0; i < numUrls; i++) {
                s.shorten("https://example.com/page/" + i);
            }
            long shortenMs = (System.nanoTime() - start) / 1_000_000;

            // Resolve benchmark (resolve first URL 10K times)
            String firstCode = s.shorten("https://example.com/page/0");
            start = System.nanoTime();
            for (int i = 0; i < numUrls; i++) {
                s.resolve(firstCode);
            }
            long resolveMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("    %-25s  shorten: %4d ms  resolve: %4d ms  size: %d%n",
                    s.strategyName(), shortenMs, resolveMs, s.size());
        }

        check("Performance benchmark completed", true);
    }

    static void printComparisonTable() {
        System.out.println("\n=== Strategy Comparison Summary ===");
        System.out.println("  ┌──────────────────────┬──────────┬───────────┬───────────────┬──────────────┐");
        System.out.println("  │ Strategy             │ Unique?  │ Predict?  │ Code Format   │ Best For     │");
        System.out.println("  ├──────────────────────┼──────────┼───────────┼───────────────┼──────────────┤");
        System.out.println("  │ Counter + Base62     │ Always   │ Yes*      │ Base62        │ General use, │");
        System.out.println("  │                      │          │           │ (a-z,A-Z,0-9) │ interview    │");
        System.out.println("  ├──────────────────────┼──────────┼───────────┼───────────────┼──────────────┤");
        System.out.println("  │ Hash (SHA truncated) │ Retry    │ Determin. │ Hex (0-9,a-f) │ Stateless    │");
        System.out.println("  │                      │ on coll. │           │               │ systems      │");
        System.out.println("  ├──────────────────────┼──────────┼───────────┼───────────────┼──────────────┤");
        System.out.println("  │ Random (SecureRandom)│ Retry    │ No        │ Base62        │ Security-    │");
        System.out.println("  │                      │ on coll. │           │               │ sensitive    │");
        System.out.println("  └──────────────────────┴──────────┴───────────┴───────────────┴──────────────┘");
        System.out.println("  * Counter is predictable — XOR with secret to obfuscate");
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Shortener Comparison — Test Suite          ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testAllShortenAndResolve();
        testAllDeduplication();
        testAllNonExistent();
        testCodeCharacteristics();
        testPerformanceComparison();
        printComparisonTable();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("══════════════════════════════════════════════");
    }
}

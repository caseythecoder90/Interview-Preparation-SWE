package urlshortener.test;

import urlshortener.service.HashBasedShortener;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class HashBasedShortenerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testBasicShortenResolve() {
        System.out.println("\n=== Test 1: Basic Shorten and Resolve ===");
        HashBasedShortener s = new HashBasedShortener();

        String code = s.shorten("https://example.com");
        check("Code is non-null", code != null);
        check("Code length is 7", code.length() == 7);

        Optional<String> resolved = s.resolve(code);
        check("Resolves correctly", resolved.isPresent() &&
                resolved.get().equals("https://example.com"));
    }

    static void testDeterministicCodes() {
        System.out.println("\n=== Test 2: Deterministic (Same URL → Same Code) ===");
        HashBasedShortener s = new HashBasedShortener();

        String c1 = s.shorten("https://example.com");
        String c2 = s.shorten("https://example.com");
        check("Same URL produces same code", c1.equals(c2));
        check("Size is 1 (dedup)", s.size() == 1);
    }

    static void testDifferentUrls() {
        System.out.println("\n=== Test 3: Different URLs → Different Codes ===");
        HashBasedShortener s = new HashBasedShortener();

        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codes.add(s.shorten("https://example.com/page" + i));
        }
        check("100 unique URLs → 100 unique codes", codes.size() == 100);
    }

    static void testDelete() {
        System.out.println("\n=== Test 4: Delete ===");
        HashBasedShortener s = new HashBasedShortener();

        String code = s.shorten("https://example.com");
        check("Exists", s.resolve(code).isPresent());
        s.delete(code);
        check("Gone after delete", s.resolve(code).isEmpty());
    }

    static void testHexCodeFormat() {
        System.out.println("\n=== Test 5: Code Format (hex characters) ===");
        HashBasedShortener s = new HashBasedShortener();

        String code = s.shorten("https://example.com");
        boolean allHex = code.chars().allMatch(c ->
                (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'));
        check("Code contains only hex chars", allHex);
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Hash-Based Shortener — Test Suite          ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicShortenResolve();
        testDeterministicCodes();
        testDifferentUrls();
        testDelete();
        testHexCodeFormat();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("══════════════════════════════════════════════");
    }
}

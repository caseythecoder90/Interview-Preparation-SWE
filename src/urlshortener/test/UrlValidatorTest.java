package urlshortener.test;

import urlshortener.validation.UrlValidator;

public class UrlValidatorTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testValidUrls() {
        System.out.println("\n=== Test 1: Valid URLs ===");
        check("http simple", UrlValidator.isValid("http://example.com"));
        check("https simple", UrlValidator.isValid("https://example.com"));
        check("with path", UrlValidator.isValid("https://example.com/path/to/page"));
        check("with query", UrlValidator.isValid("https://example.com?q=test"));
        check("with port", UrlValidator.isValid("https://example.com:8080"));
        check("with fragment", UrlValidator.isValid("https://example.com#section"));
    }

    static void testInvalidUrls() {
        System.out.println("\n=== Test 2: Invalid URLs ===");
        check("null", !UrlValidator.isValid(null));
        check("empty", !UrlValidator.isValid(""));
        check("blank", !UrlValidator.isValid("   "));
        check("no scheme", !UrlValidator.isValid("example.com"));
        check("ftp scheme", !UrlValidator.isValid("ftp://files.example.com"));
        check("just text", !UrlValidator.isValid("not-a-url"));
    }

    static void testNormalization() {
        System.out.println("\n=== Test 3: Normalization ===");
        check("lowercase scheme", UrlValidator.normalize("HTTP://example.com").equals("http://example.com"));
        check("lowercase host", UrlValidator.normalize("https://EXAMPLE.COM").equals("https://example.com"));
        check("remove trailing slash", UrlValidator.normalize("https://example.com/").equals("https://example.com"));
        check("remove default http port", UrlValidator.normalize("http://example.com:80/page").equals("http://example.com/page"));
        check("remove default https port", UrlValidator.normalize("https://example.com:443/page").equals("https://example.com/page"));
        check("keep non-default port", UrlValidator.normalize("https://example.com:8080/page").equals("https://example.com:8080/page"));
        check("remove fragment", !UrlValidator.normalize("https://example.com/page#section").contains("#"));
        check("preserve query", UrlValidator.normalize("https://example.com/s?q=test").contains("q=test"));
    }

    static void testNormalizationDedup() {
        System.out.println("\n=== Test 4: Normalization Deduplication ===");
        // These should all normalize to the same string
        String n1 = UrlValidator.normalize("https://example.com/");
        String n2 = UrlValidator.normalize("https://EXAMPLE.COM");
        String n3 = UrlValidator.normalize("HTTPS://example.com");
        String n4 = UrlValidator.normalize("https://example.com:443");
        check("trailing slash == no slash", n1.equals(n2));
        check("uppercase host == lowercase", n2.equals(n3));
        check("default port == no port", n3.equals(n4));
    }

    static void testNormalizeInvalid() {
        System.out.println("\n=== Test 5: Normalize Invalid Throws ===");
        boolean caught = false;
        try { UrlValidator.normalize("not-a-url"); } catch (IllegalArgumentException e) { caught = true; }
        check("Invalid URL throws on normalize", caught);
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  URL Validator — Test Suite                 ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testValidUrls();
        testInvalidUrls();
        testNormalization();
        testNormalizationDedup();
        testNormalizeInvalid();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("══════════════════════════════════════════════");
    }
}

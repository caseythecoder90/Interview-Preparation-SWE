package urlshortener.validation;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * ============================================================
 * URL VALIDATOR — Validation + Normalization
 * ============================================================
 *
 * Validates and normalizes URLs before shortening.
 *
 * Why normalize?
 *   - "http://example.com" and "http://example.com/" are the same page
 *   - "HTTP://EXAMPLE.COM" and "http://example.com" are the same
 *   - Without normalization, we'd create duplicate short URLs for the same destination
 *   - Normalization ensures one canonical form → one short code
 *
 * Validation rules:
 *   1. Not null or blank
 *   2. Has a valid scheme (http or https)
 *   3. Has a valid host
 *   4. Parseable as a URI
 */
public final class UrlValidator {

    private UrlValidator() {}

    /**
     * Validates that a URL is well-formed and uses http/https.
     *
     * @param url the URL to validate
     * @return true if valid
     */
    public static boolean isValid(String url) {
        if (url == null || url.isBlank()) return false;

        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Normalizes a URL to its canonical form.
     *
     * <p>Normalization steps:
     * <ol>
     *   <li>Trim whitespace</li>
     *   <li>Lowercase the scheme and host</li>
     *   <li>Remove default ports (80 for http, 443 for https)</li>
     *   <li>Remove trailing slash on the path (unless path is just "/")</li>
     *   <li>Remove fragment (#section) — not sent to server</li>
     * </ol>
     *
     * <p>Time Complexity: O(n) where n = URL length
     *
     * @param url the URL to normalize
     * @return normalized URL string
     * @throws IllegalArgumentException if URL is invalid
     */
    public static String normalize(String url) {
        if (!isValid(url)) {
            throw new IllegalArgumentException("Invalid URL: " + url);
        }

        try {
            URI uri = new URI(url.trim());

            String scheme = uri.getScheme().toLowerCase();
            String host = uri.getHost().toLowerCase();
            int port = uri.getPort();

            // Remove default ports
            if ((scheme.equals("http") && port == 80) ||
                    (scheme.equals("https") && port == 443)) {
                port = -1;  // -1 means "no explicit port"
            }

            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "";
            }
            // Remove trailing slash — including root "/" → ""
            // "https://example.com/" and "https://example.com" should be the same
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Reconstruct without fragment
            StringBuilder sb = new StringBuilder();
            sb.append(scheme).append("://").append(host);
            if (port != -1) {
                sb.append(":").append(port);
            }
            sb.append(path);

            // Preserve query string if present
            if (uri.getQuery() != null) {
                sb.append("?").append(uri.getQuery());
            }

            return sb.toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== URL Validator Demo ===\n");

        String[] testUrls = {
                "https://example.com",
                "https://example.com/",
                "HTTP://EXAMPLE.COM/path/",
                "https://example.com:443/page",
                "http://example.com:80/page",
                "http://example.com:8080/page",
                "https://example.com/search?q=test#section",
                "ftp://files.example.com",  // invalid scheme
                "not-a-url",                 // invalid
                "",                          // blank
                null                         // null
        };

        for (String url : testUrls) {
            boolean valid = isValid(url);
            System.out.printf("  %-45s  valid=%s", url == null ? "null" : url, valid);
            if (valid) {
                System.out.printf("  → %s", normalize(url));
            }
            System.out.println();
        }
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ WHY NORMALIZE?                                              │
 * │   Without normalization, the same webpage gets multiple     │
 * │   short codes — wasting space and confusing analytics.     │
 * │   Example: "HTTP://Example.com/" and "http://example.com"  │
 * │   would get different codes even though they're the same.  │
 * │                                                             │
 * │ Q: "What other normalization would you do in production?"  │
 * │ A: "Sort query parameters alphabetically, decode           │
 * │    unnecessary percent-encoding (%41 → A), handle IDN     │
 * │    (internationalized domain names), follow redirects to   │
 * │    get the final URL (optional — adds latency)."           │
 * │                                                             │
 * │ Q: "Should you deduplicate URLs (same long URL → same code)│
 * │ A: "Depends on requirements. With deduplication:            │
 * │    ✓ saves space, consistent analytics per destination     │
 * │    ✗ requires a reverse lookup (URL → code), adds latency │
 * │    In practice: most services DO deduplicate."             │
 * └─────────────────────────────────────────────────────────────┘
 */

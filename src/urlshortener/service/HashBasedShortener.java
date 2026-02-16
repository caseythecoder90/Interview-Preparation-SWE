package urlshortener.service;

import urlshortener.model.ShortenedUrl;
import urlshortener.model.UrlStats;
import urlshortener.validation.UrlValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * HASH-BASED URL SHORTENER — MD5/SHA Truncation Approach
 * ============================================================
 *
 * Approach:
 *   1. Compute MD5 or SHA-256 hash of the URL → 128 or 256 bits
 *   2. Take the first N characters of the hex hash → short code
 *   3. If collision, append a suffix and rehash
 *
 * Why this is a valid alternative:
 *   - Deterministic: same URL always produces the same hash → natural dedup
 *   - No counter needed — works in stateless/distributed systems
 *   - Widely used: bit.ly originally used a hash-based approach
 *
 * Downsides:
 *   - Collisions: different URLs CAN produce the same truncated hash
 *   - Collision handling adds complexity and unpredictable latency
 *   - MD5 is cryptographically broken (but fine for non-security hashing)
 *
 * Collision probability (Birthday Problem):
 *   For 7 hex chars (28 bits): collision likely after ~16K URLs
 *   For 8 hex chars (32 bits): collision likely after ~65K URLs
 *   For 7 Base62 chars: collision likely after ~2.4M URLs
 *   → Hash truncation is NOT suitable for large-scale services
 */
public class HashBasedShortener implements UrlShortener {

    private final ConcurrentHashMap<String, ShortenedUrl> codeToUrl;
    private final int codeLength;
    private final Clock clock;

    public HashBasedShortener(int codeLength, Clock clock) {
        this.codeLength = codeLength;
        this.clock = clock;
        this.codeToUrl = new ConcurrentHashMap<>();
    }

    public HashBasedShortener() {
        this(7, Clock.systemUTC());
    }

    public HashBasedShortener(Clock clock) {
        this(7, clock);
    }

    /**
     * Generates a short code by hashing the URL.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Compute SHA-256 of the normalized URL</li>
     *   <li>Take first `codeLength` hex characters as the code</li>
     *   <li>If collision (different URL, same code): append attempt number and rehash</li>
     * </ol>
     *
     * <p>Time Complexity: O(1) average, O(k) worst case where k = collision retries
     * <p>Space Complexity: O(1) per URL
     */
    @Override
    public String shorten(String originalUrl) {
        return shorten(originalUrl, null);
    }

    @Override
    public String shorten(String originalUrl, Duration ttl) {
        String normalized = UrlValidator.normalize(originalUrl);

        // Try hashing with incrementing suffix on collision
        for (int attempt = 0; attempt < 100; attempt++) {
            String input = (attempt == 0) ? normalized : normalized + ":" + attempt;
            String code = hashAndTruncate(input);

            ShortenedUrl existing = codeToUrl.get(code);

            // No collision — store and return
            if (existing == null) {
                Instant now = Instant.now(clock);
                ShortenedUrl entry = new ShortenedUrl(code, normalized, now, ttl);
                // putIfAbsent for thread safety: only one thread wins
                ShortenedUrl prev = codeToUrl.putIfAbsent(code, entry);
                if (prev == null) {
                    return code;  // we won the race
                }
                existing = prev;  // another thread got there first
            }

            // Same URL → return existing code (deduplication)
            if (existing.originalUrl().equals(normalized) && !existing.isExpired(Instant.now(clock))) {
                return code;
            }

            // Different URL, same code → collision! Retry with next suffix
        }

        throw new RuntimeException("Failed to find unique code after 100 attempts for: " + originalUrl);
    }

    private String hashAndTruncate(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert first bytes to hex string and truncate
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
                if (hex.length() >= codeLength) break;
            }
            return hex.substring(0, codeLength);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Override
    public Optional<String> resolve(String shortCode) {
        ShortenedUrl entry = codeToUrl.get(shortCode);
        if (entry == null) return Optional.empty();

        Instant now = Instant.now(clock);
        if (entry.isExpired(now)) {
            codeToUrl.remove(shortCode);
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
        return codeToUrl.remove(shortCode) != null;
    }

    @Override
    public int size() {
        return codeToUrl.size();
    }

    @Override
    public String strategyName() {
        return "Hash (SHA-256 truncated)";
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== Hash-Based URL Shortener Demo ===\n");

        HashBasedShortener shortener = new HashBasedShortener();

        String[] urls = {
                "https://www.google.com",
                "https://github.com",
                "https://example.com/page",
                "https://www.google.com"  // duplicate — should return same code
        };

        for (String url : urls) {
            String code = shortener.shorten(url);
            System.out.printf("  %s → %s%n", url, code);
        }

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   shorten(): O(1) average, O(k) worst (k = collision       │
 * │              retries). SHA-256 is O(n) on input length.    │
 * │   resolve(): O(1)                                           │
 * │   Space:     O(n) for the code→URL map                    │
 * │                                                             │
 * │ COLLISION PROBABILITY (Birthday Problem)                   │
 * │   For N existing codes and M possible codes:               │
 * │   P(collision) ≈ 1 - e^(-N²/2M)                           │
 * │                                                             │
 * │   7 hex chars (16^7 = 268M): ~50% collision at ~18K URLs  │
 * │   8 hex chars (16^8 = 4.3B): ~50% collision at ~83K URLs  │
 * │   → NOT scalable for large services                        │
 * │                                                             │
 * │ WHY COUNTER-BASED IS BETTER                                 │
 * │   Counter: zero collisions, O(1) guaranteed, simpler       │
 * │   Hash: collisions possible, retry overhead, more complex  │
 * │   → Use hash only when you need deterministic codes        │
 * │     (stateless systems, no shared counter)                  │
 * │                                                             │
 * │ Q: "When would you use hash-based?"                        │
 * │ A: "When servers can't share a counter — e.g., serverless │
 * │    functions with no shared state. The hash approach is    │
 * │    fully stateless for code generation (only storage needs │
 * │    to be shared for collision detection)."                  │
 * └─────────────────────────────────────────────────────────────┘
 */

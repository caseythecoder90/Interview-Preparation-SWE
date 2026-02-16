package urlshortener.service;

import urlshortener.model.ShortenedUrl;
import urlshortener.model.UrlStats;
import urlshortener.validation.UrlValidator;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * RANDOM URL SHORTENER — SecureRandom + Collision Retry
 * ============================================================
 *
 * Approach:
 *   1. Generate a random Base62 string of `codeLength` characters
 *   2. Check if the code already exists (collision check)
 *   3. If collision, retry with a new random code
 *   4. Store the mapping
 *
 * Why SecureRandom (not Random)?
 *   - Random is predictable (seeded PRNG) — codes can be guessed
 *   - SecureRandom uses OS entropy (/dev/urandom) — unpredictable
 *   - For URL shorteners, unpredictable codes prevent enumeration attacks
 *
 * Tradeoffs:
 *   ✓ Unpredictable codes (good for security)
 *   ✓ Uniform distribution across code space
 *   ✗ Collision retries as the space fills up
 *   ✗ No natural deduplication (same URL → different codes)
 *   ✗ SecureRandom is slower than counter increment
 */
public class RandomShortener implements UrlShortener {

    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final ConcurrentHashMap<String, ShortenedUrl> codeToUrl;
    private final ConcurrentHashMap<String, String> urlToCode;
    private final SecureRandom random;
    private final int codeLength;
    private final Clock clock;

    public RandomShortener(int codeLength, Clock clock) {
        this.codeLength = codeLength;
        this.clock = clock;
        this.random = new SecureRandom();
        this.codeToUrl = new ConcurrentHashMap<>();
        this.urlToCode = new ConcurrentHashMap<>();
    }

    public RandomShortener() {
        this(6, Clock.systemUTC());
    }

    public RandomShortener(Clock clock) {
        this(6, clock);
    }

    /**
     * Generates a random Base62 code of the configured length.
     */
    private String generateRandomCode() {
        char[] code = new char[codeLength];
        for (int i = 0; i < codeLength; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }

    /**
     * Shortens a URL using random code generation with collision retry.
     *
     * <p>Time Complexity: O(1) average. O(k) worst case where k = retries.
     *    Retries are rare when codeSpace >> numEntries.
     *    With 6 chars (56B codes) and 1M entries: P(collision per try) ≈ 0.002%
     */
    @Override
    public String shorten(String originalUrl) {
        return shorten(originalUrl, null);
    }

    @Override
    public String shorten(String originalUrl, Duration ttl) {
        String normalized = UrlValidator.normalize(originalUrl);

        // Deduplication check
        String existingCode = urlToCode.get(normalized);
        if (existingCode != null) {
            ShortenedUrl existing = codeToUrl.get(existingCode);
            if (existing != null && !existing.isExpired(Instant.now(clock))) {
                return existingCode;
            }
        }

        // Generate random code with collision retry
        for (int attempt = 0; attempt < 100; attempt++) {
            String code = generateRandomCode();

            Instant now = Instant.now(clock);
            ShortenedUrl entry = new ShortenedUrl(code, normalized, now, ttl);

            // putIfAbsent: atomic "check and insert" — handles race conditions
            if (codeToUrl.putIfAbsent(code, entry) == null) {
                urlToCode.put(normalized, code);
                return code;
            }
            // Collision — try again with a new random code
        }

        throw new RuntimeException("Failed to generate unique code after 100 attempts");
    }

    @Override
    public Optional<String> resolve(String shortCode) {
        ShortenedUrl entry = codeToUrl.get(shortCode);
        if (entry == null) return Optional.empty();

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
        return "Random (SecureRandom)";
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== Random URL Shortener Demo ===\n");

        RandomShortener shortener = new RandomShortener();

        for (int i = 0; i < 5; i++) {
            String code = shortener.shorten("https://example.com/page" + i);
            System.out.println("  https://example.com/page" + i + " → " + code);
        }

        // Same URL returns same code (deduplication)
        String c1 = shortener.shorten("https://example.com/page0");
        String c2 = shortener.shorten("https://example.com/page0");
        System.out.println("\n  Dedup test: " + c1 + " == " + c2 + " → " + c1.equals(c2));

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   shorten(): O(1) average, O(k) worst case (retries)      │
 * │   resolve(): O(1)                                           │
 * │   Space:     O(n) — two maps                               │
 * │                                                             │
 * │ COLLISION PROBABILITY                                       │
 * │   Code space: 62^6 ≈ 56.8 billion                          │
 * │   With 1M entries: P(collision per attempt) ≈ 0.002%       │
 * │   With 100M entries: P(collision per attempt) ≈ 0.18%      │
 * │   With 1B entries: P(collision per attempt) ≈ 1.8%         │
 * │   → Retries increase as space fills up                     │
 * │                                                             │
 * │ WHY SecureRandom?                                           │
 * │   java.util.Random uses a linear congruential generator —  │
 * │   predictable. An attacker could guess the seed and        │
 * │   enumerate all short URLs. SecureRandom uses OS entropy   │
 * │   → computationally infeasible to predict.                 │
 * │                                                             │
 * │ COMPARISON TO COUNTER-BASED                                 │
 * │   Counter:   guaranteed unique, sequential (predictable)   │
 * │   Random:    unpredictable, but collisions possible         │
 * │   → Counter + XOR shuffle gives the best of both worlds   │
 * └─────────────────────────────────────────────────────────────┘
 */

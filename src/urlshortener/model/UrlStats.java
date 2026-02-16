package urlshortener.model;

import java.time.Instant;

/**
 * ============================================================
 * URL STATS — Analytics Response Record
 * ============================================================
 *
 * Immutable snapshot of a shortened URL's statistics.
 * Returned by analytics endpoints — never mutated after creation.
 *
 * Why a record (unlike ShortenedUrl)?
 *   - This is a response DTO (Data Transfer Object) — immutable by nature
 *   - Created once, read many times, never mutated
 *   - Records auto-generate equals/hashCode/toString — perfect for DTOs
 *
 * @param shortCode    the Base62 short code
 * @param originalUrl  the destination URL
 * @param hitCount     total number of redirects
 * @param createdAt    when the URL was shortened
 * @param lastAccessed when the URL was last resolved
 * @param isExpired    whether the URL has expired
 */
public record UrlStats(
        String shortCode,
        String originalUrl,
        long hitCount,
        Instant createdAt,
        Instant lastAccessed,
        boolean isExpired
) {
    /**
     * Creates a UrlStats snapshot from a ShortenedUrl.
     */
    public static UrlStats from(ShortenedUrl url, Instant now) {
        return new UrlStats(
                url.shortCode(),
                url.originalUrl(),
                url.hitCount(),
                url.createdAt(),
                url.lastAccessed(),
                url.isExpired(now)
        );
    }
}

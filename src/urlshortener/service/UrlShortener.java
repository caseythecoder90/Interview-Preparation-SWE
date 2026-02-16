package urlshortener.service;

import urlshortener.model.ShortenedUrl;
import urlshortener.model.UrlStats;

import java.time.Duration;
import java.util.Optional;

/**
 * ============================================================
 * URL SHORTENER — Common Interface (Strategy Pattern)
 * ============================================================
 *
 * All URL shortening strategies implement this interface.
 */
public interface UrlShortener {

    /**
     * Shortens a URL, returning the generated short code.
     *
     * @param originalUrl the URL to shorten
     * @return the short code (e.g., "3dL4x")
     * @throws IllegalArgumentException if URL is invalid
     */
    String shorten(String originalUrl);

    /**
     * Shortens a URL with a time-to-live.
     *
     * @param originalUrl the URL to shorten
     * @param ttl         time-to-live duration (null = no expiration)
     * @return the short code
     */
    String shorten(String originalUrl, Duration ttl);

    /**
     * Resolves a short code to the original URL.
     * Records a hit for analytics. Returns empty if not found or expired.
     *
     * @param shortCode the code to resolve
     * @return the original URL, or empty if not found/expired
     */
    Optional<String> resolve(String shortCode);

    /**
     * Gets analytics for a short code.
     *
     * @param shortCode the code to query
     * @return stats if found, empty otherwise
     */
    Optional<UrlStats> getStats(String shortCode);

    /**
     * Deletes a shortened URL.
     *
     * @param shortCode the code to delete
     * @return true if existed and was deleted
     */
    boolean delete(String shortCode);

    /**
     * Returns the total number of active (non-expired) shortened URLs.
     */
    int size();

    /**
     * Returns the strategy name.
     */
    String strategyName();
}

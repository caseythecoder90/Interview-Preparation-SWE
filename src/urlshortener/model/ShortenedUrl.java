package urlshortener.model;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ============================================================
 * SHORTENED URL — Mutable Data Model
 * ============================================================
 *
 * Represents a shortened URL with metadata, access tracking, and TTL.
 *
 * Why NOT a record?
 *   - hitCount and lastAccessed are mutable (updated on each redirect)
 *   - Records are immutable by design — wrong fit for mutable state
 *   - Using AtomicLong/AtomicReference for thread-safe mutation
 *     without external synchronization
 *
 * Fields:
 *   - shortCode:    the Base62 code (e.g., "3dL4x")
 *   - originalUrl:  the full destination URL
 *   - createdAt:    when this mapping was created
 *   - expiresAt:    when this mapping expires (null = never)
 *   - hitCount:     number of times this short URL was resolved
 *   - lastAccessed: timestamp of last resolve (for LRU eviction)
 */
public class ShortenedUrl {

    private final String shortCode;
    private final String originalUrl;
    private final Instant createdAt;
    private final Instant expiresAt;      // null = no expiration
    private final AtomicLong hitCount;
    private final AtomicReference<Instant> lastAccessed;

    public ShortenedUrl(String shortCode, String originalUrl, Instant createdAt, Duration ttl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = (ttl != null) ? createdAt.plus(ttl) : null;
        this.hitCount = new AtomicLong(0);
        this.lastAccessed = new AtomicReference<>(createdAt);
    }

    /** Convenience: no expiration. */
    public ShortenedUrl(String shortCode, String originalUrl, Instant createdAt) {
        this(shortCode, originalUrl, createdAt, null);
    }

    // --- Getters ---

    public String shortCode()    { return shortCode; }
    public String originalUrl()  { return originalUrl; }
    public Instant createdAt()   { return createdAt; }
    public Instant expiresAt()   { return expiresAt; }
    public long hitCount()       { return hitCount.get(); }
    public Instant lastAccessed(){ return lastAccessed.get(); }

    // --- Mutation (thread-safe) ---

    /**
     * Records a hit (redirect access).
     * Atomically increments hit count and updates last accessed time.
     */
    public void recordHit(Instant accessTime) {
        hitCount.incrementAndGet();
        lastAccessed.set(accessTime);
    }

    /**
     * Checks if this URL has expired.
     *
     * @param now the current time
     * @return true if expired, false if still valid or no TTL set
     */
    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    @Override
    public String toString() {
        return String.format("ShortenedUrl{code='%s', url='%s', hits=%d, expires=%s}",
                shortCode, originalUrl, hitCount.get(),
                expiresAt != null ? expiresAt.toString() : "never");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ WHY AtomicLong/AtomicReference (NOT synchronized)?         │
 * │   - Lock-free: uses CAS (Compare-And-Swap) under the hood │
 * │   - No contention: multiple threads can update concurrently│
 * │   - Much faster than synchronized for simple counters      │
 * │   - hitCount is a classic "just increment" use case        │
 * │                                                             │
 * │ WHY NOT A RECORD?                                           │
 * │   Records are immutable. ShortenedUrl has mutable state    │
 * │   (hitCount, lastAccessed). Using a record would require   │
 * │   creating a new instance on every hit — wasteful and not │
 * │   thread-safe without extra synchronization.               │
 * │                                                             │
 * │ WHY STORE expiresAt INSTEAD OF ttl?                        │
 * │   - Checking expiration is O(1): now.isAfter(expiresAt)   │
 * │   - No need to compute createdAt + ttl on every check     │
 * │   - Pre-computed at creation time, never changes           │
 * └─────────────────────────────────────────────────────────────┘
 */

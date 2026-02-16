package eventtracker.retention;

import eventtracker.service.EventTracker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * ============================================================
 * EVENT RETENTION MANAGER — Lifecycle and eviction
 * ============================================================
 *
 * Manages event lifecycle through configurable retention policies.
 * Different event types can have different retention durations:
 *   - "purchase" events: 2 years (financial compliance)
 *   - "page_view" events: 90 days (low individual value)
 *   - "login" events: 1 year (security audit trail)
 *
 * Works through the EventTracker interface — not coupled to any
 * specific tracker implementation. Each tracker handles its own
 * index cleanup in removeEventsIf().
 *
 * Why retention matters at scale:
 *   1M events/day × 365 days = 365M events/year × 500 bytes = 183 GB
 *   Without retention, storage grows unbounded.
 *   Real systems use tiered storage:
 *     Hot (< 7 days):  in-memory or SSD (fast queries)
 *     Warm (< 90 days): SSD or HDD (slower queries)
 *     Cold (< 2 years): S3/GCS (batch queries only)
 *     Archive (2+ years): Glacier (compliance, rarely accessed)
 *
 * GDPR compliance:
 *   evictUser() implements "right to erasure" — complete deletion
 *   of all data for a specific user. This is a legal requirement
 *   under GDPR Article 17. Must remove from ALL indexes, not just
 *   the primary storage (a common bug in production systems).
 */
public class EventRetentionManager {

    private final EventTracker tracker;
    private final Map<String, Duration> policyMap;  // eventType → max retention
    private final Duration defaultRetention;
    private final Clock clock;

    /**
     * Creates a retention manager with per-type policies.
     *
     * @param tracker          the event tracker to manage
     * @param policies         per-type retention policies
     * @param defaultRetention fallback for event types without a specific policy
     * @param clock            injected clock for testability
     */
    public EventRetentionManager(EventTracker tracker, List<RetentionPolicy> policies,
                                 Duration defaultRetention, Clock clock) {
        this.tracker = Objects.requireNonNull(tracker);
        this.defaultRetention = Objects.requireNonNull(defaultRetention);
        this.clock = Objects.requireNonNull(clock);
        this.policyMap = new HashMap<>();
        for (RetentionPolicy p : policies) {
            policyMap.put(p.eventType(), p.retention());
        }
    }

    public EventRetentionManager(EventTracker tracker, Duration defaultRetention, Clock clock) {
        this(tracker, List.of(), defaultRetention, clock);
    }

    /**
     * Evicts all events older than the given cutoff, regardless of type.
     *
     * <p>Time Complexity: O(U × n) where U = users, n = events per user
     * <p>Space Complexity: O(1) — events are removed in-place
     *
     * @param cutoff events with timestamp before this are removed
     * @return number of events evicted
     */
    public int evictBefore(Instant cutoff) {
        return tracker.removeEventsIf(e -> e.timestamp().isBefore(cutoff));
    }

    /**
     * Removes ALL data for a specific user (GDPR right to erasure).
     *
     * <p>Time Complexity: depends on tracker implementation
     *   (IndexedEventTracker: O(n) to clean secondary indexes)
     *
     * @param userId the user whose data to purge completely
     * @return number of events removed
     */
    public int evictUser(String userId) {
        return tracker.removeAllForUser(userId);
    }

    /**
     * Applies per-type retention policies.
     *
     * <p>For each configured event type, calculates the cutoff timestamp
     * (now - retention duration) and evicts events of that type older
     * than the cutoff. Event types without a specific policy use the
     * default retention.
     *
     * <p>Time Complexity: O(T × U × n) where T = event types,
     *   U = users, n = events per user
     *
     * @return total number of events evicted across all policies
     */
    public int applyPolicies() {
        Instant now = clock.instant();
        int totalEvicted = 0;

        // Apply type-specific policies
        Set<String> coveredTypes = new HashSet<>(policyMap.keySet());
        for (var entry : policyMap.entrySet()) {
            String eventType = entry.getKey();
            Duration retention = entry.getValue();
            Instant cutoff = now.minus(retention);

            totalEvicted += tracker.removeEventsIf(e ->
                    e.eventType().equals(eventType) && e.timestamp().isBefore(cutoff));
        }

        // Apply default policy to uncovered event types
        Instant defaultCutoff = now.minus(defaultRetention);
        totalEvicted += tracker.removeEventsIf(e ->
                !coveredTypes.contains(e.eventType()) && e.timestamp().isBefore(defaultCutoff));

        return totalEvicted;
    }

    /**
     * Returns the approximate storage used by the tracker.
     *
     * @return estimated bytes
     */
    public long getStorageEstimate() {
        return tracker.storageEstimate();
    }

    /** @return the configured retention policies */
    public Map<String, Duration> getPolicies() {
        return Collections.unmodifiableMap(policyMap);
    }

    /** @return the default retention duration for unconfigured event types */
    public Duration getDefaultRetention() {
        return defaultRetention;
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — DATA RETENTION AT SCALE                   │
 * │                                                             │
 * │ WHY RETENTION IS CRITICAL:                                   │
 * │   Without it, storage grows linearly forever.                │
 * │   1M events/day × 500 bytes × 365 days = 183 GB/year       │
 * │   After 5 years: nearly 1 TB — and most of it is never      │
 * │   queried. Retention keeps hot data fast and costs low.     │
 * │                                                             │
 * │ TIERED STORAGE (how real systems do it):                    │
 * │   Hot tier:  < 7 days   → in-memory/Redis (sub-ms queries) │
 * │   Warm tier: < 90 days  → SSD/database (ms queries)        │
 * │   Cold tier: < 2 years  → S3 + Parquet (second queries)    │
 * │   Archive:   2+ years   → Glacier (minute queries, cheap)  │
 * │                                                             │
 * │ COMPACTION (instead of deletion):                           │
 * │   Don't delete old events — ROLL THEM UP:                   │
 * │   1000 page_view events from last month →                   │
 * │   1 summary record: {type: "page_view", count: 1000,       │
 * │     avg_duration: 3.2s, date: "2024-05"}                   │
 * │   Preserves aggregate insights, reduces storage 1000×.     │
 * │                                                             │
 * │ GDPR CONSIDERATIONS:                                         │
 * │   - Right to erasure (Article 17): must delete ALL user data│
 * │   - Must remove from primary storage AND all indexes        │
 * │   - Must remove from backups within "reasonable timeframe"  │
 * │   - Common bug: deleting from primary but leaving in cache  │
 * │     or secondary index → data leak                          │
 * │   - PII in properties: consider field-level encryption so   │
 * │     deletion = "lose the key" (crypto-shredding)            │
 * │                                                             │
 * │ PRODUCTION IMPLEMENTATION:                                    │
 * │   - TTL in storage layer (Cassandra TTL, DynamoDB TTL)      │
 * │   - Background compaction job runs during off-peak hours    │
 * │   - Tombstones for deleted data (mark deleted, reclaim later)│
 * │   - Partition-level deletion: drop entire time partitions   │
 * │     instead of row-by-row deletion (1000× faster)           │
 * └─────────────────────────────────────────────────────────────┘
 */

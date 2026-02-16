package eventtracker.service;

import eventtracker.model.Event;
import eventtracker.model.TrackRequest;
import eventtracker.query.EventQuery;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * ============================================================
 * EVENT TRACKER — Strategy interface for all implementations
 * ============================================================
 *
 * Common interface enabling the Strategy pattern: swap indexing
 * approaches without changing calling code. This is the foundation
 * for our comparison of LinearScan vs Indexed vs Partitioned vs InvertedIndex.
 *
 * Every analytics platform has this interface under the hood:
 *   - Segment: track() for ingestion, queries via Personas API
 *   - Mixpanel: track() for events, JQL for queries
 *   - Amplitude: logEvent() for tracking, behavioral cohorts for queries
 *
 * The interface separates:
 *   - Write path: track(), trackBatch()
 *   - Read path: query(), count(), aggregations
 *   - Lifecycle: removeEventsIf(), removeAllForUser()
 */
public interface EventTracker {

    // ==================== WRITE PATH ====================

    /**
     * Records a single event.
     *
     * @param userId     the user performing the action
     * @param eventType  the action type (e.g., "page_view", "purchase")
     * @param properties flexible metadata for this event
     * @return the created Event with generated eventId and timestamp
     */
    Event track(String userId, String eventType, Map<String, Object> properties);

    /**
     * Batch ingestion for bulk imports.
     *
     * <p>Why batch? In production, events arrive in micro-batches from Kafka.
     * Batch ingestion amortizes lock acquisition and index update costs.
     *
     * @param requests list of track requests to ingest
     */
    void trackBatch(List<TrackRequest> requests);

    // ==================== READ PATH ====================

    /**
     * Retrieves events matching the query criteria.
     *
     * @param query the query specifying filters, pagination, and sort order
     * @return list of matching events, sorted and paginated per query
     */
    List<Event> query(EventQuery query);

    /**
     * Counts matching events without materializing the list.
     * Ignores pagination (offset/limit) — counts ALL matches.
     *
     * @param query the query specifying filter criteria
     * @return count of matching events
     */
    long count(EventQuery query);

    // ==================== AGGREGATIONS ====================

    /**
     * Returns event type distribution for a user: eventType -> count.
     *
     * @param userId the user to aggregate for
     * @return map of event type to count
     */
    Map<String, Long> countByType(String userId);

    /**
     * Time-series aggregation: date string -> event count for a specific type.
     *
     * @param userId    the user to aggregate for
     * @param eventType the event type to count
     * @return map of "YYYY-MM-DD" date string to count
     */
    Map<String, Long> countByDay(String userId, String eventType);

    // ==================== CONVENIENCE METHODS ====================

    /**
     * Returns the N most recent events for a user (reverse-chronological).
     * This is the most common access pattern in activity feeds.
     *
     * @param userId the user
     * @param n      maximum events to return
     * @return up to n most recent events, newest first
     */
    List<Event> getLatestEvents(String userId, int n);

    /**
     * Returns the most recent event of a specific type for a user.
     * Answers "when did user last do X?" — critical for re-engagement.
     *
     * @param userId    the user
     * @param eventType the event type to find
     * @return the most recent event of that type, or empty
     */
    Optional<Event> getLastEventOfType(String userId, String eventType);

    /**
     * Returns userIds with at least one event since the given timestamp.
     *
     * @param since threshold timestamp
     * @return list of active user IDs
     */
    List<String> getActiveUsers(Instant since);

    // ==================== LIFECYCLE ====================

    /**
     * Removes all events matching the predicate.
     * Used by EventRetentionManager for policy-based eviction.
     *
     * @param filter predicate identifying events to remove
     * @return number of events removed
     */
    int removeEventsIf(Predicate<Event> filter);

    /**
     * Removes ALL events for a specific user (GDPR right to deletion).
     *
     * @param userId the user whose data to purge
     * @return number of events removed
     */
    int removeAllForUser(String userId);

    // ==================== METADATA ====================

    /** @return human-readable strategy name for comparison display */
    String strategyName();

    /** @return total number of events across all users */
    int size();

    /**
     * Approximate memory usage in bytes.
     * Rough estimate: ~200 bytes base per event + property storage.
     *
     * @return estimated bytes of memory used
     */
    long storageEstimate();
}

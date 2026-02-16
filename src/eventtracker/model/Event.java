package eventtracker.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * ============================================================
 * EVENT — Immutable record representing a single user action
 * ============================================================
 *
 * The atomic unit of the event tracking system. Each event captures:
 *   - WHO did it (userId)
 *   - WHAT they did (eventType)
 *   - WHEN they did it (timestamp)
 *   - HOW / context (properties map)
 *   - WHICH session (sessionId)
 *
 * Why a record?
 *   Events are inherently immutable facts — something happened, you can't
 *   un-happen it. Records give us equals/hashCode/toString for free.
 *
 * Why Map<String, Object> for properties?
 *   Flexible schema: different event types carry different metadata.
 *   "page_view" has {page: "/home", referrer: "google.com"}
 *   "purchase" has {amount: 49.99, currency: "USD", item_id: "ABC123"}
 *   This is exactly how Segment, Mixpanel, and Amplitude model events.
 *   Tradeoff: flexibility vs type safety — production systems use JSON
 *   Schema validation at ingestion to get the best of both worlds.
 *
 * Space: ~100-500 bytes per event depending on properties count
 * At scale: 1M events/day x 365 days x 500 bytes = 183 GB/year
 */
public record Event(
        String eventId,
        String userId,
        String eventType,
        Instant timestamp,
        Map<String, Object> properties,
        String sessionId
) implements Comparable<Event> {

    /**
     * Compact constructor — validates invariants and defensively copies properties.
     *
     * <p>Why copy properties? The caller might modify their map after creating
     * the event. Map.copyOf() creates an unmodifiable shallow copy — sufficient
     * because property values are typically immutable (String, Number, Boolean).
     */
    public Event {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        // sessionId may be null — not all events are sessionized
    }

    /**
     * Natural ordering: chronological by timestamp, breaking ties by eventId.
     *
     * <p>Why also compare eventId?
     *   Two events can have the same timestamp (millisecond precision).
     *   Without a tiebreaker, TreeSet/sorting would be unstable.
     *   eventId (UUID) provides a deterministic tiebreaker.
     *
     * <p>Time Complexity: O(1)
     *
     * @param other the event to compare to
     * @return negative if this event is earlier, positive if later
     */
    @Override
    public int compareTo(Event other) {
        int cmp = this.timestamp.compareTo(other.timestamp);
        return cmp != 0 ? cmp : this.eventId.compareTo(other.eventId);
    }
}

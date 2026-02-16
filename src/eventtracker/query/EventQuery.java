package eventtracker.query;

import eventtracker.model.Event;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ============================================================
 * EVENT QUERY — Fluent builder for event retrieval
 * ============================================================
 *
 * Why the Builder pattern?
 *   1. Readable chaining: EventQuery.forUser("u1").ofType("purchase").limit(10)
 *   2. Optional parameters: only set the filters you need (vs 10-param constructor)
 *   3. Self-documenting: each method name IS the documentation
 *   4. Immutability of the built query: once constructed, filter criteria are fixed
 *
 * Why NOT method overloading?
 *   With 6+ optional filters, you'd need 2^6 = 64 method overloads.
 *   Builder pattern scales to any number of optional parameters.
 *
 * Why NOT a parameter object (simple POJO with setters)?
 *   Builder enforces required params (userId via forUser) while making
 *   others optional. A POJO doesn't distinguish required from optional.
 *
 * How this maps to real query languages:
 *   - SQL: WHERE userId = ? AND eventType IN (?) AND timestamp BETWEEN ? AND ?
 *   - Elasticsearch: bool query with must clauses
 *   - MongoDB: find({userId: ?, eventType: {$in: ?}, timestamp: {$gte: ?, $lte: ?}})
 *   - The builder pattern is essentially a type-safe query DSL
 *
 * Query execution plan (which filter to apply first):
 *   1. userId (always first — partitions data by user, eliminating most events)
 *   2. eventType (if indexed, O(1) lookup narrows candidates dramatically)
 *   3. time range (can use binary search or partition pruning)
 *   4. sessionId (narrow further if sessionized)
 *   5. property filters (most expensive — requires scanning property maps)
 *   6. Apply sort, then pagination (offset + limit)
 *   This is selectivity-based ordering: apply the most selective filter first.
 */
public final class EventQuery {

    private final String userId;
    private Set<String> eventTypes;
    private Instant startTime;
    private Instant endTime;
    private final Map<String, Object> propertyFilters = new LinkedHashMap<>();
    private String sessionId;
    private int limit = Integer.MAX_VALUE;
    private int offset = 0;
    private SortOrder sortOrder = SortOrder.CHRONOLOGICAL;

    private EventQuery(String userId) {
        this.userId = Objects.requireNonNull(userId, "userId is required");
    }

    // ========== BUILDER METHODS ==========

    /**
     * Entry point: every query starts with a userId.
     * This mirrors real analytics APIs — you almost always query per-user.
     *
     * @param userId the user to query events for
     * @return new query builder
     */
    public static EventQuery forUser(String userId) {
        return new EventQuery(userId);
    }

    /**
     * Filter by one or more event types (OR semantics within types).
     *
     * @param types event types to include (e.g., "purchase", "page_view")
     * @return this builder for chaining
     */
    public EventQuery ofType(String... types) {
        this.eventTypes = Set.of(types);
        return this;
    }

    /**
     * Filter to events within a time range (inclusive on both ends).
     *
     * @param start earliest timestamp (inclusive)
     * @param end   latest timestamp (inclusive)
     * @return this builder for chaining
     */
    public EventQuery between(Instant start, Instant end) {
        this.startTime = start;
        this.endTime = end;
        return this;
    }

    /**
     * Filter by a metadata property value (AND semantics across properties).
     * Multiple calls to withProperty add additional AND conditions.
     *
     * @param key   property name
     * @param value expected value (uses Object.equals for comparison)
     * @return this builder for chaining
     */
    public EventQuery withProperty(String key, Object value) {
        this.propertyFilters.put(key, value);
        return this;
    }

    /**
     * Filter to events within a specific session.
     *
     * @param sessionId the session to filter to
     * @return this builder for chaining
     */
    public EventQuery inSession(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    /**
     * Maximum number of results to return (for pagination).
     *
     * @param n max results
     * @return this builder for chaining
     */
    public EventQuery limit(int n) {
        this.limit = n;
        return this;
    }

    /**
     * Number of results to skip before returning (for pagination).
     * Combined with limit: offset=20, limit=10 returns results 21-30.
     *
     * @param n results to skip
     * @return this builder for chaining
     */
    public EventQuery offset(int n) {
        this.offset = n;
        return this;
    }

    /**
     * Sort direction for results.
     *
     * @param order CHRONOLOGICAL (oldest first) or REVERSE_CHRONOLOGICAL (newest first)
     * @return this builder for chaining
     */
    public EventQuery orderBy(SortOrder order) {
        this.sortOrder = order;
        return this;
    }

    // ========== GETTERS (for tracker implementations) ==========

    public String userId()                    { return userId; }
    public Set<String> eventTypes()           { return eventTypes; }
    public Instant startTime()                { return startTime; }
    public Instant endTime()                  { return endTime; }
    public Map<String, Object> propertyFilters() { return Collections.unmodifiableMap(propertyFilters); }
    public String sessionId()                 { return sessionId; }
    public int limit()                        { return limit; }
    public int offset()                       { return offset; }
    public SortOrder sortOrder()              { return sortOrder; }

    // ========== QUERY EXECUTION ==========

    /**
     * Tests whether a single event matches all filter criteria.
     *
     * <p>Time Complexity: O(P) where P = number of property filters
     * <p>Space Complexity: O(1)
     *
     * @param event the event to test
     * @return true if the event matches all active filters
     */
    public boolean matches(Event event) {
        if (eventTypes != null && !eventTypes.contains(event.eventType())) return false;
        if (startTime != null && event.timestamp().isBefore(startTime)) return false;
        if (endTime != null && event.timestamp().isAfter(endTime)) return false;
        if (sessionId != null && !sessionId.equals(event.sessionId())) return false;
        for (var entry : propertyFilters.entrySet()) {
            Object actual = event.properties().get(entry.getKey());
            if (!Objects.equals(actual, entry.getValue())) return false;
        }
        return true;
    }

    /**
     * Executes the query against a list of events: filter, sort, paginate.
     *
     * <p>This method is the "query engine" — each tracker calls it with the
     * best candidate list it can produce (full user list for LinearScan,
     * type-filtered list for Indexed, time-filtered list for Partitioned).
     *
     * <p>Time Complexity: O(n) for filtering + O(n log n) for sorting
     *   (but TimSort is O(n) on already-sorted data — which our lists are)
     * <p>Space Complexity: O(k) where k = number of matching results
     *
     * @param candidates events to filter (typically already scoped to a user)
     * @return filtered, sorted, paginated results as an unmodifiable list
     */
    public List<Event> execute(List<Event> candidates) {
        Comparator<Event> cmp = sortOrder == SortOrder.REVERSE_CHRONOLOGICAL
                ? Comparator.reverseOrder()
                : Comparator.naturalOrder();

        return candidates.stream()
                .filter(this::matches)
                .sorted(cmp)
                .skip(offset)
                .limit(limit)
                .toList();
    }

    /**
     * Counts matching events without materializing the full list.
     * Ignores pagination (offset/limit) — counts ALL matches.
     *
     * <p>Time Complexity: O(n)
     * <p>Space Complexity: O(1) — stream doesn't collect
     *
     * @param candidates events to count matches in
     * @return number of matching events
     */
    public long executeCount(List<Event> candidates) {
        return candidates.stream().filter(this::matches).count();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EventQuery{user=").append(userId);
        if (eventTypes != null) sb.append(", types=").append(eventTypes);
        if (startTime != null) sb.append(", from=").append(startTime);
        if (endTime != null) sb.append(", to=").append(endTime);
        if (!propertyFilters.isEmpty()) sb.append(", props=").append(propertyFilters);
        if (sessionId != null) sb.append(", session=").append(sessionId);
        if (offset > 0) sb.append(", offset=").append(offset);
        if (limit < Integer.MAX_VALUE) sb.append(", limit=").append(limit);
        sb.append(", order=").append(sortOrder);
        return sb.append('}').toString();
    }
}

package eventtracker.service;

import eventtracker.model.Event;
import eventtracker.model.TrackRequest;
import eventtracker.query.EventQuery;
import eventtracker.query.SortOrder;
import eventtracker.session.SessionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ============================================================
 * INDEXED EVENT TRACKER — Primary implementation with secondary indexes
 * ============================================================
 *
 * Maintains secondary indexes updated on every write so that filtered
 * queries can skip the full-list scan. This is the core tradeoff:
 *   FASTER READS at the cost of SLOWER WRITES and MORE MEMORY.
 *
 * Data Structures:
 *
 *   PRIMARY STORAGE (required):
 *   userEvents: ConcurrentHashMap<userId, CopyOnWriteArrayList<Event>>
 *   └── "user1" → [e1, e2, e3, e4, e5]  (chronological)
 *   └── "user2" → [e6, e7, e8]
 *
 *   SECONDARY INDEX — by type (per user):
 *   userTypeIndex: ConcurrentHashMap<"userId:type", CopyOnWriteArrayList<Event>>
 *   └── "user1:purchase" → [e2, e5]     (only purchase events for user1)
 *   └── "user1:page_view" → [e1, e3, e4] (only page_views for user1)
 *
 *   SECONDARY INDEX — by type (global):
 *   typeIndex: ConcurrentHashMap<type, CopyOnWriteArrayList<Event>>
 *   └── "purchase" → [e2, e5, e7]  (all purchases across all users)
 *
 *   SECONDARY INDEX — by day (per user):
 *   userDayIndex: ConcurrentHashMap<"userId:YYYY-MM-DD", CopyOnWriteArrayList<Event>>
 *   └── "user1:2024-06-15" → [e1, e2, e3] (user1's events on June 15)
 *
 * Write Amplification:
 *   Each track() call updates 4 maps. This is the "write amplification"
 *   cost of maintaining indexes. One logical write → 4 physical writes.
 *   At scale, this is mitigated by:
 *     - Batching index updates (update all indexes after N writes)
 *     - Async index updates (write to primary, queue index updates)
 *     - Write-ahead log: write event to WAL first, indexes updated asynchronously
 *
 * Memory Cost:
 *   Secondary indexes store REFERENCES to the same Event objects, not copies.
 *   Memory overhead = ~8 bytes per reference per index per event.
 *   For 1M events with 3 secondary indexes: 1M × 3 × 8 = 24 MB overhead.
 *   This is a small price for O(1) lookups instead of O(n) scans.
 *
 * Complexity Summary:
 *   track():              O(n) per CopyOnWriteArrayList copy × 4 indexes
 *   query(type filter):   O(k) where k = events of that type for user (vs O(n))
 *   query(no filter):     O(n) same as LinearScan
 *   countByType():        O(1) per type using index sizes
 *   countByDay():         O(D) where D = distinct days (vs O(n) scan)
 *   getLastEventOfType(): O(1) — last element in type-indexed list
 */
public class IndexedEventTracker implements EventTracker {

    // ========== PRIMARY STORAGE ==========
    // userId → chronologically ordered event list
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> userEvents = new ConcurrentHashMap<>();

    // ========== SECONDARY INDEXES ==========
    // Why maintain these? Without them, every filtered query scans the FULL user event list.
    // With them, we jump directly to matching events in O(1).

    // Global type index: eventType → all events of that type across all users
    // Use case: "How many total purchases happened today?"
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> typeIndex = new ConcurrentHashMap<>();

    // Per-user type index: "userId:eventType" → events for that user of that type
    // Use case: "Show me user1's purchase history" → O(1) lookup vs O(n) scan
    // This is the MOST VALUABLE secondary index in analytics — the most common query
    // pattern is "get events of type X for user Y"
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> userTypeIndex = new ConcurrentHashMap<>();

    // Per-user day index: "userId:YYYY-MM-DD" → events for that user on that day
    // Use case: "countByDay" aggregation, time-range queries that align to day boundaries
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> userDayIndex = new ConcurrentHashMap<>();

    private final Clock clock;
    private final Supplier<String> idGenerator;
    private final SessionManager sessionManager; // nullable
    private final AtomicInteger totalSize = new AtomicInteger(0);

    public IndexedEventTracker(Clock clock, Supplier<String> idGenerator, SessionManager sessionManager) {
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.sessionManager = sessionManager;
    }

    public IndexedEventTracker(Clock clock, Supplier<String> idGenerator) {
        this(clock, idGenerator, null);
    }

    // ==================== WRITE PATH ====================

    /**
     * Records an event and updates ALL secondary indexes.
     *
     * <p>Time Complexity: O(n) per CopyOnWriteArrayList.add() × 4 data structures
     *   = O(4n) = O(n) where n = current list size for each structure
     * <p>Space Complexity: O(1) new references (Event object shared, not copied)
     *
     * <p>Write Amplification: 1 logical event → 4 physical map updates.
     * If track() succeeds on primary but fails before updating all indexes,
     * we'd have an inconsistency. In production, solve with:
     *   (a) Write-ahead log: log the event first, apply to all indexes from WAL
     *   (b) Atomic batch: update all data structures in one synchronized block
     *   (c) Eventual consistency: background reconciliation job
     */
    @Override
    public Event track(String userId, String eventType, Map<String, Object> properties) {
        Instant now = clock.instant();
        String sessionId = sessionManager != null
                ? sessionManager.getOrCreateSession(userId, now) : null;

        Event event = new Event(idGenerator.get(), userId, eventType, now, properties, sessionId);

        // 1. Primary storage
        userEvents.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(event);

        // 2. Global type index
        typeIndex.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);

        // 3. Per-user type index
        String userTypeKey = userId + ":" + eventType;
        userTypeIndex.computeIfAbsent(userTypeKey, k -> new CopyOnWriteArrayList<>()).add(event);

        // 4. Per-user day index
        String dayKey = userId + ":" + LocalDate.ofInstant(now, ZoneOffset.UTC);
        userDayIndex.computeIfAbsent(dayKey, k -> new CopyOnWriteArrayList<>()).add(event);

        totalSize.incrementAndGet();
        return event;
    }

    @Override
    public void trackBatch(List<TrackRequest> requests) {
        for (TrackRequest req : requests) {
            track(req.userId(), req.eventType(), req.properties());
        }
    }

    // ==================== READ PATH ====================

    /**
     * Query execution with index optimization.
     *
     * <p>Optimization strategy:
     *   1. If query filters by a single eventType → use userTypeIndex (O(1) lookup)
     *   2. If query filters by multiple types → union of userTypeIndex lookups
     *   3. Otherwise → fall back to full user event list scan
     *
     * <p>After selecting the candidate list, remaining filters (time range,
     * properties, session) are applied by EventQuery.execute().
     *
     * <p>Time Complexity:
     *   With type filter: O(k) where k = events of that type for user
     *   Without type filter: O(n) where n = all user events
     */
    @Override
    public List<Event> query(EventQuery query) {
        List<Event> candidates = getCandidates(query);
        return query.execute(candidates);
    }

    @Override
    public long count(EventQuery query) {
        List<Event> candidates = getCandidates(query);
        return query.executeCount(candidates);
    }

    /**
     * Selects the best candidate list based on query filters.
     * This is a simple "query planner" — real databases do this with statistics.
     */
    private List<Event> getCandidates(EventQuery query) {
        if (query.eventTypes() != null && !query.eventTypes().isEmpty()) {
            if (query.eventTypes().size() == 1) {
                // Single type — direct index lookup, O(1)
                String type = query.eventTypes().iterator().next();
                String key = query.userId() + ":" + type;
                return userTypeIndex.getOrDefault(key, new CopyOnWriteArrayList<>());
            } else {
                // Multiple types — union of index lookups
                List<Event> union = new ArrayList<>();
                for (String type : query.eventTypes()) {
                    String key = query.userId() + ":" + type;
                    union.addAll(userTypeIndex.getOrDefault(key, new CopyOnWriteArrayList<>()));
                }
                // Must sort the union since events come from different type lists
                union.sort(Comparator.naturalOrder());
                return union;
            }
        }
        // No type filter — fall back to full user event list
        return userEvents.getOrDefault(query.userId(), new CopyOnWriteArrayList<>());
    }

    // ==================== AGGREGATIONS ====================

    /**
     * Event type distribution — leverages userTypeIndex for O(T) instead of O(n).
     *
     * <p>Time Complexity: O(T) where T = number of distinct event types
     *   (vs O(n) in LinearScan — we just check index list sizes)
     */
    @Override
    public Map<String, Long> countByType(String userId) {
        Map<String, Long> result = new LinkedHashMap<>();
        // Scan type index keys that start with this userId
        String prefix = userId + ":";
        for (var entry : userTypeIndex.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String type = entry.getKey().substring(prefix.length());
                result.put(type, (long) entry.getValue().size());
            }
        }
        return result;
    }

    /**
     * Time-series aggregation using the day index.
     *
     * <p>Time Complexity: O(D) where D = distinct days the user has events
     *   (vs O(n) in LinearScan where n = all user events)
     */
    @Override
    public Map<String, Long> countByDay(String userId, String eventType) {
        Map<String, Long> result = new TreeMap<>(); // TreeMap for chronological day ordering
        String prefix = userId + ":";
        for (var entry : userDayIndex.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String day = entry.getKey().substring(prefix.length());
                long count = entry.getValue().stream()
                        .filter(e -> e.eventType().equals(eventType))
                        .count();
                if (count > 0) result.put(day, count);
            }
        }
        return result;
    }

    // ==================== CONVENIENCE ====================

    /**
     * N most recent events.
     *
     * <p>Time Complexity: O(n) to create reversed copy
     *   (same as LinearScan — could optimize with a reverse-order secondary structure)
     */
    @Override
    public List<Event> getLatestEvents(String userId, int n) {
        List<Event> events = userEvents.getOrDefault(userId, new CopyOnWriteArrayList<>());
        int size = events.size();
        int start = Math.max(0, size - n);
        List<Event> latest = new ArrayList<>();
        for (int i = size - 1; i >= start; i--) {
            latest.add(events.get(i));
        }
        return latest;
    }

    /**
     * Most recent event of type — O(1) using userTypeIndex!
     *
     * <p>Time Complexity: O(1) — just get last element of the type-indexed list
     *   (vs O(n) reverse scan in LinearScan — this is where indexes shine)
     */
    @Override
    public Optional<Event> getLastEventOfType(String userId, String eventType) {
        String key = userId + ":" + eventType;
        CopyOnWriteArrayList<Event> events = userTypeIndex.get(key);
        if (events == null || events.isEmpty()) return Optional.empty();
        return Optional.of(events.get(events.size() - 1));
    }

    @Override
    public List<String> getActiveUsers(Instant since) {
        List<String> active = new ArrayList<>();
        for (var entry : userEvents.entrySet()) {
            List<Event> events = entry.getValue();
            if (!events.isEmpty()) {
                Event last = events.get(events.size() - 1);
                if (!last.timestamp().isBefore(since)) {
                    active.add(entry.getKey());
                }
            }
        }
        return active;
    }

    // ==================== LIFECYCLE ====================

    /**
     * Removes matching events from primary AND all secondary indexes.
     *
     * <p>This is the expensive part of maintaining indexes: deletions must
     * be propagated to every index. One logical delete → 4 physical deletes.
     *
     * <p>Time Complexity: O(U * n) for scanning + O(removed) for index cleanup
     */
    @Override
    public int removeEventsIf(Predicate<Event> filter) {
        AtomicInteger removed = new AtomicInteger(0);

        userEvents.forEach((userId, events) -> {
            List<Event> toRemove = events.stream().filter(filter).toList();
            if (toRemove.isEmpty()) return;

            events.removeAll(toRemove);
            removed.addAndGet(toRemove.size());
            totalSize.addAndGet(-toRemove.size());

            // Clean up secondary indexes
            for (Event e : toRemove) {
                removeFromIndex(typeIndex, e.eventType(), e);
                removeFromIndex(userTypeIndex, userId + ":" + e.eventType(), e);
                String dayKey = userId + ":" + LocalDate.ofInstant(e.timestamp(), ZoneOffset.UTC);
                removeFromIndex(userDayIndex, dayKey, e);
            }
        });

        return removed.get();
    }

    @Override
    public int removeAllForUser(String userId) {
        CopyOnWriteArrayList<Event> events = userEvents.remove(userId);
        if (events == null) return 0;

        int count = events.size();
        totalSize.addAndGet(-count);

        // Clean secondary indexes
        for (Event e : events) {
            removeFromIndex(typeIndex, e.eventType(), e);
            removeFromIndex(userTypeIndex, userId + ":" + e.eventType(), e);
            String dayKey = userId + ":" + LocalDate.ofInstant(e.timestamp(), ZoneOffset.UTC);
            removeFromIndex(userDayIndex, dayKey, e);
        }

        return count;
    }

    /** Helper: remove an event from a secondary index list */
    private void removeFromIndex(ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> index,
                                 String key, Event event) {
        CopyOnWriteArrayList<Event> list = index.get(key);
        if (list != null) {
            list.remove(event);
            if (list.isEmpty()) index.remove(key);
        }
    }

    // ==================== METADATA ====================

    @Override
    public String strategyName() { return "Indexed (type + day indexes)"; }

    @Override
    public int size() { return totalSize.get(); }

    @Override
    public long storageEstimate() {
        // Primary: ~208 bytes per event (object + reference)
        // Secondary indexes: ~24 bytes per event (3 indexes × 8 bytes per reference)
        return (long) totalSize.get() * 232;
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — SECONDARY INDEXING                        │
 * │                                                             │
 * │ THE FUNDAMENTAL TRADEOFF:                                    │
 * │   Faster reads ← → Slower writes + More memory              │
 * │   This is the SAME tradeoff as adding a B-tree index in     │
 * │   PostgreSQL: CREATE INDEX ON events(event_type)            │
 * │   SELECT is faster, INSERT/UPDATE/DELETE are slower.         │
 * │                                                             │
 * │ WHEN TO INDEX:                                               │
 * │   1. High read/write ratio (analytics: 100:1 reads:writes) │
 * │   2. Known query patterns (we KNOW users filter by type)    │
 * │   3. Low cardinality field (eventType has ~20 values,       │
 * │      not ~1M values — few distinct values means each        │
 * │      index list has many events, making it worthwhile)      │
 * │                                                             │
 * │ WHEN NOT TO INDEX:                                           │
 * │   1. High cardinality (userId has millions of values —      │
 * │      each index list has ~1 entry, pointless)               │
 * │   2. Write-heavy workload (100K writes/sec would mean       │
 * │      400K index updates/sec with 4 indexes)                 │
 * │   3. Unknown query patterns (index the wrong field and      │
 * │      you waste memory + write throughput for nothing)       │
 * │                                                             │
 * │ MEMORY COST:                                                 │
 * │   Secondary indexes store REFERENCES (8 bytes each),        │
 * │   not copies of the Event. 3 indexes × 1M events × 8B =    │
 * │   24 MB — trivial compared to the 200 MB for Event objects. │
 * │   This is analogous to database indexes: the index stores   │
 * │   pointers to rows, not row copies.                          │
 * │                                                             │
 * │ CONSISTENCY CHALLENGE:                                       │
 * │   If track() crashes after updating primary but before      │
 * │   updating all indexes, you have inconsistency.             │
 * │   Solutions:                                                 │
 * │   (a) Write-ahead log (WAL): log event, then apply to all  │
 * │       structures. On crash recovery, replay WAL.            │
 * │   (b) Synchronized block: update all structures atomically  │
 * │       (simple but limits concurrency).                       │
 * │   (c) Eventual consistency: background reconciliation       │
 * │       rebuilds indexes from primary (most common at scale). │
 * │                                                             │
 * │ CONNECTION TO DATABASES:                                      │
 * │   This is LITERALLY what a database index does:              │
 * │   - B-tree index on type column = our typeIndex             │
 * │   - Composite index on (userId, type) = our userTypeIndex   │
 * │   - Covering index = storing the data IN the index          │
 * │   The difference: we do it in-memory, databases do it on    │
 * │   disk with more sophisticated structures (B+ trees,        │
 * │   LSM trees) that handle persistence and crash recovery.     │
 * └─────────────────────────────────────────────────────────────┘
 */

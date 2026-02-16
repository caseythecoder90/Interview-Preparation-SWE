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
 * LINEAR SCAN TRACKER — Baseline implementation
 * ============================================================
 *
 * The simplest possible event tracker: one HashMap mapping userId
 * to an ordered list of events. All queries scan the full user list.
 *
 * Why build this "bad" version?
 *   1. Establishes the baseline — you can't optimize what you can't measure
 *   2. It's actually NOT bad for small datasets (< 10K events per user)
 *   3. No index maintenance overhead → fastest writes
 *   4. Interview strategy: "Here's the simple O(n) version. Now let me
 *      show you how secondary indexes bring type queries to O(1)..."
 *
 * Data Structure:
 *   ConcurrentHashMap<String, CopyOnWriteArrayList<Event>>
 *   └── userId ──────────────► [event0, event1, event2, ...]
 *                                (chronological order, maintained by append)
 *
 * Why ArrayList over LinkedList for event storage?
 *   - Cache locality: ArrayList stores elements in contiguous memory,
 *     so sequential scanning (our primary access pattern) benefits from
 *     CPU cache prefetching. LinkedList nodes are scattered in heap.
 *   - O(1) random access: pagination (skip N, take M) is O(1) with ArrayList
 *     index arithmetic, but O(N) with LinkedList traversal.
 *   - Lower memory overhead: LinkedList has 24 bytes overhead per node
 *     (prev + next pointers + node object header). ArrayList has ~0 bytes
 *     per element (just the reference in the backing array).
 *   - Append is O(1) amortized for both, but ArrayList's amortized constant
 *     is smaller due to fewer allocations and better cache behavior.
 *
 * Why CopyOnWriteArrayList?
 *   - Reads (queries) never block, even during concurrent writes
 *   - Iterator never throws ConcurrentModificationException
 *   - Perfect for read-heavy analytics workloads (100:1 read:write)
 *   - TRADEOFF: each write copies the entire backing array → O(n) per write
 *   - Becomes a bottleneck when: user has 100K+ events AND write rate > 100/sec
 *   - At that point, switch to synchronized ArrayList + snapshot-on-read
 *
 * Complexity Summary:
 *   track():              O(n) where n = user's event count (array copy)
 *   trackBatch():         O(B * n) where B = batch size
 *   query():              O(n) scan + O(k log k) sort where k = matches
 *   count():              O(n) scan
 *   countByType():        O(n) scan
 *   countByDay():         O(n) scan
 *   getLatestEvents():    O(n) for reverse scan
 *   getLastEventOfType(): O(n) reverse scan
 *   getActiveUsers():     O(U * n) where U = number of users
 *   removeEventsIf():     O(U * n)
 *   removeAllForUser():   O(1) map remove
 */
public class LinearScanTracker implements EventTracker {

    // Single data structure — the whole point of this "baseline" implementation
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> userEvents = new ConcurrentHashMap<>();

    private final Clock clock;
    private final Supplier<String> idGenerator;
    private final SessionManager sessionManager; // nullable
    private final AtomicInteger totalSize = new AtomicInteger(0);

    public LinearScanTracker(Clock clock, Supplier<String> idGenerator, SessionManager sessionManager) {
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.sessionManager = sessionManager;
    }

    public LinearScanTracker(Clock clock, Supplier<String> idGenerator) {
        this(clock, idGenerator, null);
    }

    // ==================== WRITE PATH ====================

    /**
     * Records a single event. O(n) due to CopyOnWriteArrayList copy.
     *
     * <p>Time Complexity: O(n) where n = current user event count
     *   (CopyOnWriteArrayList copies the entire array on add)
     * <p>Space Complexity: O(1) amortized (one new Event object + array copy)
     */
    @Override
    public Event track(String userId, String eventType, Map<String, Object> properties) {
        Instant now = clock.instant();
        String sessionId = sessionManager != null
                ? sessionManager.getOrCreateSession(userId, now) : null;

        Event event = new Event(idGenerator.get(), userId, eventType, now, properties, sessionId);
        userEvents.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(event);
        totalSize.incrementAndGet();
        return event;
    }

    /**
     * Batch ingestion. Simply delegates to track() for each request.
     *
     * <p>Time Complexity: O(B * n) where B = batch size
     */
    @Override
    public void trackBatch(List<TrackRequest> requests) {
        for (TrackRequest req : requests) {
            track(req.userId(), req.eventType(), req.properties());
        }
    }

    // ==================== READ PATH ====================

    /**
     * Retrieves events matching query by scanning the full user event list.
     *
     * <p>Time Complexity: O(n) filter + O(k log k) sort, where n = user events, k = matches
     *   (TimSort is O(k) if list is already sorted — which it is for CHRONOLOGICAL)
     * <p>Space Complexity: O(k) for the result list
     */
    @Override
    public List<Event> query(EventQuery query) {
        List<Event> events = userEvents.getOrDefault(query.userId(), new CopyOnWriteArrayList<>());
        return query.execute(events);
    }

    /**
     * Counts matching events without materializing.
     *
     * <p>Time Complexity: O(n) — must scan entire user list
     */
    @Override
    public long count(EventQuery query) {
        List<Event> events = userEvents.getOrDefault(query.userId(), new CopyOnWriteArrayList<>());
        return query.executeCount(events);
    }

    // ==================== AGGREGATIONS ====================

    /**
     * Event type distribution. Scans all user events, groups by type.
     *
     * <p>Time Complexity: O(n) where n = user's event count
     * <p>Space Complexity: O(T) where T = number of distinct event types
     */
    @Override
    public Map<String, Long> countByType(String userId) {
        List<Event> events = userEvents.getOrDefault(userId, new CopyOnWriteArrayList<>());
        return events.stream()
                .collect(Collectors.groupingBy(Event::eventType, Collectors.counting()));
    }

    /**
     * Time-series aggregation: day -> count for a specific event type.
     *
     * <p>Time Complexity: O(n) scan + O(k) grouping where k = matching events
     */
    @Override
    public Map<String, Long> countByDay(String userId, String eventType) {
        List<Event> events = userEvents.getOrDefault(userId, new CopyOnWriteArrayList<>());
        return events.stream()
                .filter(e -> e.eventType().equals(eventType))
                .collect(Collectors.groupingBy(
                        e -> LocalDate.ofInstant(e.timestamp(), ZoneOffset.UTC).toString(),
                        Collectors.counting()
                ));
    }

    // ==================== CONVENIENCE ====================

    /**
     * Returns N most recent events, newest first.
     *
     * <p>Time Complexity: O(n) to reverse + O(1) per element to take N
     *   (could optimize by iterating from end, but Stream API is cleaner)
     */
    @Override
    public List<Event> getLatestEvents(String userId, int n) {
        List<Event> events = userEvents.getOrDefault(userId, new CopyOnWriteArrayList<>());
        // Events are in chronological order; reverse and take first N
        List<Event> reversed = new ArrayList<>(events);
        Collections.reverse(reversed);
        return reversed.stream().limit(n).toList();
    }

    /**
     * Most recent event of a specific type.
     *
     * <p>Time Complexity: O(n) — must scan all events to find last of type
     *   (with a type index, this would be O(1))
     */
    @Override
    public Optional<Event> getLastEventOfType(String userId, String eventType) {
        List<Event> events = userEvents.getOrDefault(userId, new CopyOnWriteArrayList<>());
        // Iterate in reverse to find the last occurrence
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).eventType().equals(eventType)) {
                return Optional.of(events.get(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Active users since a timestamp.
     *
     * <p>Time Complexity: O(U * n) worst case — check each user's most recent event
     *   Optimization: check last event first (most likely to be recent)
     */
    @Override
    public List<String> getActiveUsers(Instant since) {
        List<String> active = new ArrayList<>();
        for (var entry : userEvents.entrySet()) {
            List<Event> events = entry.getValue();
            // Check last event first (optimization: most users' latest event is recent)
            if (!events.isEmpty()) {
                Event last = events.get(events.size() - 1);
                if (!last.timestamp().isBefore(since)) {
                    active.add(entry.getKey());
                    continue;
                }
            }
            // Fall back to scanning (handles out-of-order edge cases)
            for (Event e : events) {
                if (!e.timestamp().isBefore(since)) {
                    active.add(entry.getKey());
                    break;
                }
            }
        }
        return active;
    }

    // ==================== LIFECYCLE ====================

    /**
     * Removes events matching predicate from all users.
     *
     * <p>Time Complexity: O(U * n) where U = users, n = max events per user
     *   Each CopyOnWriteArrayList.removeIf() copies the array.
     */
    @Override
    public int removeEventsIf(Predicate<Event> filter) {
        AtomicInteger removed = new AtomicInteger(0);
        userEvents.forEach((userId, events) -> {
            int before = events.size();
            events.removeIf(filter);
            int delta = before - events.size();
            removed.addAndGet(delta);
            totalSize.addAndGet(-delta);
        });
        return removed.get();
    }

    /**
     * Removes all events for a user.
     *
     * <p>Time Complexity: O(1) for map removal
     */
    @Override
    public int removeAllForUser(String userId) {
        CopyOnWriteArrayList<Event> removed = userEvents.remove(userId);
        int count = removed != null ? removed.size() : 0;
        totalSize.addAndGet(-count);
        return count;
    }

    // ==================== METADATA ====================

    @Override
    public String strategyName() { return "LinearScan (no indexes)"; }

    @Override
    public int size() { return totalSize.get(); }

    @Override
    public long storageEstimate() {
        // ~200 bytes per Event object + 8 bytes per reference in ArrayList
        return (long) totalSize.get() * 208;
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — LINEAR SCAN BASELINE                      │
 * │                                                             │
 * │ WHEN LINEAR SCAN IS ACTUALLY FINE:                          │
 * │   - Fewer than ~10K events per user                         │
 * │   - Queries don't filter by type (rare in practice)         │
 * │   - Write-heavy workload (no index maintenance)             │
 * │   - Prototyping / MVP where query patterns aren't known yet │
 * │                                                             │
 * │ WHEN IT BREAKS DOWN:                                        │
 * │   - Power user with 1M events: every query scans 1M records │
 * │   - Filtering by type or property: O(n) per query           │
 * │   - countByDay for one type: scans ALL events, not just     │
 * │     that type — wasted work                                 │
 * │                                                             │
 * │ THE TRANSITION TO INDEXING:                                   │
 * │   "I'd start with LinearScan for simplicity, monitor query  │
 * │    latencies in production, then add secondary indexes on    │
 * │    the fields that appear in the most queries. In analytics, │
 * │    eventType is almost always the first index you add."     │
 * │                                                             │
 * │ REAL-WORLD PARALLEL:                                         │
 * │   This is a table scan in SQL (SELECT * FROM events WHERE   │
 * │   userId = ? AND type = ? — no index on type). PostgreSQL   │
 * │   EXPLAIN would show "Seq Scan" instead of "Index Scan."    │
 * └─────────────────────────────────────────────────────────────┘
 */

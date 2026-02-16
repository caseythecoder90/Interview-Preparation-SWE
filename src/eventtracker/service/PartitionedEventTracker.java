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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ============================================================
 * PARTITIONED EVENT TRACKER — Time-bucketed with TreeMap
 * ============================================================
 *
 * Organizes events into daily partitions per user using a NavigableMap.
 * Time-range queries use subMap() for O(log D) partition selection,
 * where D = number of distinct days — dramatically faster than scanning
 * all events when the time range is narrow.
 *
 * Data Structure:
 *   ConcurrentHashMap<userId, ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>>>
 *   └── "user1" → { 2024-06-14 → [e1, e2],
 *                    2024-06-15 → [e3, e4, e5],
 *                    2024-06-16 → [e6] }
 *
 * Why ConcurrentSkipListMap (not TreeMap)?
 *   - TreeMap is NOT thread-safe
 *   - ConcurrentSkipListMap is the concurrent equivalent of TreeMap
 *   - Provides O(log n) access AND supports subMap/headMap/tailMap
 *   - Skip list internally: probabilistic balanced structure, similar
 *     performance to red-black tree but with better concurrency
 *
 * Query Optimization for Time Ranges:
 *   Query: events between June 14 and June 15
 *   LinearScan: iterate ALL events, check timestamp → O(n)
 *   Partitioned: subMap(June14, June15) → get only those 2 days → O(log D + k)
 *     where D = total distinct days, k = events in matching days
 *
 * This mirrors real-world time-series databases:
 *   - Cassandra: partition by date for time-series data
 *   - InfluxDB: TSM files organized by time ranges
 *   - TimescaleDB: hypertables partitioned by time
 *
 * Complexity Summary:
 *   track():           O(log D) for TreeMap insert + O(n) for COW list copy
 *   query(time range): O(log D + k) where D = days, k = events in range
 *   query(no range):   O(n) must flatten all partitions
 *   countByDay():      O(D) iterate day keys, count per day
 */
public class PartitionedEventTracker implements EventTracker {

    // userId → sorted day partitions → events on that day
    private final ConcurrentHashMap<String, ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>>>
            userPartitions = new ConcurrentHashMap<>();

    private final Clock clock;
    private final Supplier<String> idGenerator;
    private final SessionManager sessionManager;
    private final AtomicInteger totalSize = new AtomicInteger(0);

    public PartitionedEventTracker(Clock clock, Supplier<String> idGenerator, SessionManager sessionManager) {
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.sessionManager = sessionManager;
    }

    public PartitionedEventTracker(Clock clock, Supplier<String> idGenerator) {
        this(clock, idGenerator, null);
    }

    // ==================== WRITE PATH ====================

    /**
     * Records event into the correct daily partition.
     *
     * <p>Time Complexity: O(log D) for ConcurrentSkipListMap access
     *   + O(n) for CopyOnWriteArrayList copy
     * <p>Space Complexity: O(1) — one Event object, one reference
     */
    @Override
    public Event track(String userId, String eventType, Map<String, Object> properties) {
        Instant now = clock.instant();
        String sessionId = sessionManager != null
                ? sessionManager.getOrCreateSession(userId, now) : null;

        Event event = new Event(idGenerator.get(), userId, eventType, now, properties, sessionId);
        LocalDate day = LocalDate.ofInstant(now, ZoneOffset.UTC);

        userPartitions
                .computeIfAbsent(userId, k -> new ConcurrentSkipListMap<>())
                .computeIfAbsent(day, k -> new CopyOnWriteArrayList<>())
                .add(event);

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
     * Query with time-range optimization via subMap().
     *
     * <p>If query has a time range, use subMap() to select only relevant partitions.
     * This is where PartitionedEventTracker shines — narrow time ranges skip
     * the vast majority of events.
     *
     * <p>Time Complexity:
     *   With time range: O(log D + k) where D = days, k = events in range days
     *   Without time range: O(n) — must flatten all partitions
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

    private List<Event> getCandidates(EventQuery query) {
        ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>> partitions =
                userPartitions.get(query.userId());
        if (partitions == null) return List.of();

        NavigableMap<LocalDate, CopyOnWriteArrayList<Event>> relevantPartitions;

        if (query.startTime() != null && query.endTime() != null) {
            // TIME RANGE OPTIMIZATION: use subMap to prune irrelevant days
            // This is O(log D) instead of O(D) — the key advantage of partitioning
            LocalDate startDay = LocalDate.ofInstant(query.startTime(), ZoneOffset.UTC);
            LocalDate endDay = LocalDate.ofInstant(query.endTime(), ZoneOffset.UTC);
            relevantPartitions = partitions.subMap(startDay, true, endDay, true);
        } else if (query.startTime() != null) {
            LocalDate startDay = LocalDate.ofInstant(query.startTime(), ZoneOffset.UTC);
            relevantPartitions = partitions.tailMap(startDay, true);
        } else if (query.endTime() != null) {
            LocalDate endDay = LocalDate.ofInstant(query.endTime(), ZoneOffset.UTC);
            relevantPartitions = partitions.headMap(endDay, true);
        } else {
            relevantPartitions = partitions;
        }

        // Flatten selected partitions into a single list
        List<Event> candidates = new ArrayList<>();
        for (CopyOnWriteArrayList<Event> dayEvents : relevantPartitions.values()) {
            candidates.addAll(dayEvents);
        }
        return candidates;
    }

    // ==================== AGGREGATIONS ====================

    @Override
    public Map<String, Long> countByType(String userId) {
        return getAllUserEvents(userId).stream()
                .collect(Collectors.groupingBy(Event::eventType, Collectors.counting()));
    }

    /**
     * Day-level aggregation — O(D) using partition keys directly.
     *
     * <p>Time Complexity: O(D * avg_events_per_day) for type filtering
     *   (but much better than O(n) when D is small relative to n)
     */
    @Override
    public Map<String, Long> countByDay(String userId, String eventType) {
        ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>> partitions =
                userPartitions.get(userId);
        if (partitions == null) return Map.of();

        Map<String, Long> result = new TreeMap<>();
        for (var entry : partitions.entrySet()) {
            long count = entry.getValue().stream()
                    .filter(e -> e.eventType().equals(eventType))
                    .count();
            if (count > 0) result.put(entry.getKey().toString(), count);
        }
        return result;
    }

    // ==================== CONVENIENCE ====================

    @Override
    public List<Event> getLatestEvents(String userId, int n) {
        List<Event> all = getAllUserEvents(userId);
        int size = all.size();
        int start = Math.max(0, size - n);
        List<Event> latest = new ArrayList<>();
        for (int i = size - 1; i >= start; i--) {
            latest.add(all.get(i));
        }
        return latest;
    }

    @Override
    public Optional<Event> getLastEventOfType(String userId, String eventType) {
        ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>> partitions =
                userPartitions.get(userId);
        if (partitions == null) return Optional.empty();

        // Iterate partitions in reverse chronological order
        for (var entry : partitions.descendingMap().entrySet()) {
            List<Event> dayEvents = entry.getValue();
            for (int i = dayEvents.size() - 1; i >= 0; i--) {
                if (dayEvents.get(i).eventType().equals(eventType)) {
                    return Optional.of(dayEvents.get(i));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> getActiveUsers(Instant since) {
        LocalDate sinceDay = LocalDate.ofInstant(since, ZoneOffset.UTC);
        List<String> active = new ArrayList<>();
        for (var entry : userPartitions.entrySet()) {
            // Check if any partition from sinceDay onwards has events
            NavigableMap<LocalDate, CopyOnWriteArrayList<Event>> recent =
                    entry.getValue().tailMap(sinceDay, true);
            if (!recent.isEmpty()) {
                // Verify at least one event is actually after 'since'
                for (var dayEvents : recent.values()) {
                    if (dayEvents.stream().anyMatch(e -> !e.timestamp().isBefore(since))) {
                        active.add(entry.getKey());
                        break;
                    }
                }
            }
        }
        return active;
    }

    // ==================== LIFECYCLE ====================

    @Override
    public int removeEventsIf(Predicate<Event> filter) {
        AtomicInteger removed = new AtomicInteger(0);
        userPartitions.forEach((userId, partitions) -> {
            Iterator<Map.Entry<LocalDate, CopyOnWriteArrayList<Event>>> it = partitions.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                CopyOnWriteArrayList<Event> events = entry.getValue();
                int before = events.size();
                events.removeIf(filter);
                int delta = before - events.size();
                removed.addAndGet(delta);
                totalSize.addAndGet(-delta);
                if (events.isEmpty()) it.remove();
            }
        });
        return removed.get();
    }

    @Override
    public int removeAllForUser(String userId) {
        ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>> partitions = userPartitions.remove(userId);
        if (partitions == null) return 0;
        int count = partitions.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
        totalSize.addAndGet(-count);
        return count;
    }

    // ==================== HELPERS ====================

    /** Flattens all partitions for a user into a single chronological list. */
    private List<Event> getAllUserEvents(String userId) {
        ConcurrentSkipListMap<LocalDate, CopyOnWriteArrayList<Event>> partitions =
                userPartitions.get(userId);
        if (partitions == null) return List.of();

        List<Event> all = new ArrayList<>();
        for (CopyOnWriteArrayList<Event> dayEvents : partitions.values()) {
            all.addAll(dayEvents);
        }
        return all;
    }

    // ==================== METADATA ====================

    @Override
    public String strategyName() { return "Partitioned (daily TreeMap)"; }

    @Override
    public int size() { return totalSize.get(); }

    @Override
    public long storageEstimate() {
        // ~208 bytes per event + ~32 bytes per day partition entry
        long partitionOverhead = userPartitions.values().stream()
                .mapToLong(ConcurrentSkipListMap::size).sum() * 32;
        return (long) totalSize.get() * 208 + partitionOverhead;
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — TIME PARTITIONING                         │
 * │                                                             │
 * │ WHY PARTITION BY TIME?                                       │
 * │   Most analytics queries have a time range ("last 7 days"). │
 * │   Without partitioning: scan ALL events, discard 90%+.      │
 * │   With daily partitions: subMap() skips irrelevant days     │
 * │   entirely — O(log D) to find boundaries, then only scan    │
 * │   events within matching days.                               │
 * │                                                             │
 * │ PARTITION GRANULARITY:                                        │
 * │   Daily: good balance of granularity vs overhead             │
 * │   Hourly: better for high-volume, intra-day queries          │
 * │   Monthly: less overhead, worse for narrow queries           │
 * │   Real systems: Cassandra uses configurable time buckets     │
 * │                                                             │
 * │ ConcurrentSkipListMap:                                        │
 * │   - Concurrent NavigableMap (thread-safe TreeMap equivalent) │
 * │   - O(log n) get/put/remove                                  │
 * │   - Supports subMap, headMap, tailMap — views, not copies!  │
 * │   - Skip list: probabilistic structure, O(log n) avg case   │
 * │   - Lock-free reads, fine-grained locking for writes         │
 * │                                                             │
 * │ AT SCALE (Cassandra partition design):                       │
 * │   Partition key: (userId, date_bucket)                       │
 * │   Clustering key: timestamp                                  │
 * │   → All events for user on one day are co-located on disk   │
 * │   → Range query within a day = sequential disk read          │
 * │   → Cross-day query = parallel partition reads                │
 * └─────────────────────────────────────────────────────────────┘
 */

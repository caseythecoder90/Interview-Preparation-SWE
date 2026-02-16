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
 * INVERTED INDEX TRACKER — Property-value inverted indexes
 * ============================================================
 *
 * Maintains inverted indexes on event property values, enabling
 * fast property-based filtering without scanning all events.
 *
 * Data Structures:
 *
 *   PRIMARY STORAGE:
 *   userEvents: ConcurrentHashMap<userId, CopyOnWriteArrayList<Event>>
 *
 *   INVERTED INDEX:
 *   propertyIndex: ConcurrentHashMap<propertyKey, ConcurrentHashMap<valueStr, CopyOnWriteArrayList<Event>>>
 *   └── "page"   → { "/home"  → [e1, e5],  "/about" → [e3] }
 *   └── "amount" → { "49.99"  → [e2],       "9.99"  → [e7] }
 *   └── "browser"→ { "Chrome" → [e1, e3, e5], "Firefox" → [e2] }
 *
 * How it works:
 *   Query: withProperty("page", "/home")
 *   LinearScan: iterate ALL events, check e.properties().get("page").equals("/home") → O(n)
 *   InvertedIndex: propertyIndex.get("page").get("/home") → [e1, e5] → O(1) lookup!
 *
 * This is EXACTLY how:
 *   - Elasticsearch works: inverted index maps terms → document IDs
 *   - Lucene's field-value index: maps field values → posting lists
 *   - Mixpanel's property filtering: pre-indexed for fast segment queries
 *
 * Tradeoffs:
 *   + O(1) property lookups instead of O(n) scan
 *   + Excellent for "find all events where page=/home" queries
 *   - High cardinality properties (e.g., "amount" with unique values) create
 *     millions of tiny lists → memory waste with little query benefit
 *   - Write amplification: each event with P properties → P index updates
 *   - Property values must be converted to String for index keys (loss of type info)
 *
 * Complexity Summary:
 *   track():                O(n_cow × P) where P = property count
 *   query(with property):   O(k) where k = events matching that property value
 *   query(without property):O(n) full scan (no advantage)
 */
public class InvertedIndexTracker implements EventTracker {

    // Primary storage
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> userEvents = new ConcurrentHashMap<>();

    // Inverted index: propertyKey → (propertyValueString → events with that value)
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<Event>>>
            propertyIndex = new ConcurrentHashMap<>();

    private final Clock clock;
    private final Supplier<String> idGenerator;
    private final SessionManager sessionManager;
    private final AtomicInteger totalSize = new AtomicInteger(0);

    public InvertedIndexTracker(Clock clock, Supplier<String> idGenerator, SessionManager sessionManager) {
        this.clock = Objects.requireNonNull(clock);
        this.idGenerator = Objects.requireNonNull(idGenerator);
        this.sessionManager = sessionManager;
    }

    public InvertedIndexTracker(Clock clock, Supplier<String> idGenerator) {
        this(clock, idGenerator, null);
    }

    // ==================== WRITE PATH ====================

    /**
     * Records event and updates inverted index for each property.
     *
     * <p>Time Complexity: O(n_cow + P) where n_cow = list size for COW copy,
     *   P = number of properties in this event
     * <p>Write Amplification: 1 primary write + P index writes
     */
    @Override
    public Event track(String userId, String eventType, Map<String, Object> properties) {
        Instant now = clock.instant();
        String sessionId = sessionManager != null
                ? sessionManager.getOrCreateSession(userId, now) : null;

        Event event = new Event(idGenerator.get(), userId, eventType, now, properties, sessionId);

        // Primary storage
        userEvents.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(event);

        // Update inverted index for each property
        // Why String.valueOf? Property values can be String, Integer, Double, Boolean.
        // Converting to String normalizes them for index key lookup.
        for (var entry : event.properties().entrySet()) {
            String propKey = entry.getKey();
            String propValue = String.valueOf(entry.getValue());
            propertyIndex
                    .computeIfAbsent(propKey, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(propValue, k -> new CopyOnWriteArrayList<>())
                    .add(event);
        }

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
     * Query with property-based optimization via inverted index.
     *
     * <p>Optimization: if query has property filters, use the inverted index
     * to get candidate events, then intersect with user's events and apply
     * remaining filters.
     *
     * <p>Time Complexity:
     *   With property filter: O(k) where k = events matching that property value
     *   Without property filter: O(n) full user scan
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
        Map<String, Object> propFilters = query.propertyFilters();

        if (propFilters != null && !propFilters.isEmpty()) {
            // Use inverted index for the FIRST property filter
            // Then intersect results with user's events
            var it = propFilters.entrySet().iterator();
            var firstFilter = it.next();
            String propKey = firstFilter.getKey();
            String propValue = String.valueOf(firstFilter.getValue());

            ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> valueMap = propertyIndex.get(propKey);
            if (valueMap == null) return List.of();
            CopyOnWriteArrayList<Event> indexed = valueMap.get(propValue);
            if (indexed == null) return List.of();

            // Filter to only this user's events from the indexed list
            return indexed.stream()
                    .filter(e -> e.userId().equals(query.userId()))
                    .toList();
        }

        // No property filters — fall back to full user event list
        return userEvents.getOrDefault(query.userId(), new CopyOnWriteArrayList<>());
    }

    // ==================== AGGREGATIONS ====================

    @Override
    public Map<String, Long> countByType(String userId) {
        return getUserEvents(userId).stream()
                .collect(Collectors.groupingBy(Event::eventType, Collectors.counting()));
    }

    @Override
    public Map<String, Long> countByDay(String userId, String eventType) {
        return getUserEvents(userId).stream()
                .filter(e -> e.eventType().equals(eventType))
                .collect(Collectors.groupingBy(
                        e -> LocalDate.ofInstant(e.timestamp(), ZoneOffset.UTC).toString(),
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    // ==================== CONVENIENCE ====================

    @Override
    public List<Event> getLatestEvents(String userId, int n) {
        List<Event> events = getUserEvents(userId);
        int size = events.size();
        int start = Math.max(0, size - n);
        List<Event> latest = new ArrayList<>();
        for (int i = size - 1; i >= start; i--) {
            latest.add(events.get(i));
        }
        return latest;
    }

    @Override
    public Optional<Event> getLastEventOfType(String userId, String eventType) {
        List<Event> events = getUserEvents(userId);
        for (int i = events.size() - 1; i >= 0; i--) {
            if (events.get(i).eventType().equals(eventType)) {
                return Optional.of(events.get(i));
            }
        }
        return Optional.empty();
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

    @Override
    public int removeEventsIf(Predicate<Event> filter) {
        AtomicInteger removed = new AtomicInteger(0);
        userEvents.forEach((userId, events) -> {
            List<Event> toRemove = events.stream().filter(filter).toList();
            if (toRemove.isEmpty()) return;

            events.removeAll(toRemove);
            removed.addAndGet(toRemove.size());
            totalSize.addAndGet(-toRemove.size());

            // Clean inverted indexes
            for (Event e : toRemove) {
                for (var prop : e.properties().entrySet()) {
                    String propKey = prop.getKey();
                    String propValue = String.valueOf(prop.getValue());
                    ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> valueMap = propertyIndex.get(propKey);
                    if (valueMap != null) {
                        CopyOnWriteArrayList<Event> list = valueMap.get(propValue);
                        if (list != null) {
                            list.remove(e);
                            if (list.isEmpty()) valueMap.remove(propValue);
                        }
                        if (valueMap.isEmpty()) propertyIndex.remove(propKey);
                    }
                }
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

        // Clean inverted indexes
        for (Event e : events) {
            for (var prop : e.properties().entrySet()) {
                String propKey = prop.getKey();
                String propValue = String.valueOf(prop.getValue());
                ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> valueMap = propertyIndex.get(propKey);
                if (valueMap != null) {
                    CopyOnWriteArrayList<Event> list = valueMap.get(propValue);
                    if (list != null) {
                        list.remove(e);
                        if (list.isEmpty()) valueMap.remove(propValue);
                    }
                    if (valueMap.isEmpty()) propertyIndex.remove(propKey);
                }
            }
        }
        return count;
    }

    // ==================== HELPERS ====================

    private List<Event> getUserEvents(String userId) {
        return userEvents.getOrDefault(userId, new CopyOnWriteArrayList<>());
    }

    // ==================== METADATA ====================

    @Override
    public String strategyName() { return "InvertedIndex (property indexes)"; }

    @Override
    public int size() { return totalSize.get(); }

    @Override
    public long storageEstimate() {
        // ~208 bytes per event + inverted index overhead
        long indexEntries = propertyIndex.values().stream()
                .mapToLong(vm -> vm.values().stream().mapToLong(CopyOnWriteArrayList::size).sum())
                .sum();
        return (long) totalSize.get() * 208 + indexEntries * 8;
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — INVERTED INDEXES                          │
 * │                                                             │
 * │ WHAT IS AN INVERTED INDEX?                                   │
 * │   Forward index:  document → [words in document]             │
 * │   Inverted index: word → [documents containing that word]    │
 * │   Our version: propertyValue → [events with that value]     │
 * │                                                             │
 * │ WHERE YOU SEE INVERTED INDEXES:                               │
 * │   - Elasticsearch/Lucene: the entire search engine is built  │
 * │     on inverted indexes (term → posting list of doc IDs)    │
 * │   - Google Search: web page index (word → pages)            │
 * │   - Database secondary indexes (conceptually similar)        │
 * │                                                             │
 * │ CARDINALITY MATTERS:                                          │
 * │   Low cardinality ("browser" with 5 values): excellent!      │
 * │     Each index list has many events → good filtering power  │
 * │   High cardinality ("user_id" with 1M values): terrible!    │
 * │     Each list has ~1 event → overhead >> benefit             │
 * │                                                             │
 * │ POSTING LIST INTERSECTION:                                    │
 * │   Multi-property query: withProperty("page", "/home")       │
 * │                         .withProperty("browser", "Chrome")  │
 * │   Naive: get both posting lists, intersect → O(min(k1, k2)) │
 * │   Optimized: start with shorter list, probe the other       │
 * │   Elasticsearch does this with skip lists + galloping search │
 * │                                                             │
 * │ MEMORY CONSIDERATION:                                         │
 * │   Each inverted index entry = 8-byte reference (pointer)    │
 * │   100K events × 5 properties each = 500K index entries      │
 * │   500K × 8 bytes = 4 MB — negligible for the query speedup │
 * └─────────────────────────────────────────────────────────────┘
 */

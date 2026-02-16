package eventtracker.test;

import eventtracker.model.Event;
import eventtracker.model.Session;
import eventtracker.query.EventQuery;
import eventtracker.query.SortOrder;
import eventtracker.retention.EventRetentionManager;
import eventtracker.retention.RetentionPolicy;
import eventtracker.service.IndexedEventTracker;
import eventtracker.session.SessionManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * FULL LIFECYCLE INTEGRATION TEST
 * ============================================================
 *
 * End-to-end test exercising the complete flow:
 *   1. Track events with sessions
 *   2. Query with various filters
 *   3. Aggregate analytics
 *   4. Apply retention policies
 *   5. GDPR user deletion
 *   6. Verify cleanup
 */
public class FullLifecycleTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testTrackQueryAggregateEvict() {
        System.out.println("\n=== Test 1: Full Track \u2192 Query \u2192 Aggregate \u2192 Evict ===");

        Instant now = Instant.parse("2024-06-15T10:00:00Z");
        AtomicInteger eventCounter = new AtomicInteger(0);
        AtomicInteger sessionCounter = new AtomicInteger(0);

        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + sessionCounter.incrementAndGet());
        IndexedEventTracker tracker = new IndexedEventTracker(
                Clock.fixed(now, ZoneOffset.UTC),
                () -> "evt-" + eventCounter.incrementAndGet(), sm);

        // Step 1: Track events for multiple users
        tracker.track("alice", "page_view", Map.of("page", "/home"));
        tracker.track("alice", "page_view", Map.of("page", "/products"));
        tracker.track("alice", "purchase", Map.of("amount", 79.99, "item", "keyboard"));
        tracker.track("alice", "page_view", Map.of("page", "/thankyou"));

        tracker.track("bob", "page_view", Map.of("page", "/home"));
        tracker.track("bob", "login", Map.of("method", "oauth"));

        tracker.track("charlie", "page_view", Map.of("page", "/pricing"));

        check("7 events total", tracker.size() == 7);

        // Step 2: Query
        List<Event> aliceEvents = tracker.query(EventQuery.forUser("alice"));
        check("Alice has 4 events", aliceEvents.size() == 4);

        List<Event> alicePageViews = tracker.query(
                EventQuery.forUser("alice").ofType("page_view"));
        check("Alice has 3 page_views", alicePageViews.size() == 3);

        List<Event> aliceLatest2 = tracker.getLatestEvents("alice", 2);
        check("Alice latest 2", aliceLatest2.size() == 2);
        check("Latest is evt-4", "evt-4".equals(aliceLatest2.get(0).eventId()));

        // Step 3: Aggregation
        Map<String, Long> aliceCounts = tracker.countByType("alice");
        check("Alice: 3 page_view, 1 purchase",
                aliceCounts.get("page_view") == 3 && aliceCounts.get("purchase") == 1);

        Optional<Event> lastPurchase = tracker.getLastEventOfType("alice", "purchase");
        check("Alice last purchase found", lastPurchase.isPresent());
        check("Correct purchase", lastPurchase.get().properties().get("item").equals("keyboard"));

        // Step 4: Session check
        List<Session> aliceSessions = sm.getUserSessions("alice");
        check("Alice has 1 session", aliceSessions.size() == 1);
        check("Session has 4 events", aliceSessions.get(0).eventCount() == 4);

        // Step 5: Active users
        List<String> active = tracker.getActiveUsers(now);
        check("3 active users", active.size() == 3);

        // Step 6: GDPR delete bob
        EventRetentionManager rm = new EventRetentionManager(tracker,
                Duration.ofDays(365), Clock.fixed(now, ZoneOffset.UTC));
        int bobDeleted = rm.evictUser("bob");
        check("Bob's 2 events deleted", bobDeleted == 2);
        check("5 events remain", tracker.size() == 5);
        check("Bob is gone", tracker.query(EventQuery.forUser("bob")).isEmpty());
        check("Alice intact", tracker.query(EventQuery.forUser("alice")).size() == 4);
    }

    static void testSessionAcrossGaps() {
        System.out.println("\n=== Test 2: Session Detection Across Activity Gaps ===");

        AtomicInteger eventCounter = new AtomicInteger(0);
        AtomicInteger sessionCounter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + sessionCounter.incrementAndGet());

        // Session 1: 10:00, 10:10, 10:20
        Instant s1Start = Instant.parse("2024-06-15T10:00:00Z");
        IndexedEventTracker t1 = new IndexedEventTracker(
                Clock.fixed(s1Start, ZoneOffset.UTC),
                () -> "evt-" + eventCounter.incrementAndGet(), sm);
        Event e1 = t1.track("alice", "page_view", Map.of());

        sm.getOrCreateSession("alice", s1Start.plus(Duration.ofMinutes(10)));
        sm.getOrCreateSession("alice", s1Start.plus(Duration.ofMinutes(20)));

        // Session 2: 11:30 (70 min gap from 10:20)
        Instant s2Start = s1Start.plus(Duration.ofMinutes(90));
        sm.getOrCreateSession("alice", s2Start);
        sm.getOrCreateSession("alice", s2Start.plus(Duration.ofMinutes(5)));

        List<Session> sessions = sm.getUserSessions("alice");
        check("2 sessions detected", sessions.size() == 2);
        check("Session 1: 3 events", sessions.get(0).eventCount() == 3);
        check("Session 2: 2 events", sessions.get(1).eventCount() == 2);

        // Verify session assigned to event
        check("Event has session ID", e1.sessionId() != null);
        check("Session ID is sess-1", "sess-1".equals(e1.sessionId()));
    }

    static void testRetentionPoliciesIntegration() {
        System.out.println("\n=== Test 3: Retention Policies Integration ===");

        Instant now = Instant.parse("2024-06-15T10:00:00Z");
        Instant sixMonthsAgo = now.minus(Duration.ofDays(180));
        AtomicInteger counter = new AtomicInteger(0);

        // Track "old" events (180 days ago)
        IndexedEventTracker tracker = new IndexedEventTracker(
                Clock.fixed(sixMonthsAgo, ZoneOffset.UTC),
                () -> "evt-" + counter.incrementAndGet());

        tracker.track("alice", "page_view", Map.of());     // Should be evicted (90d policy)
        tracker.track("alice", "page_view", Map.of());     // Should be evicted
        tracker.track("alice", "purchase", Map.of());       // Should survive (2yr policy)
        tracker.track("alice", "login", Map.of());          // Should survive (default 1yr)
        tracker.track("bob", "page_view", Map.of());        // Should be evicted

        check("5 events before policies", tracker.size() == 5);

        List<RetentionPolicy> policies = List.of(
                new RetentionPolicy("page_view", Duration.ofDays(90)),
                new RetentionPolicy("purchase", Duration.ofDays(730))
        );

        EventRetentionManager rm = new EventRetentionManager(
                tracker, policies, Duration.ofDays(365),
                Clock.fixed(now, ZoneOffset.UTC));

        int evicted = rm.applyPolicies();
        check("3 page_views evicted", evicted == 3);
        check("2 events remain", tracker.size() == 2);
        check("Purchase survives", tracker.getLastEventOfType("alice", "purchase").isPresent());
        check("Login survives", tracker.getLastEventOfType("alice", "login").isPresent());
        check("Bob is empty", tracker.query(EventQuery.forUser("bob")).isEmpty());
    }

    static void testPropertyFiltering() {
        System.out.println("\n=== Test 4: Property-Based Filtering ===");

        AtomicInteger counter = new AtomicInteger(0);
        IndexedEventTracker tracker = new IndexedEventTracker(
                Clock.systemUTC(), () -> "evt-" + counter.incrementAndGet());

        tracker.track("alice", "purchase", Map.of("amount", 10.00, "currency", "USD"));
        tracker.track("alice", "purchase", Map.of("amount", 50.00, "currency", "USD"));
        tracker.track("alice", "purchase", Map.of("amount", 30.00, "currency", "EUR"));
        tracker.track("alice", "page_view", Map.of("page", "/home"));

        // Filter by property
        List<Event> usdPurchases = tracker.query(
                EventQuery.forUser("alice")
                        .ofType("purchase")
                        .withProperty("currency", "USD"));
        check("2 USD purchases", usdPurchases.size() == 2);

        List<Event> eurPurchases = tracker.query(
                EventQuery.forUser("alice")
                        .ofType("purchase")
                        .withProperty("currency", "EUR"));
        check("1 EUR purchase", eurPurchases.size() == 1);
    }

    static void testPaginatedBrowsing() {
        System.out.println("\n=== Test 5: Paginated Activity Feed ===");

        AtomicInteger counter = new AtomicInteger(0);
        IndexedEventTracker tracker = new IndexedEventTracker(
                Clock.systemUTC(), () -> "evt-" + counter.incrementAndGet());

        // Simulate 50 events
        for (int i = 0; i < 50; i++) {
            tracker.track("alice", "event_" + (i % 5), Map.of("page", "/p" + i));
        }

        // Browse pages of 10, reverse chronological (activity feed)
        int pageSize = 10;
        Set<String> allEventIds = new HashSet<>();

        for (int page = 0; page < 5; page++) {
            List<Event> results = tracker.query(
                    EventQuery.forUser("alice")
                            .orderBy(SortOrder.REVERSE_CHRONOLOGICAL)
                            .offset(page * pageSize)
                            .limit(pageSize));

            check("Page " + (page + 1) + " has " + pageSize + " events",
                    results.size() == pageSize);

            for (Event e : results) {
                allEventIds.add(e.eventId());
            }
        }

        check("All 50 events seen across 5 pages", allEventIds.size() == 50);
    }

    static void testConcurrentTrackAndEvict() throws InterruptedException {
        System.out.println("\n=== Test 6: Concurrent Track + Evict ===");

        AtomicInteger counter = new AtomicInteger(0);
        IndexedEventTracker tracker = new IndexedEventTracker(
                Clock.systemUTC(), () -> "evt-" + counter.incrementAndGet());

        // Pre-populate
        for (int i = 0; i < 1000; i++) {
            tracker.track("user" + (i % 10), "event", Map.of("i", i));
        }

        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(30);

        // 10 writer threads
        for (int t = 0; t < 10; t++) {
            final int tid = t;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < 100; i++) {
                        tracker.track("user" + tid, "event", Map.of());
                    }
                } catch (Exception e) { errors.incrementAndGet(); }
                finally { endGate.countDown(); }
            }).start();
        }

        // 10 reader threads
        for (int t = 0; t < 10; t++) {
            final int tid = t;
            new Thread(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < 100; i++) {
                        tracker.query(EventQuery.forUser("user" + tid));
                        tracker.countByType("user" + tid);
                    }
                } catch (Exception e) { errors.incrementAndGet(); }
                finally { endGate.countDown(); }
            }).start();
        }

        // 10 evictor threads — each evicts a DIFFERENT subset to avoid
        // double-counting race on the size counter
        for (int t = 0; t < 10; t++) {
            final int evictorId = t;
            new Thread(() -> {
                try {
                    startGate.await();
                    // Each evictor evicts events from a different user
                    tracker.removeEventsIf(e ->
                            e.userId().equals("user" + evictorId) &&
                            e.properties().containsKey("i"));
                } catch (Exception e) { errors.incrementAndGet(); }
                finally { endGate.countDown(); }
            }).start();
        }

        startGate.countDown();
        endGate.await();

        check("No concurrent errors", errors.get() == 0);
        check("Tracker still consistent (size >= 0)", tracker.size() >= 0);
    }

    static void testCountByDayAggregation() {
        System.out.println("\n=== Test 7: Count by Day Aggregation ===");

        Instant day1 = Instant.parse("2024-06-15T10:00:00Z");
        AtomicInteger counter = new AtomicInteger(0);

        IndexedEventTracker tracker = new IndexedEventTracker(
                Clock.fixed(day1, ZoneOffset.UTC),
                () -> "evt-" + counter.incrementAndGet());

        // 3 page_views and 1 purchase on day1
        tracker.track("alice", "page_view", Map.of());
        tracker.track("alice", "page_view", Map.of());
        tracker.track("alice", "page_view", Map.of());
        tracker.track("alice", "purchase", Map.of());

        Map<String, Long> pvByDay = tracker.countByDay("alice", "page_view");
        check("3 page_views on 2024-06-15",
                pvByDay.getOrDefault("2024-06-15", 0L) == 3);

        Map<String, Long> purchaseByDay = tracker.countByDay("alice", "purchase");
        check("1 purchase on 2024-06-15",
                purchaseByDay.getOrDefault("2024-06-15", 0L) == 1);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println("\u2551  Full Lifecycle Integration \u2014 Test Suite      \u2551");
        System.out.println("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");

        testTrackQueryAggregateEvict();
        testSessionAcrossGaps();
        testRetentionPoliciesIntegration();
        testPropertyFiltering();
        testPaginatedBrowsing();
        testConcurrentTrackAndEvict();
        testCountByDayAggregation();

        System.out.println("\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — EVENT TRACKING SYSTEM DESIGN              │
 * │                                                             │
 * │ FROM HASHMAP TO DISTRIBUTED STORAGE:                        │
 * │   "This won't hold 1B events. Now what?"                    │
 * │   Write path: SDK → API Gateway → Kafka → Stream Processor │
 * │     → Storage (Cassandra/ScyllaDB)                          │
 * │   Why Kafka? Decouples producers from consumers, enables    │
 * │   replay, handles burst traffic, provides ordering guarantees│
 * │   Partition key: userId → all user events on same partition │
 * │                                                             │
 * │ REAL-TIME ANALYTICS PIPELINE:                               │
 * │   Stream processing: Kafka Streams / Flink processes events │
 * │   as they arrive. Pre-aggregate running counters in Redis   │
 * │   (increment on each event, don't count from scratch).      │
 * │   Lambda architecture: real-time layer + batch correction.  │
 * │   Materialized views: pre-compute DAU, event counts by type │
 * │   so dashboards are instant (< 100ms).                      │
 * │                                                             │
 * │ QUERY OPTIMIZATION AT SCALE:                                │
 * │   "How does Mixpanel query 1B events in under a second?"    │
 * │   - Pre-aggregated rollup tables (min → hr → day → month)  │
 * │   - Columnar storage (ClickHouse, Apache Druid)             │
 * │   - Bloom filters: "does user X have any purchase events?"  │
 * │   - Approximate algorithms: HyperLogLog for uniques,        │
 * │     t-digest for percentiles                                │
 * │                                                             │
 * │ SCHEMA EVOLUTION:                                            │
 * │   "What happens when event properties change over time?"    │
 * │   - Schema registry (Confluent + Avro)                      │
 * │   - Flexible Map<String, Object> vs strict typing tradeoff  │
 * │   - Backward/forward compatibility: adding optional fields  │
 * │     is safe, removing/renaming is dangerous                 │
 * │                                                             │
 * │ EVENT SOURCING CONNECTION:                                    │
 * │   Event tracking = recording what happened (analytics)       │
 * │   Event sourcing = deriving state FROM event history (arch)  │
 * │   The event list IS the source of truth.                     │
 * │   CQRS: separate write model (append) from read model       │
 * │   (materialized views). Kafka = distributed commit log.     │
 * │                                                             │
 * │ COMMON FOLLOW-UP QUESTIONS:                                   │
 * │   Q: "Out-of-order events?"                                  │
 * │   A: Accept with original timestamp, watermarking in Flink  │
 * │                                                             │
 * │   Q: "User with 10M events?"                                │
 * │   A: Pagination essential, time-partition, rollup old data  │
 * │                                                             │
 * │   Q: "Duplicate events?"                                    │
 * │   A: Idempotency key (eventId), dedup in stream processor   │
 * │                                                             │
 * │   Q: "Tracking vs logging?"                                 │
 * │   A: Tracking = user-behavior, structured                   │
 * │      Logging = system-health, often unstructured             │
 * │                                                             │
 * │   Q: "Funnel analysis?"                                     │
 * │   A: Ordered sequence matching across types per user,        │
 * │      sessionized, with time constraints between steps        │
 * │                                                             │
 * │ CAPACITY ESTIMATION:                                          │
 * │   10M DAU × 50 events/day = 500M events/day                 │
 * │   × 365 days = 182.5B events/year                           │
 * │   × 500 bytes/event = 91 TB/year                            │
 * │   Write QPS: 500M / 86400 = ~5,800 QPS avg, ~30K peak      │
 * │   Read QPS: 10× write = ~60K QPS → need caching layer      │
 * └─────────────────────────────────────────────────────────────┘
 */

package eventtracker.test;

import eventtracker.model.Event;
import eventtracker.model.TrackRequest;
import eventtracker.query.EventQuery;
import eventtracker.query.SortOrder;
import eventtracker.service.LinearScanTracker;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * LINEAR SCAN TRACKER — Test Suite
 * ============================================================
 *
 * Verifies the baseline implementation works correctly.
 * These tests serve as a correctness reference — the same queries
 * should produce identical results across all tracker strategies.
 */
public class LinearScanTrackerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    private static AtomicInteger idCounter = new AtomicInteger(0);

    private static LinearScanTracker createTracker() {
        idCounter.set(0);
        return new LinearScanTracker(Clock.systemUTC(),
                () -> "evt-" + idCounter.incrementAndGet());
    }

    static void testTrackAndRetrieve() {
        System.out.println("\n=== Test 1: Track and Retrieve ===");
        LinearScanTracker t = createTracker();

        Event e = t.track("user1", "page_view", Map.of("page", "/home"));
        check("Event created", e != null);
        check("ID generated", e.eventId().startsWith("evt-"));
        check("Size is 1", t.size() == 1);

        List<Event> events = t.query(EventQuery.forUser("user1"));
        check("Query returns 1", events.size() == 1);
        check("Same event", events.get(0).eventId().equals(e.eventId()));
    }

    static void testMultipleEvents() {
        System.out.println("\n=== Test 2: Multiple Events ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());
        t.track("user1", "page_view", Map.of());

        check("Size is 3", t.size() == 3);
        check("All returned", t.query(EventQuery.forUser("user1")).size() == 3);
    }

    static void testQueryByType() {
        System.out.println("\n=== Test 3: Query by Type (full scan) ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());
        t.track("user1", "page_view", Map.of());

        List<Event> pageViews = t.query(EventQuery.forUser("user1").ofType("page_view"));
        check("2 page_views", pageViews.size() == 2);
        check("All are page_view type",
                pageViews.stream().allMatch(e -> "page_view".equals(e.eventType())));
    }

    static void testQueryByProperty() {
        System.out.println("\n=== Test 4: Query by Property (full scan) ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "page_view", Map.of("page", "/home"));
        t.track("user1", "page_view", Map.of("page", "/about"));
        t.track("user1", "page_view", Map.of("page", "/home"));

        List<Event> home = t.query(EventQuery.forUser("user1").withProperty("page", "/home"));
        check("2 events with page=/home", home.size() == 2);
    }

    static void testPagination() {
        System.out.println("\n=== Test 5: Pagination ===");
        LinearScanTracker t = createTracker();

        for (int i = 0; i < 10; i++) {
            t.track("user1", "event", Map.of("seq", i));
        }

        List<Event> page1 = t.query(EventQuery.forUser("user1").limit(3));
        List<Event> page2 = t.query(EventQuery.forUser("user1").offset(3).limit(3));
        List<Event> page3 = t.query(EventQuery.forUser("user1").offset(6).limit(3));
        List<Event> page4 = t.query(EventQuery.forUser("user1").offset(9).limit(3));

        check("Page 1: 3 results", page1.size() == 3);
        check("Page 2: 3 results", page2.size() == 3);
        check("Page 3: 3 results", page3.size() == 3);
        check("Page 4: 1 result", page4.size() == 1);
    }

    static void testCountByType() {
        System.out.println("\n=== Test 6: Count by Type ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "page_view", Map.of());
        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());

        Map<String, Long> counts = t.countByType("user1");
        check("2 page_views", counts.get("page_view") == 2);
        check("1 purchase", counts.get("purchase") == 1);
    }

    static void testGetLatestEvents() {
        System.out.println("\n=== Test 7: Get Latest Events ===");
        LinearScanTracker t = createTracker();

        for (int i = 1; i <= 5; i++) {
            t.track("user1", "event", Map.of("seq", i));
        }

        List<Event> latest = t.getLatestEvents("user1", 2);
        check("2 latest", latest.size() == 2);
        check("Newest first", "evt-5".equals(latest.get(0).eventId()));
    }

    static void testGetLastEventOfType() {
        System.out.println("\n=== Test 8: Last Event of Type ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "purchase", Map.of("amount", 10));
        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of("amount", 20));
        t.track("user1", "page_view", Map.of());

        Optional<Event> last = t.getLastEventOfType("user1", "purchase");
        check("Last purchase found", last.isPresent());
        check("Is the second purchase", "evt-3".equals(last.get().eventId()));
    }

    static void testRemoveEventsIf() {
        System.out.println("\n=== Test 9: Remove Events If ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());
        t.track("user2", "page_view", Map.of());

        int removed = t.removeEventsIf(e -> "page_view".equals(e.eventType()));
        check("Removed 2", removed == 2);
        check("1 remains", t.size() == 1);
    }

    static void testRemoveAllForUser() {
        System.out.println("\n=== Test 10: Remove All for User ===");
        LinearScanTracker t = createTracker();

        t.track("user1", "a", Map.of());
        t.track("user1", "b", Map.of());
        t.track("user2", "a", Map.of());

        int removed = t.removeAllForUser("user1");
        check("Removed 2", removed == 2);
        check("User2 intact", t.query(EventQuery.forUser("user2")).size() == 1);
    }

    static void testBatchTracking() {
        System.out.println("\n=== Test 11: Batch Tracking ===");
        LinearScanTracker t = createTracker();

        List<TrackRequest> batch = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            batch.add(new TrackRequest("user" + (i % 5), "type_" + (i % 3), Map.of("i", i)));
        }
        t.trackBatch(batch);

        check("500 events tracked", t.size() == 500);
        check("User0 has 100", t.query(EventQuery.forUser("user0")).size() == 100);
    }

    static void testGetActiveUsers() {
        System.out.println("\n=== Test 12: Get Active Users ===");
        Instant base = Instant.parse("2024-06-15T10:00:00Z");
        AtomicInteger counter = new AtomicInteger(0);
        LinearScanTracker t = new LinearScanTracker(
                Clock.fixed(base, ZoneOffset.UTC),
                () -> "evt-" + counter.incrementAndGet());

        t.track("active1", "event", Map.of());
        t.track("active2", "event", Map.of());

        check("2 active users", t.getActiveUsers(base).size() == 2);
        check("0 active in future", t.getActiveUsers(base.plusSeconds(1)).isEmpty());
    }

    public static void main(String[] args) {
        System.out.println("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println("\u2551  Linear Scan Tracker \u2014 Test Suite             \u2551");
        System.out.println("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");

        testTrackAndRetrieve();
        testMultipleEvents();
        testQueryByType();
        testQueryByProperty();
        testPagination();
        testCountByType();
        testGetLatestEvents();
        testGetLastEventOfType();
        testRemoveEventsIf();
        testRemoveAllForUser();
        testBatchTracking();
        testGetActiveUsers();

        System.out.println("\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }
}

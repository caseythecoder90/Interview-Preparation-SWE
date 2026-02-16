package eventtracker.test;

import eventtracker.model.Event;
import eventtracker.query.EventQuery;
import eventtracker.query.SortOrder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * EVENT QUERY — Test Suite
 * ============================================================
 *
 * Tests the builder pattern and query execution logic:
 *   - Builder chaining and defaults
 *   - Matching logic for each filter type
 *   - Combined filters (AND semantics)
 *   - Pagination (limit/offset)
 *   - Sort order (chronological/reverse)
 *   - Edge cases: empty results, boundary timestamps
 */
public class EventQueryTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    // ========== HELPER: Create test events ==========

    private static final Instant T1 = Instant.parse("2024-06-15T10:00:00Z");
    private static final Instant T2 = Instant.parse("2024-06-15T11:00:00Z");
    private static final Instant T3 = Instant.parse("2024-06-15T12:00:00Z");
    private static final Instant T4 = Instant.parse("2024-06-16T10:00:00Z");
    private static final Instant T5 = Instant.parse("2024-06-16T11:00:00Z");

    private static List<Event> createTestEvents() {
        return List.of(
                new Event("e1", "user1", "page_view", T1, Map.of("page", "/home"), "s1"),
                new Event("e2", "user1", "purchase", T2, Map.of("amount", 49.99, "page", "/cart"), "s1"),
                new Event("e3", "user1", "page_view", T3, Map.of("page", "/about"), "s1"),
                new Event("e4", "user1", "login", T4, Map.of(), "s2"),
                new Event("e5", "user1", "purchase", T5, Map.of("amount", 9.99, "page", "/cart"), "s2")
        );
    }

    // ========== TEST METHODS ==========

    static void testBuilderDefaults() {
        System.out.println("\n=== Test 1: Builder Defaults ===");
        EventQuery q = EventQuery.forUser("user1");
        check("userId is set", "user1".equals(q.userId()));
        check("eventTypes is null (no filter)", q.eventTypes() == null);
        check("startTime is null", q.startTime() == null);
        check("endTime is null", q.endTime() == null);
        check("propertyFilters is empty", q.propertyFilters().isEmpty());
        check("sessionId is null", q.sessionId() == null);
        check("limit is MAX_VALUE", q.limit() == Integer.MAX_VALUE);
        check("offset is 0", q.offset() == 0);
        check("sortOrder is CHRONOLOGICAL", q.sortOrder() == SortOrder.CHRONOLOGICAL);
    }

    static void testFilterByType() {
        System.out.println("\n=== Test 2: Filter by Event Type ===");
        List<Event> events = createTestEvents();

        // Single type
        EventQuery q = EventQuery.forUser("user1").ofType("purchase");
        List<Event> result = q.execute(events);
        check("Purchase filter: 2 results", result.size() == 2);
        check("Both are purchases", result.stream().allMatch(e -> "purchase".equals(e.eventType())));

        // Multiple types
        EventQuery q2 = EventQuery.forUser("user1").ofType("purchase", "login");
        List<Event> result2 = q2.execute(events);
        check("Purchase+login filter: 3 results", result2.size() == 3);
    }

    static void testFilterByTimeRange() {
        System.out.println("\n=== Test 3: Filter by Time Range ===");
        List<Event> events = createTestEvents();

        // Narrow range: only June 15 events
        EventQuery q = EventQuery.forUser("user1")
                .between(T1, T3);
        List<Event> result = q.execute(events);
        check("June 15 range: 3 results", result.size() == 3);

        // Boundary test: exact start/end match
        EventQuery q2 = EventQuery.forUser("user1")
                .between(T2, T2);
        List<Event> result2 = q2.execute(events);
        check("Exact boundary: 1 result", result2.size() == 1);
        check("Boundary event is e2", "e2".equals(result2.get(0).eventId()));
    }

    static void testFilterByProperty() {
        System.out.println("\n=== Test 4: Filter by Property ===");
        List<Event> events = createTestEvents();

        // String property
        EventQuery q = EventQuery.forUser("user1").withProperty("page", "/home");
        List<Event> result = q.execute(events);
        check("page=/home: 1 result", result.size() == 1);
        check("Correct event", "e1".equals(result.get(0).eventId()));

        // Numeric property
        EventQuery q2 = EventQuery.forUser("user1").withProperty("amount", 49.99);
        List<Event> result2 = q2.execute(events);
        check("amount=49.99: 1 result", result2.size() == 1);
        check("Correct purchase", "e2".equals(result2.get(0).eventId()));

        // Non-matching property
        EventQuery q3 = EventQuery.forUser("user1").withProperty("page", "/nonexistent");
        check("Non-matching: 0 results", q3.execute(events).isEmpty());
    }

    static void testFilterBySession() {
        System.out.println("\n=== Test 5: Filter by Session ===");
        List<Event> events = createTestEvents();

        EventQuery q = EventQuery.forUser("user1").inSession("s1");
        List<Event> result = q.execute(events);
        check("Session s1: 3 events", result.size() == 3);

        EventQuery q2 = EventQuery.forUser("user1").inSession("s2");
        List<Event> result2 = q2.execute(events);
        check("Session s2: 2 events", result2.size() == 2);
    }

    static void testCombinedFilters() {
        System.out.println("\n=== Test 6: Combined Filters (AND semantics) ===");
        List<Event> events = createTestEvents();

        // Type + time range
        EventQuery q = EventQuery.forUser("user1")
                .ofType("page_view")
                .between(T1, T3);
        List<Event> result = q.execute(events);
        check("page_view in June 15: 2 results", result.size() == 2);

        // Type + property
        EventQuery q2 = EventQuery.forUser("user1")
                .ofType("purchase")
                .withProperty("amount", 9.99);
        List<Event> result2 = q2.execute(events);
        check("purchase with amount=9.99: 1 result", result2.size() == 1);
        check("Correct event e5", "e5".equals(result2.get(0).eventId()));

        // Type + time + session
        EventQuery q3 = EventQuery.forUser("user1")
                .ofType("purchase")
                .between(T1, T3)
                .inSession("s1");
        List<Event> result3 = q3.execute(events);
        check("purchase in s1 on June 15: 1 result", result3.size() == 1);
        check("Correct event e2", "e2".equals(result3.get(0).eventId()));
    }

    static void testPagination() {
        System.out.println("\n=== Test 7: Pagination ===");
        List<Event> events = createTestEvents();

        // Limit
        EventQuery q1 = EventQuery.forUser("user1").limit(2);
        check("Limit 2: 2 results", q1.execute(events).size() == 2);

        // Offset
        EventQuery q2 = EventQuery.forUser("user1").offset(3);
        List<Event> result2 = q2.execute(events);
        check("Offset 3: 2 results", result2.size() == 2);
        check("First is e4", "e4".equals(result2.get(0).eventId()));

        // Limit + Offset (page 2 of size 2)
        EventQuery q3 = EventQuery.forUser("user1").offset(2).limit(2);
        List<Event> result3 = q3.execute(events);
        check("Page 2 (offset=2, limit=2): 2 results", result3.size() == 2);
        check("Starts at e3", "e3".equals(result3.get(0).eventId()));
        check("Ends at e4", "e4".equals(result3.get(1).eventId()));

        // Offset beyond results
        EventQuery q4 = EventQuery.forUser("user1").offset(100);
        check("Offset beyond data: 0 results", q4.execute(events).isEmpty());
    }

    static void testSortOrder() {
        System.out.println("\n=== Test 8: Sort Order ===");
        List<Event> events = createTestEvents();

        // Chronological (default)
        EventQuery q1 = EventQuery.forUser("user1").orderBy(SortOrder.CHRONOLOGICAL);
        List<Event> chrono = q1.execute(events);
        check("Chronological: first is e1", "e1".equals(chrono.get(0).eventId()));
        check("Chronological: last is e5", "e5".equals(chrono.get(4).eventId()));

        // Reverse chronological
        EventQuery q2 = EventQuery.forUser("user1").orderBy(SortOrder.REVERSE_CHRONOLOGICAL);
        List<Event> reverse = q2.execute(events);
        check("Reverse: first is e5", "e5".equals(reverse.get(0).eventId()));
        check("Reverse: last is e1", "e1".equals(reverse.get(4).eventId()));

        // Reverse + limit (most recent 2)
        EventQuery q3 = EventQuery.forUser("user1")
                .orderBy(SortOrder.REVERSE_CHRONOLOGICAL)
                .limit(2);
        List<Event> recent = q3.execute(events);
        check("Latest 2: e5 and e4", "e5".equals(recent.get(0).eventId()) &&
                "e4".equals(recent.get(1).eventId()));
    }

    static void testEmptyResults() {
        System.out.println("\n=== Test 9: Empty Results ===");
        List<Event> events = createTestEvents();

        check("Non-matching type: empty", EventQuery.forUser("user1")
                .ofType("nonexistent").execute(events).isEmpty());
        check("Empty event list: empty", EventQuery.forUser("user1")
                .execute(List.of()).isEmpty());
    }

    static void testCount() {
        System.out.println("\n=== Test 10: Count (no pagination) ===");
        List<Event> events = createTestEvents();

        EventQuery q = EventQuery.forUser("user1").ofType("purchase").limit(1);
        check("Execute with limit=1: 1 result", q.execute(events).size() == 1);
        check("Count ignores limit: 2", q.executeCount(events) == 2);
    }

    public static void main(String[] args) {
        System.out.println("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println("\u2551  EventQuery Builder \u2014 Test Suite            \u2551");
        System.out.println("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");

        testBuilderDefaults();
        testFilterByType();
        testFilterByTimeRange();
        testFilterByProperty();
        testFilterBySession();
        testCombinedFilters();
        testPagination();
        testSortOrder();
        testEmptyResults();
        testCount();

        System.out.println("\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }
}

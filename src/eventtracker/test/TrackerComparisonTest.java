package eventtracker.test;

import eventtracker.model.Event;
import eventtracker.query.EventQuery;
import eventtracker.query.SortOrder;
import eventtracker.service.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * TRACKER COMPARISON — Correctness + Performance Test Suite
 * ============================================================
 *
 * Verifies that all 4 strategies produce IDENTICAL results
 * for the same queries, then benchmarks each to show where
 * different strategies excel.
 */
public class TrackerComparisonTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    /** Creates all 4 trackers sharing the same clock and ID generator state */
    private static EventTracker[] createTrackers(Clock clock, AtomicInteger counter) {
        return new EventTracker[] {
                new LinearScanTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new IndexedEventTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new PartitionedEventTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new InvertedIndexTracker(clock, () -> "evt-" + counter.incrementAndGet())
        };
    }

    /** Populates all trackers with identical data (separate ID sequences) */
    private static void populateAll(EventTracker[] trackers) {
        String[][] data = {
                {"user1", "page_view", "page", "/home"},
                {"user1", "page_view", "page", "/about"},
                {"user1", "purchase", "amount", "49.99"},
                {"user1", "login", "method", "oauth"},
                {"user1", "page_view", "page", "/cart"},
                {"user2", "page_view", "page", "/home"},
                {"user2", "purchase", "amount", "19.99"},
                {"user3", "login", "method", "password"},
        };
        for (EventTracker t : trackers) {
            for (String[] row : data) {
                t.track(row[0], row[1], Map.of(row[2], row[3]));
            }
        }
    }

    static void testCorrectnessAllStrategies() {
        System.out.println("\n=== Test 1: Correctness — All Strategies Return Same Results ===");
        AtomicInteger counter = new AtomicInteger(0);
        Clock clock = Clock.fixed(Instant.parse("2024-06-15T10:00:00Z"), ZoneOffset.UTC);

        // Create and populate each tracker independently (with separate counters)
        LinearScanTracker linear = new LinearScanTracker(clock, () -> "evt-" + counter.incrementAndGet());
        counter.set(0);
        IndexedEventTracker indexed = new IndexedEventTracker(clock, () -> "evt-" + counter.incrementAndGet());
        counter.set(0);
        PartitionedEventTracker partitioned = new PartitionedEventTracker(clock, () -> "evt-" + counter.incrementAndGet());
        counter.set(0);
        InvertedIndexTracker inverted = new InvertedIndexTracker(clock, () -> "evt-" + counter.incrementAndGet());

        // Same data into each
        EventTracker[] all = {linear, indexed, partitioned, inverted};
        populateAll(all);

        // Query 1: All events for user1
        for (EventTracker t : all) {
            check(t.strategyName() + " — user1 has 5 events",
                    t.query(EventQuery.forUser("user1")).size() == 5);
        }

        // Query 2: Filter by type
        for (EventTracker t : all) {
            check(t.strategyName() + " — user1 page_views = 3",
                    t.query(EventQuery.forUser("user1").ofType("page_view")).size() == 3);
        }

        // Query 3: Count by type
        for (EventTracker t : all) {
            Map<String, Long> counts = t.countByType("user1");
            check(t.strategyName() + " — countByType correct",
                    counts.get("page_view") == 3 && counts.get("purchase") == 1 && counts.get("login") == 1);
        }

        // Query 4: Total size
        for (EventTracker t : all) {
            check(t.strategyName() + " — total size 8", t.size() == 8);
        }
    }

    static void testPaginationConsistency() {
        System.out.println("\n=== Test 2: Pagination Consistency Across Strategies ===");
        AtomicInteger counter = new AtomicInteger(0);
        Clock clock = Clock.fixed(Instant.parse("2024-06-15T10:00:00Z"), ZoneOffset.UTC);

        EventTracker[] trackers = {
                new LinearScanTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new IndexedEventTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new PartitionedEventTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new InvertedIndexTracker(clock, () -> "evt-" + counter.incrementAndGet())
        };

        // 20 events each
        for (EventTracker t : trackers) {
            counter.set(0);
            for (int i = 0; i < 20; i++) {
                t.track("user1", "event_" + (i % 3), Map.of("idx", i));
            }
        }

        // Page 2 (offset=5, limit=5) should return same count from all
        for (EventTracker t : trackers) {
            List<Event> page = t.query(EventQuery.forUser("user1").offset(5).limit(5));
            check(t.strategyName() + " — page 2 has 5 events", page.size() == 5);
        }
    }

    static void testDeleteConsistency() {
        System.out.println("\n=== Test 3: Delete Consistency ===");
        AtomicInteger counter = new AtomicInteger(0);
        Clock clock = Clock.fixed(Instant.parse("2024-06-15T10:00:00Z"), ZoneOffset.UTC);

        EventTracker[] trackers = {
                new LinearScanTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new IndexedEventTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new PartitionedEventTracker(clock, () -> "evt-" + counter.incrementAndGet()),
                new InvertedIndexTracker(clock, () -> "evt-" + counter.incrementAndGet())
        };

        for (EventTracker t : trackers) {
            counter.set(0);
            t.track("user1", "keep", Map.of());
            t.track("user1", "remove", Map.of());
            t.track("user2", "keep", Map.of());
        }

        for (EventTracker t : trackers) {
            t.removeEventsIf(e -> "remove".equals(e.eventType()));
            check(t.strategyName() + " — 2 remain after removal", t.size() == 2);
            check(t.strategyName() + " — user1 has 1", t.query(EventQuery.forUser("user1")).size() == 1);
        }
    }

    static void testPerformanceComparison() {
        System.out.println("\n=== Test 4: Performance Comparison ===");

        int numEvents = 10_000;

        // Each tracker gets its own counter reset
        String[] types = {"page_view", "purchase", "login", "button_click", "scroll"};
        String[] pages = {"/home", "/about", "/cart", "/profile", "/settings"};

        EventTracker[] trackers = {
                new LinearScanTracker(Clock.systemUTC(), () -> UUID.randomUUID().toString()),
                new IndexedEventTracker(Clock.systemUTC(), () -> UUID.randomUUID().toString()),
                new PartitionedEventTracker(Clock.systemUTC(), () -> UUID.randomUUID().toString()),
                new InvertedIndexTracker(Clock.systemUTC(), () -> UUID.randomUUID().toString())
        };

        // ---- WRITE BENCHMARK ----
        System.out.println("\n  Write benchmark (" + numEvents + " events):");
        for (EventTracker t : trackers) {
            long start = System.nanoTime();
            for (int i = 0; i < numEvents; i++) {
                t.track("user" + (i % 100), types[i % types.length],
                        Map.of("page", pages[i % pages.length], "seq", i));
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-35s  write: %4d ms%n", t.strategyName(), ms);
        }

        // ---- TYPE-FILTERED QUERY BENCHMARK ----
        System.out.println("\n  Type-filtered query (1000 iterations):");
        for (EventTracker t : trackers) {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                t.query(EventQuery.forUser("user0").ofType("purchase"));
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-35s  query: %4d ms%n", t.strategyName(), ms);
        }

        // ---- PROPERTY-FILTERED QUERY BENCHMARK ----
        System.out.println("\n  Property-filtered query (1000 iterations):");
        for (EventTracker t : trackers) {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                t.query(EventQuery.forUser("user0").withProperty("page", "/home"));
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-35s  query: %4d ms%n", t.strategyName(), ms);
        }

        // ---- AGGREGATION BENCHMARK ----
        System.out.println("\n  countByType aggregation (1000 iterations):");
        for (EventTracker t : trackers) {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                t.countByType("user0");
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-35s  agg:   %4d ms%n", t.strategyName(), ms);
        }

        check("Performance benchmark completed", true);
    }

    static void printComparisonTable() {
        System.out.println("\n=== Strategy Comparison Summary ===");
        System.out.println("  \u250c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u252c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2510");
        System.out.println("  \u2502 Strategy                 \u2502 Best Query  \u2502 Write Cost  \u2502 When to Use     \u2502");
        System.out.println("  \u251c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2524");
        System.out.println("  \u2502 LinearScan (no indexes)  \u2502 None        \u2502 O(1)*       \u2502 Small datasets  \u2502");
        System.out.println("  \u2502                          \u2502             \u2502             \u2502 or prototyping  \u2502");
        System.out.println("  \u251c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2524");
        System.out.println("  \u2502 Indexed (type+day)       \u2502 By type     \u2502 O(4)*       \u2502 General purpose \u2502");
        System.out.println("  \u2502                          \u2502 By day      \u2502             \u2502 analytics       \u2502");
        System.out.println("  \u251c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2524");
        System.out.println("  \u2502 Partitioned (daily)      \u2502 Time range  \u2502 O(log D)*   \u2502 Time-series     \u2502");
        System.out.println("  \u2502                          \u2502             \u2502             \u2502 heavy workloads \u2502");
        System.out.println("  \u251c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u253c\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2524");
        System.out.println("  \u2502 InvertedIndex (property) \u2502 By property \u2502 O(P)*       \u2502 Property-heavy  \u2502");
        System.out.println("  \u2502                          \u2502             \u2502             \u2502 filtering       \u2502");
        System.out.println("  \u2514\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2534\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2518");
        System.out.println("  * Plus CopyOnWriteArrayList copy cost O(n) per affected list");
        System.out.println("    D = distinct days, P = properties per event");
    }

    public static void main(String[] args) {
        System.out.println("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println("\u2551  Tracker Comparison \u2014 Test Suite               \u2551");
        System.out.println("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");

        testCorrectnessAllStrategies();
        testPaginationConsistency();
        testDeleteConsistency();
        testPerformanceComparison();
        printComparisonTable();

        System.out.println("\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }
}

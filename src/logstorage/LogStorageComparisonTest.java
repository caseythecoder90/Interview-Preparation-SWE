package logstorage;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * COMPARISON TEST: All Log Storage Strategies Side-by-Side
 * ============================================================
 *
 * Demonstrates:
 *   1. All implementations produce identical results
 *   2. Performance differences across strategies
 *   3. When each strategy excels
 */
public class LogStorageComparisonTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("    PASS: " + testName);
            passed++;
        } else {
            System.out.println("    FAIL: " + testName);
            failed++;
        }
    }

    /**
     * Populates a LogStorage with the standard test dataset.
     */
    private static void populate(LogStorage store) {
        store.put(1,  "2024:01:15:08:30:00", "Server started");
        store.put(2,  "2024:01:15:13:45:22", "User login");
        store.put(3,  "2024:01:15:13:45:22", "Duplicate timestamp");
        store.put(4,  "2024:01:15:23:59:59", "End of day");
        store.put(5,  "2024:01:16:00:00:00", "Start of next day");
        store.put(6,  "2024:01:16:09:00:00", "Batch job started");
        store.put(7,  "2024:01:16:17:30:45", "Batch job completed");
        store.put(8,  "2024:01:17:02:15:00", "Nightly backup");
        store.put(9,  "2024:02:01:10:00:00", "February log");
        store.put(10, "2024:02:01:10:00:00", "Another Feb log");
    }

    // =============================================================
    // TEST 1: All strategies produce identical results
    // =============================================================
    static void testIdenticalResults() {
        System.out.println("\n=== Test 1: Identical Results Across Strategies ===");

        LogStorage[] stores = {
                new TreeMapLogStorage(),
                new EpochTreeMapLogStorage(),
                new BucketedLogStorage(Granularity.DAY)
        };

        for (LogStorage store : stores) {
            populate(store);
        }

        // Test at each granularity level
        for (Granularity g : Granularity.values()) {
            List<Integer> expected = stores[0].retrieve(
                    "2024:01:01:00:00:00", "2024:12:31:23:59:59", g);

            for (int i = 1; i < stores.length; i++) {
                List<Integer> actual = stores[i].retrieve(
                        "2024:01:01:00:00:00", "2024:12:31:23:59:59", g);

                // Compare as sets since order across buckets may differ
                boolean sameSize = expected.size() == actual.size();
                boolean sameContent = expected.containsAll(actual) && actual.containsAll(expected);

                check(stores[i].strategyName() + " matches at " + g.name() +
                        " (" + expected.size() + " entries)", sameSize && sameContent);
            }
        }
    }

    // =============================================================
    // TEST 2: Closest log — same results
    // =============================================================
    static void testClosestLogAcrossStrategies() {
        System.out.println("\n=== Test 2: Closest Log — Same Results ===");

        LogStorage[] stores = {
                new TreeMapLogStorage(),
                new EpochTreeMapLogStorage(),
                new BucketedLogStorage(Granularity.DAY)
        };
        for (LogStorage store : stores) populate(store);

        String queryTs = "2024:01:15:12:00:00";

        LogEntry expectedBefore = stores[0].getClosestLog(queryTs, LogStorage.Direction.BEFORE);
        LogEntry expectedAfter = stores[0].getClosestLog(queryTs, LogStorage.Direction.AFTER);

        for (int i = 1; i < stores.length; i++) {
            LogEntry actualBefore = stores[i].getClosestLog(queryTs, LogStorage.Direction.BEFORE);
            LogEntry actualAfter = stores[i].getClosestLog(queryTs, LogStorage.Direction.AFTER);

            check(stores[i].strategyName() + " BEFORE matches (ID " +
                            (expectedBefore == null ? "null" : expectedBefore.id()) + ")",
                    (expectedBefore == null && actualBefore == null) ||
                            (expectedBefore != null && actualBefore != null &&
                                    expectedBefore.id() == actualBefore.id()));

            check(stores[i].strategyName() + " AFTER matches (ID " +
                            (expectedAfter == null ? "null" : expectedAfter.id()) + ")",
                    (expectedAfter == null && actualAfter == null) ||
                            (expectedAfter != null && actualAfter != null &&
                                    expectedAfter.id() == actualAfter.id()));
        }
    }

    // =============================================================
    // TEST 3: Count by granularity — same results
    // =============================================================
    static void testCountAcrossStrategies() {
        System.out.println("\n=== Test 3: Count By Granularity — Same Results ===");

        LogStorage[] stores = {
                new TreeMapLogStorage(),
                new EpochTreeMapLogStorage(),
                new BucketedLogStorage(Granularity.DAY)
        };
        for (LogStorage store : stores) populate(store);

        Map<String, Long> expected = stores[0].countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.DAY);

        for (int i = 1; i < stores.length; i++) {
            Map<String, Long> actual = stores[i].countByGranularity(
                    "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.DAY);

            boolean match = expected.size() == actual.size() &&
                    expected.entrySet().stream().allMatch(e ->
                            e.getValue().equals(actual.get(e.getKey())));
            check(stores[i].strategyName() + " day counts match", match);
        }
    }

    // =============================================================
    // TEST 4: Performance comparison
    // =============================================================
    static void testPerformanceComparison() {
        System.out.println("\n=== Test 4: Performance Comparison (50K entries) ===");

        LogStorage treeMap = new TreeMapLogStorage();
        LogStorage epoch = new EpochTreeMapLogStorage();
        LogStorage bucketed = new BucketedLogStorage(Granularity.DAY);
        LogStorage[] stores = {treeMap, epoch, bucketed};

        // Insert 50K entries
        for (LogStorage store : stores) {
            int id = 0;
            for (int month = 1; month <= 12; month++) {
                for (int day = 1; day <= 28; day++) {
                    for (int i = 0; i < 149; i++) {
                        String ts = String.format("2024:%02d:%02d:%02d:%02d:%02d",
                                month, day, i % 24, i % 60, i % 60);
                        store.put(id++, ts, null);
                    }
                }
            }
        }

        // Benchmark: narrow query (single day)
        System.out.println("\n  Narrow query (single day):");
        for (LogStorage store : stores) {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                store.retrieve("2024:06:15:00:00:00", "2024:06:15:23:59:59", Granularity.DAY);
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-25s  1000 queries in %d ms%n", store.strategyName(), elapsed);
        }

        // Benchmark: wide query (full year)
        System.out.println("\n  Wide query (full year):");
        for (LogStorage store : stores) {
            long start = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                store.retrieve("2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-25s  10 queries in %d ms%n", store.strategyName(), elapsed);
        }

        // Benchmark: closest log
        System.out.println("\n  Closest log (10K queries):");
        for (LogStorage store : stores) {
            long start = System.nanoTime();
            for (int i = 0; i < 10000; i++) {
                store.getClosestLog("2024:06:15:12:00:00", LogStorage.Direction.BEFORE);
            }
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("    %-25s  10K queries in %d ms%n", store.strategyName(), elapsed);
        }

        check("Performance benchmark completed", true);
    }

    // =============================================================
    // TEST 5: Summary table
    // =============================================================
    static void printComparisonTable() {
        System.out.println("\n=== Strategy Comparison Summary ===");
        System.out.println("  ┌──────────────────────┬──────────┬──────────────┬─────────────┬───────────────┐");
        System.out.println("  │ Strategy             │ put()    │ retrieve()   │ closest()   │ Best For      │");
        System.out.println("  ├──────────────────────┼──────────┼──────────────┼─────────────┼───────────────┤");
        System.out.println("  │ TreeMap (String)     │ O(log n) │ O(log n + k) │ O(log n)    │ Interview,    │");
        System.out.println("  │                      │          │              │             │ general use   │");
        System.out.println("  ├──────────────────────┼──────────┼──────────────┼─────────────┼───────────────┤");
        System.out.println("  │ TreeMap (Epoch)      │ O(log n) │ O(log n + k) │ O(log n)    │ Production,   │");
        System.out.println("  │                      │          │              │             │ memory-tight  │");
        System.out.println("  ├──────────────────────┼──────────┼──────────────┼─────────────┼───────────────┤");
        System.out.println("  │ Bucketed (HashMap +  │ O(log m) │ O(B×log m+k) │ O(B×log m)  │ Archival,     │");
        System.out.println("  │  TreeMap per bucket) │ m<n      │              │             │ sharding      │");
        System.out.println("  └──────────────────────┴──────────┴──────────────┴─────────────┴───────────────┘");
        System.out.println("  n = total entries, k = results, m = entries per bucket, B = buckets in range");
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Log Storage Comparison — Test Suite        ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testIdenticalResults();
        testClosestLogAcrossStrategies();
        testCountAcrossStrategies();
        testPerformanceComparison();

        printComparisonTable();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("══════════════════════════════════════════════");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — LOG STORAGE DESIGN DISCUSSION            │
 * │                                                             │
 * │ Q: "Which implementation would you use in production?"     │
 * │ A: "For a single-server system: EpochTreeMapLogStorage     │
 * │    with ConcurrentSkipListMap for thread safety. For       │
 * │    distributed: time-partitioned sharding (one TreeMap per │
 * │    day/week on each node), similar to BucketedLogStorage." │
 * │                                                             │
 * │ Q: "How does this relate to real log systems?"             │
 * │ A: "ELK Stack: Elasticsearch uses inverted index + time    │
 * │    sharding (index-per-day). InfluxDB: time-structured     │
 * │    merge tree (TSM) with time-based partitioning.          │
 * │    Our TreeMap approach is the simplified in-memory         │
 * │    equivalent of these on-disk structures."                │
 * │                                                             │
 * │ Q: "How would you add full-text search on messages?"       │
 * │ A: "Build an inverted index alongside the TreeMap:         │
 * │    HashMap<String, Set<Integer>> where key=word,           │
 * │    value=set of log IDs. For production: integrate         │
 * │    Lucene (embedded) or Elasticsearch (distributed)."      │
 * │                                                             │
 * │ Q: "How to handle out-of-order log arrival?"               │
 * │ A: "TreeMap handles this naturally — inserts are O(log n)  │
 * │    regardless of order. The tree stays sorted. This is a   │
 * │    major advantage over append-only structures like arrays │
 * │    which would need re-sorting."                            │
 * └─────────────────────────────────────────────────────────────┘
 */

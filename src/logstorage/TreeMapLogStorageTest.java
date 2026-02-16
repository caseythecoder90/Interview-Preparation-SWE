package logstorage;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * COMPREHENSIVE TESTS: TreeMap Log Storage
 * ============================================================
 *
 * Tests cover:
 *   1. Basic put and retrieve
 *   2. Range queries at every granularity level
 *   3. Granularity boundary separation
 *   4. Closest log (floor/ceiling) behavior
 *   5. Count by granularity
 *   6. Duplicate timestamps
 *   7. Edge cases: empty store, no matches, single entry
 *   8. Large dataset performance
 */
public class TreeMapLogStorageTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + testName);
            passed++;
        } else {
            System.out.println("  FAIL: " + testName);
            failed++;
        }
    }

    /**
     * Creates a pre-populated store for testing.
     * Logs span Jan 15 to Feb 1, 2024 with various times.
     */
    private static TreeMapLogStorage createTestStore() {
        TreeMapLogStorage store = new TreeMapLogStorage();
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
        return store;
    }

    // =============================================================
    // TEST 1: Basic put and retrieve
    // =============================================================
    static void testBasicPutRetrieve() {
        System.out.println("\n=== Test 1: Basic Put and Retrieve ===");
        TreeMapLogStorage store = new TreeMapLogStorage();

        store.put(1, "2024:01:15:08:30:00", "test");
        store.put(2, "2024:01:15:13:45:22", "test");

        List<Integer> result = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:15:23:59:59", Granularity.SECOND);
        check("Retrieved 2 entries", result.size() == 2);
        check("Contains ID 1", result.contains(1));
        check("Contains ID 2", result.contains(2));
        check("Total entries is 2", store.totalEntries() == 2);
    }

    // =============================================================
    // TEST 2: Retrieve at YEAR granularity
    // =============================================================
    static void testRetrieveByYear() {
        System.out.println("\n=== Test 2: Retrieve at YEAR granularity ===");
        TreeMapLogStorage store = createTestStore();

        List<Integer> result = store.retrieve(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        check("All 10 entries in 2024", result.size() == 10);

        // 2023 should have nothing
        List<Integer> empty = store.retrieve(
                "2023:01:01:00:00:00", "2023:12:31:23:59:59", Granularity.YEAR);
        check("No entries in 2023", empty.isEmpty());
    }

    // =============================================================
    // TEST 3: Retrieve at MONTH granularity
    // =============================================================
    static void testRetrieveByMonth() {
        System.out.println("\n=== Test 3: Retrieve at MONTH granularity ===");
        TreeMapLogStorage store = createTestStore();

        List<Integer> jan = store.retrieve(
                "2024:01:01:00:00:00", "2024:01:31:23:59:59", Granularity.MONTH);
        check("8 entries in January", jan.size() == 8);

        List<Integer> feb = store.retrieve(
                "2024:02:01:00:00:00", "2024:02:28:23:59:59", Granularity.MONTH);
        check("2 entries in February", feb.size() == 2);
    }

    // =============================================================
    // TEST 4: Retrieve at DAY granularity
    // =============================================================
    static void testRetrieveByDay() {
        System.out.println("\n=== Test 4: Retrieve at DAY granularity ===");
        TreeMapLogStorage store = createTestStore();

        List<Integer> jan15 = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:15:23:59:59", Granularity.DAY);
        check("4 entries on Jan 15 (IDs 1,2,3,4)", jan15.size() == 4);

        List<Integer> jan16 = store.retrieve(
                "2024:01:16:00:00:00", "2024:01:16:23:59:59", Granularity.DAY);
        check("3 entries on Jan 16 (IDs 5,6,7)", jan16.size() == 3);

        List<Integer> jan17 = store.retrieve(
                "2024:01:17:00:00:00", "2024:01:17:23:59:59", Granularity.DAY);
        check("1 entry on Jan 17 (ID 8)", jan17.size() == 1);
    }

    // =============================================================
    // TEST 5: Retrieve at HOUR granularity
    // =============================================================
    static void testRetrieveByHour() {
        System.out.println("\n=== Test 5: Retrieve at HOUR granularity ===");
        TreeMapLogStorage store = createTestStore();

        // Hour 13 on Jan 15 should have IDs 2, 3 (both at 13:45:22)
        List<Integer> hour13 = store.retrieve(
                "2024:01:15:13:00:00", "2024:01:15:13:59:59", Granularity.HOUR);
        check("2 entries at hour 13 on Jan 15", hour13.size() == 2);

        // Hour 08 should have ID 1
        List<Integer> hour08 = store.retrieve(
                "2024:01:15:08:00:00", "2024:01:15:08:59:59", Granularity.HOUR);
        check("1 entry at hour 08 on Jan 15", hour08.size() == 1);
    }

    // =============================================================
    // TEST 6: Granularity boundary — day separation
    // =============================================================
    static void testGranularityBoundary() {
        System.out.println("\n=== Test 6: Granularity Boundary (Day Separation) ===");
        TreeMapLogStorage store = createTestStore();
        // ID 4: "2024:01:15:23:59:59" — last second of Jan 15
        // ID 5: "2024:01:16:00:00:00" — first second of Jan 16

        List<Integer> jan15 = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:15:23:59:59", Granularity.DAY);
        List<Integer> jan16 = store.retrieve(
                "2024:01:16:00:00:00", "2024:01:16:23:59:59", Granularity.DAY);

        check("ID 4 is on Jan 15 (end of day)", jan15.contains(4));
        check("ID 4 is NOT on Jan 16", !jan16.contains(4));
        check("ID 5 is on Jan 16 (start of day)", jan16.contains(5));
        check("ID 5 is NOT on Jan 15", !jan15.contains(5));
    }

    // =============================================================
    // TEST 7: Closest log — BEFORE
    // =============================================================
    static void testClosestLogBefore() {
        System.out.println("\n=== Test 7: Closest Log — BEFORE ===");
        TreeMapLogStorage store = createTestStore();

        // Closest log BEFORE noon on Jan 15
        LogEntry before = store.getClosestLog("2024:01:15:12:00:00", LogStorage.Direction.BEFORE);
        check("BEFORE noon is ID 1 (08:30)", before != null && before.id() == 1);

        // Closest BEFORE the very first log
        LogEntry first = store.getClosestLog("2024:01:15:08:00:00", LogStorage.Direction.BEFORE);
        check("Nothing BEFORE the first log", first == null);

        // Exact match — should return that entry
        LogEntry exact = store.getClosestLog("2024:01:15:08:30:00", LogStorage.Direction.BEFORE);
        check("Exact match returns that entry (ID 1)", exact != null && exact.id() == 1);
    }

    // =============================================================
    // TEST 8: Closest log — AFTER
    // =============================================================
    static void testClosestLogAfter() {
        System.out.println("\n=== Test 8: Closest Log — AFTER ===");
        TreeMapLogStorage store = createTestStore();

        // Closest AFTER noon on Jan 15
        LogEntry after = store.getClosestLog("2024:01:15:12:00:00", LogStorage.Direction.AFTER);
        check("AFTER noon is ID 2 (13:45)", after != null && after.id() == 2);

        // Closest AFTER the very last log
        LogEntry last = store.getClosestLog("2024:03:01:00:00:00", LogStorage.Direction.AFTER);
        check("Nothing AFTER the last log", last == null);

        // Exact match — should return first entry at that timestamp
        LogEntry exact = store.getClosestLog("2024:01:15:13:45:22", LogStorage.Direction.AFTER);
        check("Exact match returns first entry (ID 2)", exact != null && exact.id() == 2);
    }

    // =============================================================
    // TEST 9: Count by granularity
    // =============================================================
    static void testCountByGranularity() {
        System.out.println("\n=== Test 9: Count by Granularity ===");
        TreeMapLogStorage store = createTestStore();

        // Count by DAY
        Map<String, Long> dayCounts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.DAY);
        check("Jan 15 has 4 entries", dayCounts.getOrDefault("2024:01:15", 0L) == 4);
        check("Jan 16 has 3 entries", dayCounts.getOrDefault("2024:01:16", 0L) == 3);
        check("Jan 17 has 1 entry",   dayCounts.getOrDefault("2024:01:17", 0L) == 1);
        check("Feb 01 has 2 entries",  dayCounts.getOrDefault("2024:02:01", 0L) == 2);

        // Count by MONTH
        Map<String, Long> monthCounts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.MONTH);
        check("January has 8 entries", monthCounts.getOrDefault("2024:01", 0L) == 8);
        check("February has 2 entries", monthCounts.getOrDefault("2024:02", 0L) == 2);

        // Count by YEAR
        Map<String, Long> yearCounts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        check("2024 has 10 entries", yearCounts.getOrDefault("2024", 0L) == 10);
    }

    // =============================================================
    // TEST 10: Duplicate timestamps
    // =============================================================
    static void testDuplicateTimestamps() {
        System.out.println("\n=== Test 10: Duplicate Timestamps ===");
        TreeMapLogStorage store = new TreeMapLogStorage();

        store.put(1, "2024:01:15:10:00:00", "First");
        store.put(2, "2024:01:15:10:00:00", "Second");
        store.put(3, "2024:01:15:10:00:00", "Third");

        List<Integer> result = store.retrieve(
                "2024:01:15:10:00:00", "2024:01:15:10:00:00", Granularity.SECOND);
        check("All 3 duplicates retrieved", result.size() == 3);
        check("IDs are [1,2,3]", result.equals(List.of(1, 2, 3)));

        // Only 1 distinct timestamp in the TreeMap
        check("1 distinct timestamp", store.size() == 1);
        check("3 total entries", store.totalEntries() == 3);
    }

    // =============================================================
    // TEST 11: Edge case — empty store
    // =============================================================
    static void testEmptyStore() {
        System.out.println("\n=== Test 11: Empty Store ===");
        TreeMapLogStorage store = new TreeMapLogStorage();

        List<Integer> result = store.retrieve(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        check("Empty store returns empty list", result.isEmpty());

        LogEntry closest = store.getClosestLog("2024:01:15:12:00:00", LogStorage.Direction.BEFORE);
        check("Closest on empty store returns null", closest == null);

        Map<String, Long> counts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.DAY);
        check("Count on empty store returns empty map", counts.isEmpty());
    }

    // =============================================================
    // TEST 12: Edge case — single entry
    // =============================================================
    static void testSingleEntry() {
        System.out.println("\n=== Test 12: Single Entry ===");
        TreeMapLogStorage store = new TreeMapLogStorage();
        store.put(42, "2024:06:15:12:00:00", "Only entry");

        // Should find it at all granularities
        for (Granularity g : Granularity.values()) {
            List<Integer> result = store.retrieve(
                    "2024:06:15:12:00:00", "2024:06:15:12:00:00", g);
            check(g.name() + " finds single entry", result.size() == 1 && result.get(0) == 42);
        }
    }

    // =============================================================
    // TEST 13: Retrieve range (no granularity)
    // =============================================================
    static void testRetrieveRange() {
        System.out.println("\n=== Test 13: Retrieve Range (exact bounds) ===");
        TreeMapLogStorage store = createTestStore();

        List<LogEntry> entries = store.retrieveRange(
                "2024:01:15:08:30:00", "2024:01:16:09:00:00");
        // IDs 1,2,3,4,5,6 are in this range (inclusive both ends)
        check("Range retrieves entries with full LogEntry objects", entries.size() == 6);
        check("First entry is ID 1", entries.get(0).id() == 1);
        check("Entries have messages", entries.get(0).message() != null);
    }

    // =============================================================
    // TEST 14: Multi-day range with DAY granularity
    // =============================================================
    static void testMultiDayRange() {
        System.out.println("\n=== Test 14: Multi-Day Range ===");
        TreeMapLogStorage store = createTestStore();

        // Jan 15-17 at DAY granularity
        List<Integer> result = store.retrieve(
                "2024:01:15:10:00:00", "2024:01:17:10:00:00", Granularity.DAY);
        // DAY granularity: captures ALL of Jan 15, 16, 17
        check("Captures all 8 entries across Jan 15-17", result.size() == 8);
    }

    // =============================================================
    // TEST 15: Performance — 100K entries
    // =============================================================
    static void testPerformanceLargeDataset() {
        System.out.println("\n=== Test 15: Performance (100K entries) ===");
        TreeMapLogStorage store = new TreeMapLogStorage();

        // Insert 100K logs spread across 365 days
        long insertStart = System.currentTimeMillis();
        int id = 0;
        for (int month = 1; month <= 12; month++) {
            for (int day = 1; day <= 28; day++) {  // 28 days for simplicity
                for (int i = 0; i < 297; i++) {    // ~297 per day ≈ 100K total
                    String ts = String.format("2024:%02d:%02d:%02d:%02d:%02d",
                            month, day, i % 24, i % 60, i % 60);
                    store.put(id++, ts, null);
                }
            }
        }
        long insertTime = System.currentTimeMillis() - insertStart;
        System.out.printf("  Inserted %d entries in %d ms%n", store.totalEntries(), insertTime);

        // Range query: single day
        long queryStart = System.currentTimeMillis();
        List<Integer> dayResult = store.retrieve(
                "2024:06:15:00:00:00", "2024:06:15:23:59:59", Granularity.DAY);
        long dayQueryTime = System.currentTimeMillis() - queryStart;
        System.out.printf("  Day query: %d results in %d ms%n", dayResult.size(), dayQueryTime);

        // Range query: single month
        queryStart = System.currentTimeMillis();
        List<Integer> monthResult = store.retrieve(
                "2024:06:01:00:00:00", "2024:06:30:23:59:59", Granularity.MONTH);
        long monthQueryTime = System.currentTimeMillis() - queryStart;
        System.out.printf("  Month query: %d results in %d ms%n", monthResult.size(), monthQueryTime);

        // Range query: full year
        queryStart = System.currentTimeMillis();
        List<Integer> yearResult = store.retrieve(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        long yearQueryTime = System.currentTimeMillis() - queryStart;
        System.out.printf("  Year query: %d results in %d ms%n", yearResult.size(), yearQueryTime);

        // Floor/ceiling query
        queryStart = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            store.getClosestLog("2024:06:15:12:00:00", LogStorage.Direction.BEFORE);
        }
        long closestTime = System.currentTimeMillis() - queryStart;
        System.out.printf("  10K closest-log queries in %d ms%n", closestTime);

        check("Day query returns results", dayResult.size() > 0);
        check("Day query is fast (< 100ms)", dayQueryTime < 100);
        check("Closest-log queries are fast (10K in < 200ms)", closestTime < 200);
    }

    // =============================================================
    // TEST 16: SECOND granularity — exact match
    // =============================================================
    static void testSecondGranularity() {
        System.out.println("\n=== Test 16: SECOND Granularity ===");
        TreeMapLogStorage store = createTestStore();

        // Exact second match
        List<Integer> exact = store.retrieve(
                "2024:01:15:08:30:00", "2024:01:15:08:30:00", Granularity.SECOND);
        check("Exact second returns ID 1", exact.size() == 1 && exact.get(0) == 1);

        // Range that doesn't contain any entry
        List<Integer> miss = store.retrieve(
                "2024:01:15:10:00:00", "2024:01:15:11:00:00", Granularity.SECOND);
        check("No entries in 10:00-11:00 range", miss.isEmpty());
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  TreeMap Log Storage — Test Suite           ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicPutRetrieve();
        testRetrieveByYear();
        testRetrieveByMonth();
        testRetrieveByDay();
        testRetrieveByHour();
        testGranularityBoundary();
        testClosestLogBefore();
        testClosestLogAfter();
        testCountByGranularity();
        testDuplicateTimestamps();
        testEmptyStore();
        testSingleEntry();
        testRetrieveRange();
        testMultiDayRange();
        testPerformanceLargeDataset();
        testSecondGranularity();

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

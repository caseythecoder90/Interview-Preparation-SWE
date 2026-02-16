package logstorage;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * TESTS: Epoch TreeMap Log Storage
 * ============================================================
 *
 * Verifies that the epoch-based implementation produces the same
 * results as the string-based TreeMapLogStorage. The API is identical;
 * only the internal key representation differs.
 */
public class EpochTreeMapLogStorageTest {

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

    private static EpochTreeMapLogStorage createTestStore() {
        EpochTreeMapLogStorage store = new EpochTreeMapLogStorage();
        store.put(1, "2024:01:15:08:30:00", "Server started");
        store.put(2, "2024:01:15:13:45:22", "User login");
        store.put(3, "2024:01:15:13:45:22", "Duplicate timestamp");
        store.put(4, "2024:01:15:23:59:59", "End of day");
        store.put(5, "2024:01:16:00:00:00", "Start of next day");
        store.put(6, "2024:01:16:09:00:00", "Batch job started");
        store.put(7, "2024:01:16:17:30:45", "Batch job completed");
        store.put(8, "2024:01:17:02:15:00", "Nightly backup");
        store.put(9, "2024:02:01:10:00:00", "February log");
        return store;
    }

    // =============================================================
    // TEST 1: Basic retrieve matches string-based implementation
    // =============================================================
    static void testBasicRetrieve() {
        System.out.println("\n=== Test 1: Basic Retrieve ===");
        EpochTreeMapLogStorage store = createTestStore();

        List<Integer> all = store.retrieve(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        check("All 9 entries retrieved", all.size() == 9);
    }

    // =============================================================
    // TEST 2: DAY granularity
    // =============================================================
    static void testDayGranularity() {
        System.out.println("\n=== Test 2: DAY Granularity ===");
        EpochTreeMapLogStorage store = createTestStore();

        List<Integer> jan15 = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:15:23:59:59", Granularity.DAY);
        check("4 entries on Jan 15", jan15.size() == 4);

        List<Integer> jan16 = store.retrieve(
                "2024:01:16:00:00:00", "2024:01:16:23:59:59", Granularity.DAY);
        check("3 entries on Jan 16", jan16.size() == 3);
    }

    // =============================================================
    // TEST 3: Boundary separation (same as string-based test)
    // =============================================================
    static void testBoundarySeparation() {
        System.out.println("\n=== Test 3: Boundary Separation ===");
        EpochTreeMapLogStorage store = createTestStore();

        List<Integer> jan15 = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:15:23:59:59", Granularity.DAY);
        List<Integer> jan16 = store.retrieve(
                "2024:01:16:00:00:00", "2024:01:16:23:59:59", Granularity.DAY);

        check("ID 4 (23:59:59) on Jan 15", jan15.contains(4));
        check("ID 5 (00:00:00) on Jan 16", jan16.contains(5));
        check("ID 4 NOT on Jan 16", !jan16.contains(4));
    }

    // =============================================================
    // TEST 4: Closest log
    // =============================================================
    static void testClosestLog() {
        System.out.println("\n=== Test 4: Closest Log ===");
        EpochTreeMapLogStorage store = createTestStore();

        LogEntry before = store.getClosestLog("2024:01:15:12:00:00", LogStorage.Direction.BEFORE);
        check("BEFORE noon is ID 1", before != null && before.id() == 1);

        LogEntry after = store.getClosestLog("2024:01:15:12:00:00", LogStorage.Direction.AFTER);
        check("AFTER noon is ID 2", after != null && after.id() == 2);

        LogEntry nothing = store.getClosestLog("2024:01:15:08:00:00", LogStorage.Direction.BEFORE);
        check("Nothing BEFORE earliest", nothing == null);
    }

    // =============================================================
    // TEST 5: Count by granularity
    // =============================================================
    static void testCountByGranularity() {
        System.out.println("\n=== Test 5: Count by Granularity ===");
        EpochTreeMapLogStorage store = createTestStore();

        Map<String, Long> counts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.MONTH);
        check("January has 8 entries", counts.getOrDefault("2024:01", 0L) == 8);
        check("February has 1 entry", counts.getOrDefault("2024:02", 0L) == 1);
    }

    // =============================================================
    // TEST 6: Duplicate timestamps
    // =============================================================
    static void testDuplicateTimestamps() {
        System.out.println("\n=== Test 6: Duplicate Timestamps ===");
        EpochTreeMapLogStorage store = createTestStore();

        // IDs 2, 3 share timestamp "2024:01:15:13:45:22"
        List<Integer> result = store.retrieve(
                "2024:01:15:13:45:22", "2024:01:15:13:45:22", Granularity.SECOND);
        check("Both duplicates retrieved", result.size() == 2);
    }

    // =============================================================
    // TEST 7: Empty store
    // =============================================================
    static void testEmptyStore() {
        System.out.println("\n=== Test 7: Empty Store ===");
        EpochTreeMapLogStorage store = new EpochTreeMapLogStorage();

        List<Integer> result = store.retrieve(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        check("Empty store returns empty list", result.isEmpty());

        LogEntry closest = store.getClosestLog("2024:06:15:12:00:00", LogStorage.Direction.BEFORE);
        check("Closest on empty store returns null", closest == null);
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Epoch TreeMap Log Storage — Test Suite     ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicRetrieve();
        testDayGranularity();
        testBoundarySeparation();
        testClosestLog();
        testCountByGranularity();
        testDuplicateTimestamps();
        testEmptyStore();

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

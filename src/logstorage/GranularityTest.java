package logstorage;

/**
 * ============================================================
 * GRANULARITY ENUM — Unit Tests
 * ============================================================
 *
 * Tests the truncation and suffix logic that powers range queries.
 * Getting this right is critical — if bounds are wrong, queries
 * silently return incorrect results.
 */
public class GranularityTest {

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

    private static void checkEquals(String testName, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("  PASS: " + testName);
            passed++;
        } else {
            System.out.println("  FAIL: " + testName + " (expected '" + expected + "', got '" + actual + "')");
            failed++;
        }
    }

    // =============================================================
    // TEST 1: Truncation at each level
    // =============================================================
    static void testTruncation() {
        System.out.println("\n=== Test 1: Truncation ===");
        String ts = "2024:07:15:13:30:45";

        checkEquals("YEAR truncation",   "2024",                Granularity.YEAR.truncate(ts));
        checkEquals("MONTH truncation",  "2024:07",             Granularity.MONTH.truncate(ts));
        checkEquals("DAY truncation",    "2024:07:15",          Granularity.DAY.truncate(ts));
        checkEquals("HOUR truncation",   "2024:07:15:13",       Granularity.HOUR.truncate(ts));
        checkEquals("MINUTE truncation", "2024:07:15:13:30",    Granularity.MINUTE.truncate(ts));
        checkEquals("SECOND truncation", "2024:07:15:13:30:45", Granularity.SECOND.truncate(ts));
    }

    // =============================================================
    // TEST 2: Lower bound construction
    // =============================================================
    static void testLowerBound() {
        System.out.println("\n=== Test 2: Lower Bound ===");
        String ts = "2024:07:15:13:30:45";

        checkEquals("YEAR lower",   "2024:01:01:00:00:00",    Granularity.YEAR.buildLowerBound(ts));
        checkEquals("MONTH lower",  "2024:07:01:00:00:00",    Granularity.MONTH.buildLowerBound(ts));
        checkEquals("DAY lower",    "2024:07:15:00:00:00",    Granularity.DAY.buildLowerBound(ts));
        checkEquals("HOUR lower",   "2024:07:15:13:00:00",    Granularity.HOUR.buildLowerBound(ts));
        checkEquals("MINUTE lower", "2024:07:15:13:30:00",    Granularity.MINUTE.buildLowerBound(ts));
        checkEquals("SECOND lower", "2024:07:15:13:30:45",    Granularity.SECOND.buildLowerBound(ts));
    }

    // =============================================================
    // TEST 3: Upper bound construction
    // =============================================================
    static void testUpperBound() {
        System.out.println("\n=== Test 3: Upper Bound ===");
        String ts = "2024:07:15:13:30:45";

        checkEquals("YEAR upper",   "2024:12:31:23:59:59",    Granularity.YEAR.buildUpperBound(ts));
        checkEquals("MONTH upper",  "2024:07:31:23:59:59",    Granularity.MONTH.buildUpperBound(ts));
        checkEquals("DAY upper",    "2024:07:15:23:59:59",    Granularity.DAY.buildUpperBound(ts));
        checkEquals("HOUR upper",   "2024:07:15:13:59:59",    Granularity.HOUR.buildUpperBound(ts));
        checkEquals("MINUTE upper", "2024:07:15:13:30:59",    Granularity.MINUTE.buildUpperBound(ts));
        checkEquals("SECOND upper", "2024:07:15:13:30:45",    Granularity.SECOND.buildUpperBound(ts));
    }

    // =============================================================
    // TEST 4: Boundary — midnight and end-of-day
    // =============================================================
    static void testBoundaryTimestamps() {
        System.out.println("\n=== Test 4: Boundary Timestamps ===");

        // Midnight
        String midnight = "2024:01:01:00:00:00";
        checkEquals("YEAR lower at midnight", "2024:01:01:00:00:00",
                Granularity.YEAR.buildLowerBound(midnight));
        checkEquals("DAY lower at midnight", "2024:01:01:00:00:00",
                Granularity.DAY.buildLowerBound(midnight));

        // End of day
        String endOfDay = "2024:12:31:23:59:59";
        checkEquals("YEAR upper at end-of-year", "2024:12:31:23:59:59",
                Granularity.YEAR.buildUpperBound(endOfDay));
        checkEquals("DAY upper at end-of-day", "2024:12:31:23:59:59",
                Granularity.DAY.buildUpperBound(endOfDay));
    }

    // =============================================================
    // TEST 5: Lexicographic ordering verification
    // =============================================================
    static void testLexicographicOrdering() {
        System.out.println("\n=== Test 5: Lexicographic Ordering ===");

        // Verify that string comparison matches chronological ordering
        check("Jan < Feb", "2024:01:15:00:00:00".compareTo("2024:02:15:00:00:00") < 0);
        check("Day 5 < Day 15", "2024:01:05:00:00:00".compareTo("2024:01:15:00:00:00") < 0);
        check("Hour 09 < Hour 13", "2024:01:15:09:00:00".compareTo("2024:01:15:13:00:00") < 0);
        check("2023 < 2024", "2023:12:31:23:59:59".compareTo("2024:01:01:00:00:00") < 0);

        // The key insight: zero-padding ensures correct lexicographic ordering.
        // "01" < "02" < "10" < "12" lexicographically — correct!
        // Without zero-padding: "1" < "10" < "2" — WRONG chronological order!
        check("Zero-padded: 01 < 10", "01".compareTo("10") < 0);
        // Without padding, "2" > "10" lexicographically (compares '2' > '1') — wrong chronological order!
        check("Without padding: 2 > 10 (wrong order!)", "2".compareTo("10") > 0);
    }

    // =============================================================
    // TEST 6: Lower bound < Upper bound for all granularities
    // =============================================================
    static void testLowerAlwaysBeforeUpper() {
        System.out.println("\n=== Test 6: Lower < Upper for all granularities ===");
        String ts = "2024:06:15:12:30:30";

        for (Granularity g : Granularity.values()) {
            String lower = g.buildLowerBound(ts);
            String upper = g.buildUpperBound(ts);
            check(g.name() + ": lower ≤ upper", lower.compareTo(upper) <= 0);
        }
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Granularity Enum — Test Suite              ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testTruncation();
        testLowerBound();
        testUpperBound();
        testBoundaryTimestamps();
        testLexicographicOrdering();
        testLowerAlwaysBeforeUpper();

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

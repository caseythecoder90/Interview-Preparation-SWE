package logstorage;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ============================================================
 * EPOCH TREEMAP LOG STORAGE — Numeric Key Alternative
 * ============================================================
 *
 * Same concept as TreeMapLogStorage, but uses epoch milliseconds
 * (long) as keys instead of formatted strings.
 *
 * Data Structure: TreeMap<Long, List<LogEntry>>
 *   - Key:   epoch millis (e.g., 1705312245000L for "2024:01:15:13:30:45")
 *   - Value: list of log entries at that exact timestamp
 *
 * Tradeoffs vs String keys:
 *
 *   Pros:
 *     ✓ 8 bytes per key vs ~40 bytes for String → less memory
 *     ✓ Long comparison is faster than String comparison
 *     ✓ Arithmetic on timestamps is natural (epoch + 86400000 = +1 day)
 *     ✓ No string parsing overhead for range bound computation
 *
 *   Cons:
 *     ✗ Granularity truncation requires date arithmetic (not just substring)
 *     ✗ Human-unreadable keys make debugging harder
 *     ✗ Conversion overhead on every put() and retrieve() call
 *     ✗ Timezone handling becomes the caller's responsibility
 *
 * This implementation demonstrates the tradeoffs — use String keys
 * for interviews (easier to explain) and epoch keys in production
 * (better performance).
 */
public class EpochTreeMapLogStorage implements LogStorage {

    // Custom formatter matching our "YYYY:MM:DD:HH:MM:SS" format
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy:MM:dd:HH:mm:ss");

    private final TreeMap<Long, List<LogEntry>> logs;

    public EpochTreeMapLogStorage() {
        this.logs = new TreeMap<>();
    }

    // =============================================================
    // TIMESTAMP CONVERSION HELPERS
    // =============================================================

    /**
     * Converts our string timestamp to epoch milliseconds.
     *
     * <p>Time Complexity: O(1) — fixed-format parsing
     */
    private static long toEpochMillis(String timestamp) {
        LocalDateTime ldt = LocalDateTime.parse(timestamp, FORMATTER);
        return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * Converts epoch milliseconds back to our string format.
     */
    private static String fromEpochMillis(long epochMillis) {
        LocalDateTime ldt = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        return ldt.format(FORMATTER);
    }

    /**
     * Builds the lower bound epoch for a given timestamp and granularity.
     * Converts to epoch, then truncates to the granularity level.
     */
    private static long buildLowerEpoch(String timestamp, Granularity granularity) {
        // Reuse the string-based truncation, then convert.
        // This keeps the granularity logic in one place (the enum).
        String lower = granularity.buildLowerBound(timestamp);
        return toEpochMillis(lower);
    }

    /**
     * Builds the upper bound epoch for a given timestamp and granularity.
     */
    private static long buildUpperEpoch(String timestamp, Granularity granularity) {
        String upper = granularity.buildUpperBound(timestamp);
        return toEpochMillis(upper);
    }

    // =============================================================
    // INTERFACE IMPLEMENTATION
    // =============================================================

    /**
     * Stores a log entry, converting the timestamp to epoch millis internally.
     *
     * <p>Time Complexity: O(log n) for TreeMap insertion + O(1) for parsing
     * <p>Space Complexity: O(1) per entry
     */
    @Override
    public void put(int id, String timestamp, String message) {
        LogEntry entry = new LogEntry(id, timestamp, message);
        long epochKey = toEpochMillis(timestamp);
        logs.computeIfAbsent(epochKey, k -> new ArrayList<>()).add(entry);
    }

    /**
     * Retrieves log IDs within the range at the specified granularity.
     *
     * <p>Same algorithm as TreeMapLogStorage, but range bounds are computed
     * as epoch longs instead of string concatenation.
     *
     * <p>Time Complexity: O(log n + k)
     * <p>Space Complexity: O(k)
     */
    @Override
    public List<Integer> retrieve(String start, String end, Granularity granularity) {
        long lowerEpoch = buildLowerEpoch(start, granularity);
        long upperEpoch = buildUpperEpoch(end, granularity);

        // subMap with Long keys — same O(log n) view creation
        Map<Long, List<LogEntry>> rangeView = logs.subMap(lowerEpoch, true, upperEpoch, true);

        List<Integer> result = new ArrayList<>();
        for (List<LogEntry> entries : rangeView.values()) {
            for (LogEntry entry : entries) {
                result.add(entry.id());
            }
        }
        return result;
    }

    /**
     * Retrieves full entries in the exact timestamp range.
     *
     * <p>Time Complexity: O(log n + k)
     */
    @Override
    public List<LogEntry> retrieveRange(String start, String end) {
        long startEpoch = toEpochMillis(start);
        long endEpoch = toEpochMillis(end);

        List<LogEntry> result = new ArrayList<>();
        for (List<LogEntry> entries : logs.subMap(startEpoch, true, endEpoch, true).values()) {
            result.addAll(entries);
        }
        return result;
    }

    /**
     * Finds the closest log using floorEntry/ceilingEntry on Long keys.
     *
     * <p>Time Complexity: O(log n)
     */
    @Override
    public LogEntry getClosestLog(String timestamp, Direction direction) {
        long epochKey = toEpochMillis(timestamp);

        Map.Entry<Long, List<LogEntry>> entry = switch (direction) {
            case BEFORE -> logs.floorEntry(epochKey);
            case AFTER -> logs.ceilingEntry(epochKey);
        };

        if (entry == null || entry.getValue().isEmpty()) {
            return null;
        }

        List<LogEntry> entries = entry.getValue();
        return direction == Direction.BEFORE
                ? entries.get(entries.size() - 1)
                : entries.get(0);
    }

    /**
     * Counts entries grouped by granularity.
     *
     * <p>Time Complexity: O(log n + k)
     */
    @Override
    public Map<String, Long> countByGranularity(String start, String end, Granularity granularity) {
        long startEpoch = toEpochMillis(start);
        long endEpoch = toEpochMillis(end);

        Map<String, Long> counts = new LinkedHashMap<>();

        for (Map.Entry<Long, List<LogEntry>> mapEntry :
                logs.subMap(startEpoch, true, endEpoch, true).entrySet()) {
            // Convert epoch back to string for truncation
            String ts = fromEpochMillis(mapEntry.getKey());
            String bucketKey = granularity.truncate(ts);
            counts.merge(bucketKey, (long) mapEntry.getValue().size(), Long::sum);
        }

        return counts;
    }

    @Override
    public String strategyName() {
        return "TreeMap (Epoch keys)";
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== Epoch TreeMap Log Storage Demo ===\n");

        EpochTreeMapLogStorage store = new EpochTreeMapLogStorage();

        store.put(1, "2024:01:15:08:30:00", "Server started");
        store.put(2, "2024:01:15:13:45:22", "User login");
        store.put(3, "2024:01:16:09:00:00", "Batch job");
        store.put(4, "2024:02:01:10:00:00", "February log");

        System.out.println("--- Retrieve at DAY granularity (Jan 15) ---");
        List<Integer> results = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:15:23:59:59", Granularity.DAY);
        System.out.println("  IDs: " + results);  // [1, 2]

        System.out.println("\n--- Closest log to 2024:01:15:12:00:00 ---");
        LogEntry before = store.getClosestLog("2024:01:15:12:00:00", Direction.BEFORE);
        LogEntry after = store.getClosestLog("2024:01:15:12:00:00", Direction.AFTER);
        System.out.println("  BEFORE: " + before);
        System.out.println("  AFTER:  " + after);

        System.out.println("\n--- Count by MONTH ---");
        Map<String, Long> counts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.MONTH);
        counts.forEach((k, v) -> System.out.println("  " + k + " → " + v));

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS (same as TreeMapLogStorage)                  │
 * │   put():     O(log n) + O(1) parsing overhead              │
 * │   retrieve():O(log n + k) + O(1) bound conversion          │
 * │   closest(): O(log n)                                       │
 * │   Space:     O(n) — same, but keys are 8 bytes vs ~40      │
 * │                                                             │
 * │ STRING KEYS vs EPOCH KEYS — WHEN TO USE WHICH              │
 * │                                                             │
 * │   String keys:                                              │
 * │     ✓ Interview: easier to explain truncation trick         │
 * │     ✓ Debugging: human-readable keys                        │
 * │     ✓ No conversion overhead if data arrives as strings    │
 * │     ✗ More memory per key                                  │
 * │     ✗ String comparison slower than long comparison        │
 * │                                                             │
 * │   Epoch keys:                                               │
 * │     ✓ Production: less memory, faster comparison            │
 * │     ✓ Arithmetic: epoch + 86400000 = next day              │
 * │     ✓ Standard: all systems understand epoch millis        │
 * │     ✗ Granularity requires date conversion                 │
 * │     ✗ Timezone pitfalls (UTC vs local)                     │
 * │                                                             │
 * │ Q: "Which would you use in production?"                    │
 * │ A: "Epoch keys. The memory savings add up at scale, long   │
 * │    comparison is a single CPU instruction, and most APIs   │
 * │    accept epoch millis natively. The conversion overhead    │
 * │    is a one-time cost on input/output boundaries."         │
 * └─────────────────────────────────────────────────────────────┘
 */

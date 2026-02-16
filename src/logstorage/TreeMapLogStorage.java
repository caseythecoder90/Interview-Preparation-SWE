package logstorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ============================================================
 * TREEMAP LOG STORAGE — Primary Implementation
 * ============================================================
 *
 * THE interview answer for "Design a Log Storage System."
 *
 * Data Structure: TreeMap<String, List<LogEntry>>
 *   - Key:   timestamp string "YYYY:MM:DD:HH:MM:SS"
 *   - Value: list of log entries at that exact timestamp
 *            (handles duplicate timestamps)
 *
 * Why TreeMap (Red-Black Tree)?
 *   - ORDERED keys → enables range queries via subMap()
 *   - O(log n) for put, get, floorEntry, ceilingEntry
 *   - subMap() returns a VIEW in O(log n), not a copy
 *   - NavigableMap methods (floor, ceiling, lower, higher)
 *     enable nearest-neighbor lookups
 *
 * Why NOT HashMap?
 *   - HashMap has O(1) get/put BUT no ordering
 *   - Range queries would require scanning ALL entries → O(n)
 *   - No floorKey/ceilingKey for nearest-neighbor queries
 *
 * Why NOT ArrayList + binary search?
 *   - Binary search gives O(log n) lookups
 *   - BUT insertions require shifting elements → O(n)
 *   - TreeMap: O(log n) insert AND O(log n) range query
 *
 * Why String keys (not long epoch)?
 *   - Lexicographic ordering matches chronological ordering
 *     (because all fields are zero-padded)
 *   - String truncation enables the granularity trick directly
 *   - No date parsing overhead on every query
 *   - Trade-off: slightly more memory than longs (~40 bytes vs 8 bytes per key)
 */
public class TreeMapLogStorage implements LogStorage {

    // TreeMap guarantees keys are sorted (Red-Black Tree).
    // Natural String ordering (lexicographic) works for our timestamp format
    // because all fields are zero-padded: "2024:01:05" < "2024:01:15" ✓
    private final TreeMap<String, List<LogEntry>> logs;

    public TreeMapLogStorage() {
        this.logs = new TreeMap<>();
    }

    /**
     * Stores a log entry at the given timestamp.
     *
     * <p>If multiple entries share the same timestamp, they are appended
     * to the list (preserving insertion order within that timestamp).
     *
     * <p>Time Complexity: O(log n) — TreeMap.computeIfAbsent does a tree traversal
     * <p>Space Complexity: O(1) amortized per entry (list append)
     *
     * @param id        unique log ID
     * @param timestamp "YYYY:MM:DD:HH:MM:SS" format
     * @param message   log message
     */
    @Override
    public void put(int id, String timestamp, String message) {
        LogEntry entry = new LogEntry(id, timestamp, message);

        // computeIfAbsent: if timestamp key doesn't exist, create a new list.
        // Then append the entry. This handles duplicate timestamps cleanly.
        logs.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(entry);
    }

    /**
     * Retrieves log IDs within the range at the specified granularity.
     *
     * <p>Algorithm (THE key interview explanation):
     * <ol>
     *   <li>Use granularity to build adjusted bounds:
     *       <ul>
     *         <li>lowerBound = truncate(start) + minSuffix</li>
     *         <li>upperBound = truncate(end) + maxSuffix</li>
     *       </ul>
     *   </li>
     *   <li>Call subMap(lowerBound, true, upperBound, true) — O(log n) to create VIEW</li>
     *   <li>Iterate the view collecting IDs — O(k) where k = matching entries</li>
     * </ol>
     *
     * <p>Example — retrieve at DAY granularity:
     * <pre>
     *   start = "2024:01:15:13:30:45"  →  lower = "2024:01:15:00:00:00"
     *   end   = "2024:01:17:08:00:00"  →  upper = "2024:01:17:23:59:59"
     *   subMap captures ALL logs from Jan 15 midnight to Jan 17 end-of-day
     * </pre>
     *
     * <p>Time Complexity: O(log n + k) where n = total entries, k = results in range
     * <p>Space Complexity: O(k) for the result list.
     *    The subMap itself is a VIEW — O(1) additional space!
     *
     * @param start       range start
     * @param end         range end
     * @param granularity query granularity
     * @return list of matching log IDs in timestamp order
     */
    @Override
    public List<Integer> retrieve(String start, String end, Granularity granularity) {
        // Build adjusted bounds using the granularity's truncation + suffix logic.
        // This is the core trick — explain it step by step in the interview.
        String lowerBound = granularity.buildLowerBound(start);
        String upperBound = granularity.buildUpperBound(end);

        // subMap returns a NAVIGABLE VIEW of the portion of the tree between
        // lowerBound and upperBound (both inclusive here).
        //
        // CRITICAL: this is a VIEW, not a copy!
        //   - Creation: O(log n) — finds the start and end positions in the tree
        //   - Iteration: O(k) — walks the tree in order between the bounds
        //   - Memory: O(1) — no data is copied, just two boundary markers
        //   - Modifications to the view affect the original map (and vice versa)
        Map<String, List<LogEntry>> rangeView = logs.subMap(lowerBound, true, upperBound, true);

        List<Integer> result = new ArrayList<>();
        for (List<LogEntry> entries : rangeView.values()) {
            for (LogEntry entry : entries) {
                result.add(entry.id());
            }
        }
        return result;
    }

    /**
     * Retrieves full log entries within the exact range [start, end].
     *
     * <p>Unlike retrieve(), this does NOT apply granularity adjustment —
     * it queries the exact timestamp range.
     *
     * <p>Time Complexity: O(log n + k)
     * <p>Space Complexity: O(k) for the result list
     *
     * @param start range start (inclusive)
     * @param end   range end (inclusive)
     * @return list of LogEntry objects in timestamp order
     */
    @Override
    public List<LogEntry> retrieveRange(String start, String end) {
        List<LogEntry> result = new ArrayList<>();
        for (List<LogEntry> entries : logs.subMap(start, true, end, true).values()) {
            result.addAll(entries);
        }
        return result;
    }

    /**
     * Finds the closest log to the given timestamp.
     *
     * <p>Uses TreeMap's NavigableMap methods:
     *   - BEFORE → floorEntry(timestamp): greatest key ≤ timestamp
     *   - AFTER  → ceilingEntry(timestamp): smallest key ≥ timestamp
     *
     * <p>These are O(log n) — they navigate the Red-Black Tree to find
     * the nearest node without scanning.
     *
     * <p>Time Complexity: O(log n)
     * <p>Space Complexity: O(1)
     *
     * @param timestamp reference timestamp
     * @param direction BEFORE or AFTER
     * @return closest LogEntry, or null if none in that direction
     */
    @Override
    public LogEntry getClosestLog(String timestamp, Direction direction) {
        Map.Entry<String, List<LogEntry>> entry = switch (direction) {
            // floorEntry: greatest key LESS THAN OR EQUAL to timestamp
            // "What's the most recent log at or before this time?"
            case BEFORE -> logs.floorEntry(timestamp);

            // ceilingEntry: smallest key GREATER THAN OR EQUAL to timestamp
            // "What's the next log at or after this time?"
            case AFTER -> logs.ceilingEntry(timestamp);
        };

        if (entry == null || entry.getValue().isEmpty()) {
            return null;
        }

        // For BEFORE: return the last entry at that timestamp (most recent)
        // For AFTER: return the first entry at that timestamp (earliest)
        List<LogEntry> entries = entry.getValue();
        return direction == Direction.BEFORE
                ? entries.get(entries.size() - 1)
                : entries.get(0);
    }

    /**
     * Counts log entries in the range grouped by the specified granularity.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Retrieve all entries in [start, end] exact range</li>
     *   <li>For each entry, truncate its timestamp to the granularity level → bucket key</li>
     *   <li>Count entries per bucket</li>
     * </ol>
     *
     * <p>Returns a LinkedHashMap to preserve chronological order of buckets.
     *
     * <p>Time Complexity: O(log n + k) where k = entries in range
     * <p>Space Complexity: O(b) where b = number of distinct buckets
     *
     * @param start       range start
     * @param end         range end
     * @param granularity grouping granularity
     * @return ordered map of bucket key → count
     */
    @Override
    public Map<String, Long> countByGranularity(String start, String end, Granularity granularity) {
        // LinkedHashMap preserves insertion order → output is chronologically ordered
        Map<String, Long> counts = new LinkedHashMap<>();

        Map<String, List<LogEntry>> rangeView = logs.subMap(start, true, end, true);

        for (Map.Entry<String, List<LogEntry>> mapEntry : rangeView.entrySet()) {
            // Truncate the timestamp to get the bucket key
            String bucketKey = granularity.truncate(mapEntry.getKey());
            counts.merge(bucketKey, (long) mapEntry.getValue().size(), Long::sum);
        }

        return counts;
    }

    @Override
    public String strategyName() {
        return "TreeMap (String keys)";
    }

    /**
     * Returns the total number of distinct timestamps stored.
     */
    public int size() {
        return logs.size();
    }

    /**
     * Returns the total number of log entries (including duplicates at same timestamp).
     */
    public int totalEntries() {
        return logs.values().stream().mapToInt(List::size).sum();
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== TreeMap Log Storage Demo ===\n");

        TreeMapLogStorage store = new TreeMapLogStorage();

        // Insert logs across several days
        store.put(1, "2024:01:15:08:30:00", "Server started");
        store.put(2, "2024:01:15:13:45:22", "User login");
        store.put(3, "2024:01:15:13:45:22", "Duplicate timestamp");
        store.put(4, "2024:01:16:09:00:00", "Batch job started");
        store.put(5, "2024:01:16:17:30:45", "Batch job completed");
        store.put(6, "2024:01:17:02:15:00", "Nightly backup");
        store.put(7, "2024:02:01:10:00:00", "February log");

        // 1. Retrieve by granularity
        System.out.println("--- Retrieve at DAY granularity (Jan 15-16) ---");
        List<Integer> dayResults = store.retrieve(
                "2024:01:15:00:00:00", "2024:01:16:23:59:59", Granularity.DAY);
        System.out.println("  IDs: " + dayResults);  // [1, 2, 3, 4, 5]

        System.out.println("\n--- Retrieve at MONTH granularity (all of Jan) ---");
        List<Integer> monthResults = store.retrieve(
                "2024:01:01:00:00:00", "2024:01:31:23:59:59", Granularity.MONTH);
        System.out.println("  IDs: " + monthResults);  // [1, 2, 3, 4, 5, 6]

        System.out.println("\n--- Retrieve at YEAR granularity (all of 2024) ---");
        List<Integer> yearResults = store.retrieve(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.YEAR);
        System.out.println("  IDs: " + yearResults);  // [1, 2, 3, 4, 5, 6, 7]

        // 2. Closest log
        System.out.println("\n--- Closest log to 2024:01:15:12:00:00 ---");
        LogEntry before = store.getClosestLog("2024:01:15:12:00:00", Direction.BEFORE);
        LogEntry after = store.getClosestLog("2024:01:15:12:00:00", Direction.AFTER);
        System.out.println("  BEFORE: " + before);  // id=1, 08:30:00
        System.out.println("  AFTER:  " + after);   // id=2, 13:45:22

        // 3. Count by granularity
        System.out.println("\n--- Count by DAY (all entries) ---");
        Map<String, Long> dayCounts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.DAY);
        dayCounts.forEach((k, v) -> System.out.println("  " + k + " → " + v));

        System.out.println("\n--- Count by MONTH (all entries) ---");
        Map<String, Long> monthCounts = store.countByGranularity(
                "2024:01:01:00:00:00", "2024:12:31:23:59:59", Granularity.MONTH);
        monthCounts.forEach((k, v) -> System.out.println("  " + k + " → " + v));

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   put():              O(log n)                              │
 * │   retrieve():         O(log n + k)  k = matching entries   │
 * │   retrieveRange():    O(log n + k)                          │
 * │   getClosestLog():    O(log n)                              │
 * │   countByGranularity: O(log n + k)                          │
 * │   Space: O(n) for all entries                               │
 * │                                                             │
 * │ subMap() IS A VIEW — WHAT DOES THAT MEAN?                  │
 * │                                                             │
 * │   view = treeMap.subMap(lo, hi)                             │
 * │   - O(log n) to create — just stores two boundary markers  │
 * │   - O(1) additional memory — NO data is copied             │
 * │   - Iterating the view is O(k) where k = entries in range  │
 * │   - view.put() / view.remove() MODIFIES the original map! │
 * │   - If you modify the original map, the view reflects it   │
 * │   - ConcurrentModificationException if you iterate the     │
 * │     view while modifying the original (same as any Map)    │
 * │                                                             │
 * │ COMMON FOLLOW-UP QUESTIONS                                  │
 * │                                                             │
 * │ Q: "How would you handle millions of logs?"                │
 * │ A: "Three strategies:                                      │
 * │    1. Time-partitioned sharding: one TreeMap per day/week  │
 * │    2. Archive old data to cold storage (S3, HDFS)          │
 * │    3. Compaction: merge fine-grained entries into summaries │
 * │    In practice, use a time-series DB (InfluxDB, TimescaleDB)│
 * │    which does all of this internally."                      │
 * │                                                             │
 * │ Q: "What about concurrent writes?"                         │
 * │ A: "ConcurrentSkipListMap is a drop-in replacement for     │
 * │    TreeMap — same NavigableMap interface, thread-safe.      │
 * │    It uses skip lists instead of Red-Black trees.           │
 * │    Alternatively, use a ReadWriteLock around the TreeMap."  │
 * │                                                             │
 * │ Q: "How would you persist this?"                           │
 * │ A: "LSM-tree storage (LevelDB/RocksDB): keys are sorted   │
 * │    on disk, range scans are efficient. Add a WAL (Write-   │
 * │    Ahead Log) for durability. This is how real log systems │
 * │    (Kafka, Cassandra) work internally."                    │
 * │                                                             │
 * │ Q: "How to delete old logs efficiently?"                   │
 * │ A: "treeMap.headMap(cutoffTimestamp).clear() removes all   │
 * │    entries before the cutoff. O(k log n) where k = entries │
 * │    deleted. For TTL: background thread calls headMap.clear()│
 * │    periodically."                                           │
 * │                                                             │
 * │ Q: "What if timestamps aren't unique?"                     │
 * │ A: "That's why the value is List<LogEntry>, not LogEntry.  │
 * │    Multiple logs at the same timestamp are stored in the   │
 * │    same list, preserving insertion order."                  │
 * │                                                             │
 * │ TREEMAP INTERNALS (interviewers love to probe)             │
 * │   - Red-Black Tree: self-balancing BST                     │
 * │   - Height guaranteed ≤ 2×log₂(n+1)                       │
 * │   - Rotations on insert/delete maintain balance            │
 * │   - Iterator: in-order traversal → sorted output           │
 * │   - subMap: stores fromKey and toKey, iterator skips       │
 * │     entries outside range                                   │
 * └─────────────────────────────────────────────────────────────┘
 */

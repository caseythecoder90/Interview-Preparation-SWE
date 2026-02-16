package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 2: TREEMAP INTERVIEW APPLICATIONS
 * ============================================================
 *
 * Three applications:
 *   1. Log Storage System (confirmed Fullstory problem)
 *   2. Calendar / Interval scheduling
 *   3. Counting with sorted order
 */
public class S2_TreeMapApplications {

    // =============================================================
    // 2.1  LOG STORAGE SYSTEM (Fullstory Coding Problem)
    // =============================================================
    //
    // Design a system that:
    //   - Stores log messages with timestamps
    //   - Retrieves all logs within a time range efficiently
    //
    // WHY TREEMAP?
    //   - Timestamps are naturally ordered → TreeMap keeps them sorted
    //   - Range queries (subMap) are O(log n + k) where k = results returned
    //   - HashMap would require scanning ALL entries for a range → O(n)
    //
    // Design decisions:
    //   - Key: Long timestamp (epoch millis or custom)
    //   - Value: List<String> — multiple logs can share a timestamp
    //   - Use subMap(start, true, end, true) for inclusive range queries

    static class LogStorageSystem {
        // TreeMap: timestamp → list of log messages at that time
        private TreeMap<Long, List<String>> logs = new TreeMap<>();

        /** Add a log message at the given timestamp. O(log n). */
        public void addLog(long timestamp, String message) {
            logs.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(message);
        }

        /**
         * Retrieve all logs in [start, end] inclusive. O(log n + k).
         *
         * subMap(start, true, end, true) returns a VIEW of all entries
         * with keys in [start, end]. We then collect all messages.
         */
        public List<String> getLogsInRange(long start, long end) {
            List<String> result = new ArrayList<>();
            // subMap returns a NavigableMap view — sorted, O(log n) to find bounds
            for (List<String> messages : logs.subMap(start, true, end, true).values()) {
                result.addAll(messages);
            }
            return result;
        }

        /** Get the most recent log entry. O(log n). */
        public Map.Entry<Long, List<String>> getMostRecent() {
            return logs.lastEntry();
        }

        /** Get the oldest log entry. O(log n). */
        public Map.Entry<Long, List<String>> getOldest() {
            return logs.firstEntry();
        }

        /**
         * Get logs at or before a given timestamp (most recent up to that point).
         * Uses floorEntry to find the nearest timestamp ≤ given.
         */
        public Map.Entry<Long, List<String>> getLogsAtOrBefore(long timestamp) {
            return logs.floorEntry(timestamp);
        }

        /** Get total number of distinct timestamps stored. */
        public int timestampCount() {
            return logs.size();
        }
    }

    // =============================================================
    // 2.2  CALENDAR / INTERVAL SCHEDULING
    // =============================================================
    //
    // Problem: given a calendar of booked events, determine if a new
    // event conflicts with any existing event.
    //
    // APPROACH: TreeMap where key = start time, value = end time.
    //   To check if [newStart, newEnd) conflicts:
    //     1. floorEntry(newStart): find the event starting at or before newStart
    //        → if its end > newStart, there's overlap
    //     2. ceilingEntry(newStart): find the event starting at or after newStart
    //        → if its start < newEnd, there's overlap
    //
    //   This is O(log n) per booking — much better than scanning all events.

    static class MyCalendar {
        // key = event start time, value = event end time
        private TreeMap<Integer, Integer> calendar = new TreeMap<>();

        /**
         * Try to book [start, end). Returns true if no conflict, false otherwise.
         *
         *   Overlap cases:
         *     Case 1: existing event starts before newStart but ends after it
         *       prev.end > newStart
         *       |---prev---|
         *            |---new---|
         *
         *     Case 2: existing event starts after newStart but before newEnd
         *       next.start < newEnd
         *            |---next---|
         *       |---new---|
         */
        public boolean book(int start, int end) {
            // Check event starting at or before this one
            Map.Entry<Integer, Integer> prev = calendar.floorEntry(start);
            if (prev != null && prev.getValue() > start) {
                return false; // previous event overlaps
            }

            // Check event starting at or after this one
            Map.Entry<Integer, Integer> next = calendar.ceilingEntry(start + 1);
            if (next != null && next.getKey() < end) {
                return false; // next event overlaps
            }

            calendar.put(start, end);
            return true;
        }
    }

    // =============================================================
    // 2.3  COUNTING WITH SORTED ORDER
    // =============================================================
    //
    // Sometimes you need BOTH frequency counting AND sorted iteration.
    // HashMap gives fast counting; TreeMap gives sorted order.
    //
    // Example: find the first character that appears exactly once,
    // but return them in alphabetical order.

    /** Count character frequencies, iterate in sorted (alphabetical) order. */
    public static Character firstUniqueCharSorted(String s) {
        TreeMap<Character, Integer> freq = new TreeMap<>();
        for (char c : s.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
        }
        // Iteration is in sorted order (alphabetical)
        for (Map.Entry<Character, Integer> entry : freq.entrySet()) {
            if (entry.getValue() == 1) return entry.getKey();
        }
        return null;
    }

    /**
     * Count occurrences in a sorted score range.
     * Example: how many students scored between 70 and 89?
     */
    public static int countInRange(TreeMap<Integer, Integer> scoreFreq, int low, int high) {
        int count = 0;
        for (int freq : scoreFreq.subMap(low, true, high, true).values()) {
            count += freq;
        }
        return count;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        // --- Log Storage System ---
        System.out.println("=== LOG STORAGE SYSTEM ===");

        LogStorageSystem logSystem = new LogStorageSystem();
        logSystem.addLog(1000, "Server started");
        logSystem.addLog(1500, "User login: alice");
        logSystem.addLog(2000, "API call: /search");
        logSystem.addLog(2000, "API call: /profile"); // same timestamp
        logSystem.addLog(2500, "User login: bob");
        logSystem.addLog(3000, "Error: timeout");
        logSystem.addLog(3500, "Server shutdown");

        System.out.println("All logs [1500, 2500]:");
        for (String log : logSystem.getLogsInRange(1500, 2500)) {
            System.out.println("  " + log);
        }
        // User login: alice, API call: /search, API call: /profile, User login: bob

        System.out.println("Most recent: " + logSystem.getMostRecent());
        // 3500=[Server shutdown]

        System.out.println("Oldest: " + logSystem.getOldest());
        // 1000=[Server started]

        System.out.println("At or before 1800: " + logSystem.getLogsAtOrBefore(1800));
        // 1500=[User login: alice]

        System.out.println("Total timestamps: " + logSystem.timestampCount()); // 6

        // --- Calendar Scheduling ---
        System.out.println("\n=== CALENDAR SCHEDULING ===");

        MyCalendar cal = new MyCalendar();
        System.out.println("Book [10, 20): " + cal.book(10, 20)); // true
        System.out.println("Book [15, 25): " + cal.book(15, 25)); // false (overlaps)
        System.out.println("Book [20, 30): " + cal.book(20, 30)); // true (no overlap: starts at 20, prev ends at 20)
        System.out.println("Book [5, 10):  " + cal.book(5, 10));  // true
        System.out.println("Book [25, 35): " + cal.book(25, 35)); // false (overlaps [20,30))
        System.out.println("Book [30, 35): " + cal.book(30, 35)); // true

        // --- Counting with sorted order ---
        System.out.println("\n=== SORTED FREQUENCY COUNTING ===");
        System.out.println("First unique (sorted) in 'stress': " +
            firstUniqueCharSorted("stress")); // t (alphabetically first unique)

        TreeMap<Integer, Integer> scores = new TreeMap<>();
        scores.put(65, 3);   // 3 students scored 65
        scores.put(72, 5);   // 5 students scored 72
        scores.put(85, 4);   // 4 students scored 85
        scores.put(91, 2);   // 2 students scored 91
        scores.put(78, 6);   // 6 students scored 78

        System.out.println("Students scoring 70-89: " + countInRange(scores, 70, 89));
        // 5 + 6 + 4 = 15
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — TREEMAP APPLICATIONS                        │
 * │                                                             │
 * │ 1. LOG STORAGE: explain why TreeMap over HashMap:            │
 * │    "Range queries on timestamps need sorted access.         │
 * │    TreeMap's subMap gives O(log n + k) range retrieval.     │
 * │    HashMap would require scanning all entries."             │
 * │                                                             │
 * │ 2. CALENDAR: the floorEntry + ceilingEntry two-check        │
 * │    pattern for overlap detection is reusable. Memorize it.  │
 * │    Check the event BEFORE and the event AFTER the new one.  │
 * │                                                             │
 * │ 3. computeIfAbsent for multi-value maps:                    │
 * │    logs.computeIfAbsent(ts, k -> new ArrayList<>()).add(m)  │
 * │    This one-liner creates the list if needed and adds to    │
 * │    it. Much cleaner than if/else.                           │
 * │                                                             │
 * │ 4. When explaining Log Storage, mention SCALABILITY:         │
 * │    "For a distributed system, I'd shard by time range and   │
 * │    use a storage engine with sorted keys (like LSM trees    │
 * │    in Cassandra). The in-memory TreeMap models the same     │
 * │    access pattern."                                         │
 * └─────────────────────────────────────────────────────────────┘
 */

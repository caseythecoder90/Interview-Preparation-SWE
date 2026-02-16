package logstorage;

/**
 * ============================================================
 * GRANULARITY ENUM — The Key Interview Insight
 * ============================================================
 *
 * Defines the 6 levels of time granularity for log queries.
 * Each level knows how to truncate a timestamp and generate
 * min/max suffixes for range queries.
 *
 * THE TRICK (explain this in the interview):
 *
 *   When querying at "Day" granularity, the caller means:
 *   "Give me all logs from this DAY, regardless of hour/minute/second."
 *
 *   To do this with a TreeMap subMap, we need to:
 *   1. TRUNCATE the start timestamp to the day level → "2024:01:15"
 *   2. APPEND the minimum suffix → "2024:01:15:00:00:00"
 *   3. TRUNCATE the end timestamp to the day level → "2024:01:17"
 *   4. APPEND the maximum suffix → "2024:01:17:23:59:59"
 *   5. Call subMap("2024:01:15:00:00:00", true, "2024:01:17:23:59:59", true)
 *
 *   This captures ALL logs on Jan 15, 16, and 17 at any time of day.
 *
 * Timestamp format: "YYYY:MM:DD:HH:MM:SS"
 * Index positions:   0123456789012345678
 *                    ^   ^  ^  ^  ^  ^
 *                    0   4  7  10 13 16  19(length)
 *
 * Each granularity defines:
 *   - truncateIndex: where to cut the timestamp string
 *   - minSuffix: appended to start → earliest possible time at this granularity
 *   - maxSuffix: appended to end → latest possible time at this granularity
 */
public enum Granularity {

    // Each constant: (truncateIndex, minSuffix, maxSuffix)
    //
    // YEAR truncates to "2024" (4 chars), then:
    //   min = ":01:01:00:00:00" (Jan 1st 00:00:00)
    //   max = ":12:31:23:59:59" (Dec 31st 23:59:59)
    YEAR(4, ":01:01:00:00:00", ":12:31:23:59:59"),

    // MONTH truncates to "2024:01" (7 chars), then:
    //   min = ":01:00:00:00" (1st of month, midnight)
    //   max = ":31:23:59:59" (31st of month, end of day)
    //   Note: using 31 is intentionally conservative — lexicographic
    //   comparison still works because no real timestamp has day > 31
    MONTH(7, ":01:00:00:00", ":31:23:59:59"),

    // DAY truncates to "2024:01:15" (10 chars)
    DAY(10, ":00:00:00", ":23:59:59"),

    // HOUR truncates to "2024:01:15:13" (13 chars)
    HOUR(13, ":00:00", ":59:59"),

    // MINUTE truncates to "2024:01:15:13:30" (16 chars)
    MINUTE(16, ":00", ":59"),

    // SECOND truncates to full timestamp (19 chars) — no suffix needed
    SECOND(19, "", "");

    private final int truncateIndex;
    private final String minSuffix;
    private final String maxSuffix;

    Granularity(int truncateIndex, String minSuffix, String maxSuffix) {
        this.truncateIndex = truncateIndex;
        this.minSuffix = minSuffix;
        this.maxSuffix = maxSuffix;
    }

    /**
     * Truncates a full timestamp to this granularity level.
     *
     * <p>Examples at DAY granularity:
     *   "2024:01:15:13:30:45" → "2024:01:15"
     *   "2024:12:31:23:59:59" → "2024:12:31"
     *
     * <p>Time Complexity: O(1) — substring operation
     *
     * @param timestamp full "YYYY:MM:DD:HH:MM:SS" timestamp
     * @return truncated timestamp string
     */
    public String truncate(String timestamp) {
        return timestamp.substring(0, truncateIndex);
    }

    /**
     * Builds the LOWER bound for a range query at this granularity.
     *
     * <p>Truncates the timestamp and appends the minimum suffix,
     * creating the earliest possible timestamp at this granularity level.
     *
     * <p>Example at DAY granularity:
     *   "2024:01:15:13:30:45" → "2024:01:15" + ":00:00:00" = "2024:01:15:00:00:00"
     *
     * @param timestamp the start timestamp
     * @return adjusted lower bound for subMap query
     */
    public String buildLowerBound(String timestamp) {
        return truncate(timestamp) + minSuffix;
    }

    /**
     * Builds the UPPER bound for a range query at this granularity.
     *
     * <p>Truncates the timestamp and appends the maximum suffix,
     * creating the latest possible timestamp at this granularity level.
     *
     * <p>Example at DAY granularity:
     *   "2024:01:17:08:00:00" → "2024:01:17" + ":23:59:59" = "2024:01:17:23:59:59"
     *
     * @param timestamp the end timestamp
     * @return adjusted upper bound for subMap query
     */
    public String buildUpperBound(String timestamp) {
        return truncate(timestamp) + maxSuffix;
    }

    /**
     * Returns the minimum suffix for this granularity.
     */
    public String getMinSuffix() {
        return minSuffix;
    }

    /**
     * Returns the maximum suffix for this granularity.
     */
    public String getMaxSuffix() {
        return maxSuffix;
    }

    /**
     * Returns the truncation index.
     */
    public int getTruncateIndex() {
        return truncateIndex;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        String ts = "2024:07:15:13:30:45";
        System.out.println("=== Granularity Demo ===\n");
        System.out.println("Full timestamp: " + ts + "\n");

        for (Granularity g : Granularity.values()) {
            System.out.printf("%-8s  truncate: %-19s  lower: %-19s  upper: %-19s%n",
                    g.name(),
                    g.truncate(ts),
                    g.buildLowerBound(ts),
                    g.buildUpperBound(ts));
        }
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ THE GRANULARITY TRICK — STEP BY STEP                        │
 * │                                                             │
 * │ Problem: retrieve all logs on Jan 15, 2024 (DAY granularity)│
 * │                                                             │
 * │ Naive approach: iterate ALL logs, parse each timestamp,     │
 * │   compare day by day → O(n). Terrible for large datasets.  │
 * │                                                             │
 * │ TreeMap approach with truncation + suffix:                  │
 * │   1. start = "2024:01:15:08:00:00" → truncate → "2024:01:15"│
 * │   2. Append min suffix → "2024:01:15:00:00:00"             │
 * │   3. end   = "2024:01:15:20:00:00" → truncate → "2024:01:15"│
 * │   4. Append max suffix → "2024:01:15:23:59:59"             │
 * │   5. subMap("2024:01:15:00:00:00", "2024:01:15:23:59:59")  │
 * │   → O(log n) to create view, O(k) to iterate results      │
 * │                                                             │
 * │ WHY MONTH MAX SUFFIX USES ":31" (NOT ":28" or ":30")       │
 * │                                                             │
 * │ We use ":31:23:59:59" for MONTH's max suffix because:      │
 * │   - Lexicographic comparison: "2024:02:28" < "2024:02:31"  │
 * │   - Even though Feb 31 doesn't exist, no real timestamp    │
 * │     will be > "2024:02:28:23:59:59" but < "2024:02:31:..." │
 * │   - Using 31 is safe and avoids needing month-length logic │
 * │   - This is a clean trick — mention it proactively!        │
 * │                                                             │
 * │ WHY AN ENUM (NOT JUST STRINGS)?                             │
 * │   - Type safety: can't pass "Dya" as a granularity         │
 * │   - Encapsulates behavior (truncate, suffix) with the data │
 * │   - Extensible: adding WEEK would be one new constant      │
 * │   - Shows the interviewer you know Java enums are powerful  │
 * └─────────────────────────────────────────────────────────────┘
 */

package logstorage;

import java.util.List;
import java.util.Map;

/**
 * ============================================================
 * LOG STORAGE — Common Interface (Strategy Pattern)
 * ============================================================
 *
 * All log storage implementations share this contract.
 * Allows swapping between TreeMap-based, epoch-based, and
 * bucketed implementations without changing client code.
 */
public interface LogStorage {

    /**
     * Direction for closest-log queries.
     * BEFORE → floorEntry (at or before timestamp)
     * AFTER  → ceilingEntry (at or after timestamp)
     */
    enum Direction {
        BEFORE, AFTER
    }

    /**
     * Stores a log entry.
     *
     * @param id        unique identifier
     * @param timestamp timestamp in "YYYY:MM:DD:HH:MM:SS" format
     * @param message   log message (may be null)
     */
    void put(int id, String timestamp, String message);

    /**
     * Retrieves log IDs within the given range at the specified granularity.
     *
     * <p>The granularity determines how timestamps are compared:
     * at DAY granularity, "2024:01:15:13:30:45" matches any log on 2024:01:15.
     *
     * @param start       range start timestamp
     * @param end         range end timestamp
     * @param granularity the comparison granularity
     * @return list of matching log IDs, in timestamp order
     */
    List<Integer> retrieve(String start, String end, Granularity granularity);

    /**
     * Retrieves full log entries within the exact timestamp range [start, end].
     *
     * @param start range start (inclusive)
     * @param end   range end (inclusive)
     * @return list of LogEntry objects in timestamp order
     */
    List<LogEntry> retrieveRange(String start, String end);

    /**
     * Finds the closest log entry to the given timestamp in the specified direction.
     *
     * @param timestamp the reference timestamp
     * @param direction BEFORE (floorEntry) or AFTER (ceilingEntry)
     * @return the closest LogEntry, or null if none exists in that direction
     */
    LogEntry getClosestLog(String timestamp, Direction direction);

    /**
     * Counts log entries within a range, grouped by the specified granularity.
     *
     * <p>Example: countByGranularity(start, end, DAY) returns a map like:
     *   {"2024:01:15" → 42, "2024:01:16" → 17, "2024:01:17" → 8}
     *
     * @param start       range start timestamp
     * @param end         range end timestamp
     * @param granularity the grouping granularity
     * @return ordered map of bucket key → count
     */
    Map<String, Long> countByGranularity(String start, String end, Granularity granularity);

    /**
     * Returns the strategy name for logging and comparison tests.
     */
    String strategyName();
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ WHY AN INTERFACE?                                           │
 * │   Strategy Pattern: client code programs to LogStorage, not │
 * │   to a specific implementation. This allows:               │
 * │   - Swapping TreeMap for epoch-based in production          │
 * │   - Testing with a mock implementation                      │
 * │   - A/B testing different storage strategies                │
 * │                                                             │
 * │ WHY DIRECTION AS A NESTED ENUM?                             │
 * │   - Logically belongs to LogStorage (not standalone)        │
 * │   - Keeps the package clean — one fewer top-level type     │
 * │   - Access: LogStorage.Direction.BEFORE                     │
 * └─────────────────────────────────────────────────────────────┘
 */

package logstorage;

/**
 * ============================================================
 * LOG ENTRY — Immutable Data Record
 * ============================================================
 *
 * Represents a single log entry with a unique ID, timestamp, and message.
 *
 * Why a record (Java 17+)?
 *   - Immutable by default — no setters, thread-safe without synchronization
 *   - Auto-generated equals(), hashCode(), toString()
 *   - Concise — no boilerplate getters/constructors
 *   - Clearly communicates "this is data, not behavior"
 *
 * Timestamp format: "YYYY:MM:DD:HH:MM:SS"
 *   Example: "2024:01:15:13:30:45"
 *
 * Why colon-separated (not ISO 8601)?
 *   - Uniform separator makes string truncation trivial
 *   - Lexicographic ordering matches chronological ordering
 *     because all fields are zero-padded
 *   - "2024:01:15" < "2024:02:01" lexicographically — correct!
 *   - This is the key insight that makes TreeMap range queries work
 *     with string keys
 *
 * @param id        unique identifier for this log entry
 * @param timestamp timestamp in "YYYY:MM:DD:HH:MM:SS" format
 * @param message   log message (may be null for minimal entries)
 */
public record LogEntry(int id, String timestamp, String message) {

    /**
     * Compact constructor — validates timestamp format.
     * Records allow custom validation in the compact constructor.
     */
    public LogEntry {
        if (timestamp == null || timestamp.length() != 19) {
            throw new IllegalArgumentException(
                    "Timestamp must be in YYYY:MM:DD:HH:MM:SS format (19 chars), got: " + timestamp);
        }
    }

    /**
     * Convenience constructor without message.
     */
    public LogEntry(int id, String timestamp) {
        this(id, timestamp, null);
    }

    @Override
    public String toString() {
        return String.format("LogEntry{id=%d, ts='%s', msg='%s'}", id, timestamp, message);
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ WHY A RECORD?                                               │
 * │   Records are Java 17+ "data carriers." In an interview,   │
 * │   using a record signals: "This class holds data. It's     │
 * │   immutable and I get equals/hashCode/toString for free."  │
 * │                                                             │
 * │ WHY NOT A PLAIN CLASS?                                      │
 * │   A class works fine, but a record is more concise and     │
 * │   communicates intent. In a 45-minute interview, saving    │
 * │   10 lines of boilerplate matters.                          │
 * │                                                             │
 * │ WHY STORE BOTH ID AND TIMESTAMP?                            │
 * │   The ID is the external identifier returned to callers.   │
 * │   The timestamp is the key for range queries. Storing both │
 * │   lets us decouple "how we look things up" (timestamp)     │
 * │   from "what we return" (id). This mirrors real log        │
 * │   systems where entries have both a sequence number and a  │
 * │   timestamp.                                                │
 * └─────────────────────────────────────────────────────────────┘
 */

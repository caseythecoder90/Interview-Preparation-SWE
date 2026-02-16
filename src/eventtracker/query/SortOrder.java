package eventtracker.query;

/**
 * ============================================================
 * SORT ORDER — Controls chronological direction of query results
 * ============================================================
 *
 * CHRONOLOGICAL: oldest first (natural insertion order — FREE if list is already ordered)
 * REVERSE_CHRONOLOGICAL: newest first (common for "recent activity" feeds)
 *
 * Why this matters for performance:
 *   Events are stored in insertion order (chronological). Returning them
 *   chronologically requires NO sorting — just iterate the list.
 *   Reverse chronological requires either:
 *     (a) Reversing after filtering — O(n)
 *     (b) Iterating backwards from the end — O(1) per element but harder with streams
 *   For interview: mention that many analytics UIs default to reverse-chronological,
 *   so optimizing that path (e.g., storing in reverse, or maintaining a reverse index)
 *   can cut latency in half for the most common query pattern.
 */
public enum SortOrder {
    CHRONOLOGICAL,
    REVERSE_CHRONOLOGICAL
}

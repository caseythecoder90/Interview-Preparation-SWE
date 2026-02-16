package lrucache;

/**
 * ============================================================
 * PART 7: QUICK REFERENCE CARD
 * ============================================================
 *
 * Glance at this before the interview. Everything on one page.
 *
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ DATA STRUCTURES                                             │
 * │                                                             │
 * │   HashMap<Integer, Node>  — O(1) key → node lookup          │
 * │   Doubly Linked List      — O(1) insert/remove, tracks order│
 * │   Sentinel HEAD and TAIL  — eliminate all null checks        │
 * │   Node stores KEY + VALUE — key needed for eviction from map │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ HELPER METHODS                                               │
 * │                                                             │
 * │   addToFront(node) — 4 pointer updates, insert after HEAD   │
 * │   removeNode(node) — 2 pointer updates, unlink from list    │
 * │   moveToFront(node)— remove + add (marks as MRU)            │
 * │   evict()          — remove tail.prev from list AND map     │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ GET ALGORITHM (3 steps)                                      │
 * │                                                             │
 * │   1. map.get(key) → node                                    │
 * │   2. If null → return -1                                    │
 * │   3. moveToFront(node), return node.value                   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ PUT ALGORITHM (5 steps)                                      │
 * │                                                             │
 * │   1. map.get(key) → existing?                               │
 * │   2. If exists: update value, moveToFront → DONE            │
 * │   3. If new and at capacity: evict()                        │
 * │   4. Create new Node(key, value)                            │
 * │   5. addToFront(node), map.put(key, node)                   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ KEY INSIGHTS                                                 │
 * │                                                             │
 * │   "Node stores BOTH key AND value because when evicting     │
 * │    the tail, I need the key to remove it from the HashMap." │
 * │                                                             │
 * │   "Dummy head/tail nodes eliminate ALL null checks —         │
 * │    every real node always has valid prev and next pointers." │
 * │                                                             │
 * │   "Neither HashMap nor DLL alone achieves O(1) for all ops. │
 * │    Together: HashMap → O(1) lookup, DLL → O(1) reorder."   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ COMPLEXITY                                                   │
 * │                                                             │
 * │   Time:  O(1) for both get and put                          │
 * │   Space: O(capacity) for HashMap + DLL nodes                │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ COMMON MISTAKES TO AVOID                                     │
 * │                                                             │
 * │   ✗ Forgetting to move to front on UPDATE (put existing key)│
 * │   ✗ Forgetting to remove from MAP on eviction               │
 * │   ✗ Not storing key in Node (can't evict from map)          │
 * │   ✗ Wrong pointer order in addToFront (overwrite before read)│
 * │   ✗ Using SLL instead of DLL (O(n) removal)                │
 * │   ✗ Not using sentinel nodes (endless null checks)          │
 * └─────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │ EXTENSION ANSWERS (one-liners)                               │
 * │                                                             │
 * │   Thread-safe: ReentrantReadWriteLock or ConcurrentHashMap  │
 * │   TTL: store timestamp in node, check on get, bg cleanup    │
 * │   LFU: frequency buckets, each with its own DLL             │
 * │   Distributed: Redis with allkeys-lru, consistent hashing   │
 * └─────────────────────────────────────────────────────────────┘
 */
public class P7_QuickReference {
    // Quick reference card — review before the interview.
}

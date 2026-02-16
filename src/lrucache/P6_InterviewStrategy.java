package lrucache;

/**
 * ============================================================
 * PART 6: INTERVIEW WALKTHROUGH STRATEGY
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * TIMELINE FOR A 45-MINUTE INTERVIEW
 * -------------------------------------------------------
 *
 * MINUTES 0-3: CLARIFY THE PROBLEM
 *
 *   Ask:
 *   - "Are keys and values integers, or could they be generic types?"
 *   - "Does get() mark the item as recently used?" (YES — confirm this)
 *   - "Should put() on an existing key update the value?" (YES)
 *   - "Do I need thread safety?" (Usually no for coding rounds)
 *   - "What's the expected capacity range?" (Validate your approach)
 *
 *   Don't skip this. Interviewers penalize you for assuming.
 *
 *
 * MINUTES 3-8: EXPLAIN THE APPROACH
 *
 *   Say: "I need O(1) for both get and put. A HashMap alone can't
 *   track recency. A linked list alone can't do O(1) lookup.
 *   Together, a HashMap maps keys to DLL nodes, giving O(1) lookup
 *   AND O(1) reordering."
 *
 *   Draw on the whiteboard:
 *     HEAD ↔ [MRU] ↔ [middle] ↔ [LRU] ↔ TAIL
 *     HashMap: {key1 → node1, key2 → node2, ...}
 *
 *   Walk through 2-3 operations verbally.
 *
 *
 * MINUTES 8-12: DESIGN API AND HELPERS
 *
 *   Write method signatures:
 *     - Node class with key, value, prev, next
 *     - addToFront(node)
 *     - removeNode(node)
 *     - moveToFront(node)
 *     - evict()
 *     - get(key) → int
 *     - put(key, value) → void
 *
 *   Explain each briefly. This shows decomposition skill.
 *
 *
 * MINUTES 12-35: IMPLEMENT
 *
 *   Code in this order:
 *     1. Node class
 *     2. Instance variables and constructor
 *     3. addToFront (draw the pointers)
 *     4. removeNode
 *     5. moveToFront (one-liner: remove + add)
 *     6. evict (remove from list AND map)
 *     7. get
 *     8. put
 *
 *   Talk through each line. Don't code silently.
 *
 *
 * MINUTES 35-40: TEST
 *
 *   Trace through a scenario on the whiteboard:
 *     Cache capacity = 2
 *     put(1,1) → HEAD ↔ [1:1] ↔ TAIL
 *     put(2,2) → HEAD ↔ [2:2] ↔ [1:1] ↔ TAIL
 *     get(1)   → HEAD ↔ [1:1] ↔ [2:2] ↔ TAIL, returns 1
 *     put(3,3) → evicts 2. HEAD ↔ [3:3] ↔ [1:1] ↔ TAIL
 *     get(2)   → returns -1 ✓
 *
 *   Mention edge cases: capacity=1, update existing, get non-existent.
 *
 *
 * MINUTES 40-45: COMPLEXITY + EXTENSIONS
 *
 *   Complexity:
 *     Time:  O(1) for both get and put.
 *     Space: O(capacity) for the map + linked list.
 *
 *   Then discuss extensions (see below).
 *
 *
 * -------------------------------------------------------
 * EXTENSION ANSWERS (2-3 sentences each)
 * -------------------------------------------------------
 *
 * Q: "How would you make this thread-safe?"
 *
 * A: "I'd use a ReentrantReadWriteLock. Reads (get) acquire a read lock
 *    — multiple threads can read simultaneously. Writes (put, evict)
 *    acquire a write lock — exclusive access. Alternatively,
 *    ConcurrentHashMap + synchronized blocks on the linked list, though
 *    that's trickier to get right."
 *
 *
 * Q: "How would you add TTL (time-to-live) expiration?"
 *
 * A: "I'd store a timestamp in each node (createdAt or expiresAt). On get(),
 *    check if the node is expired — if so, remove it and return -1. For
 *    proactive cleanup, I'd run a background thread that periodically walks
 *    the list from the tail (oldest items) and removes expired nodes.
 *    Alternatively, use a separate min-heap ordered by expiration time."
 *
 *
 * Q: "How would you implement LFU (Least Frequently Used) instead?"
 *
 * A: "LFU needs to track access frequency for each key. I'd use a HashMap
 *    for key→node lookup, and a TreeMap or bucket list where each bucket
 *    holds keys with the same frequency. On access, increment the node's
 *    frequency and move it to the next bucket. On eviction, remove from
 *    the lowest-frequency bucket. The tricky part is tie-breaking within
 *    the same frequency (use insertion order — another DLL per bucket)."
 *
 *
 * Q: "What if this needs to be distributed?"
 *
 * A: "I'd use a distributed cache like Redis, which has built-in LRU
 *    eviction (maxmemory-policy = allkeys-lru). For custom implementations,
 *    consistent hashing distributes keys across cache nodes. Each node
 *    runs its own local LRU. For coordination, use a gossip protocol
 *    or centralized config service (ZooKeeper/etcd)."
 */
public class P6_InterviewStrategy {
    // Reference document — read this the night before your interview.
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP — DISCUSSION (Minutes 40-45)                   │
 * │                                                             │
 * │ The interviewer evaluates:                                   │
 * │                                                             │
 * │ 1. Did you explain the O(1) complexity convincingly?         │
 * │    → "Every step is a HashMap op or pointer update. No      │
 * │    loops, no traversals. Each operation touches at most a   │
 * │    constant number of pointers."                            │
 * │                                                             │
 * │ 2. Did you mention trade-offs?                               │
 * │    → "The space overhead is one extra prev/next pointer per │
 * │    node, plus the HashMap storing key→node. For typical     │
 * │    cache sizes, this is negligible."                        │
 * │                                                             │
 * │ 3. Can you think beyond the basic problem?                   │
 * │    → Thread safety, TTL, distribution. Even brief answers   │
 * │    show senior-level thinking.                              │
 * └─────────────────────────────────────────────────────────────┘
 */

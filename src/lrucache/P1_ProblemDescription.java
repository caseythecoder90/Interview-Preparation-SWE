package lrucache;

/**
 * ============================================================
 * PART 1: LRU CACHE — PROBLEM DESCRIPTION
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * 1.1  WHAT IS AN LRU CACHE?
 * -------------------------------------------------------
 *
 * LRU = Least Recently Used.
 *
 * A CACHE is a fast-access storage layer that holds a subset of data.
 * When the cache is full and a new item needs to be stored, something
 * must be EVICTED. LRU evicts the item that hasn't been used for the
 * longest time.
 *
 * Real-world analogies:
 *
 *   PHONE'S RECENT APPS:
 *     Your phone shows 5 recent apps. When you open a new app and the
 *     list is full, the app you haven't touched in the longest time
 *     falls off the list. If you switch back to an old app, it moves
 *     to the front.
 *
 *   BROWSER'S BACK BUTTON HISTORY:
 *     The browser keeps the N most recently visited pages. The page
 *     you visited longest ago is dropped first.
 *
 *   DNS CACHE:
 *     Your computer caches recent DNS lookups. When full, the domain
 *     you haven't looked up in the longest time gets evicted.
 *
 * WHY LRU over other eviction strategies?
 *
 *   FIFO (First In, First Out):
 *     Evicts the oldest item regardless of how often it's accessed.
 *     Bad for hot items that were added early but are still popular.
 *
 *   LFU (Least Frequently Used):
 *     Evicts the least-frequently-accessed item. Problem: items that
 *     were popular in the past but aren't anymore stay cached (frequency
 *     doesn't decay). Also harder to implement (need per-item counters).
 *
 *   Random:
 *     Might evict a hot item. Unpredictable behavior.
 *
 *   LRU — the sweet spot:
 *     Simple, effective, approximates "keep what's likely needed next."
 *     Based on temporal locality: recently accessed items are likely
 *     to be accessed again soon.
 *
 *
 * -------------------------------------------------------
 * 1.2  THE PROBLEM STATEMENT (LeetCode 146)
 * -------------------------------------------------------
 *
 * Design a data structure that supports:
 *
 *   get(int key):
 *     → Return the VALUE if key exists, otherwise return -1.
 *     → Mark this key as MOST RECENTLY USED.
 *
 *   put(int key, int value):
 *     → Insert or update the key-value pair.
 *     → Mark this key as MOST RECENTLY USED.
 *     → If at capacity, EVICT the LEAST recently used item first.
 *
 *   Both operations must be O(1) time.
 *   The cache has a fixed capacity set at construction.
 *
 *
 * -------------------------------------------------------
 * 1.3  WHY O(1) IS THE HARD PART
 * -------------------------------------------------------
 *
 * Let's try each data structure alone and see why it fails:
 *
 *   HASHMAP ALONE:
 *     ✅ O(1) get by key
 *     ✅ O(1) put
 *     ❌ No ordering — can't find "least recently used" without scanning ALL entries
 *     Finding the LRU item would be O(n). FAILS.
 *
 *   LINKED LIST ALONE:
 *     ✅ O(1) insert at front (mark as most recent)
 *     ✅ O(1) remove from tail (evict LRU)
 *     ❌ O(n) lookup — must traverse to find a key
 *     get() would be O(n). FAILS.
 *
 *   ARRAY ALONE:
 *     ✅ O(1) access by index
 *     ❌ O(n) removal from middle (shift elements)
 *     ❌ No key-based lookup without scanning
 *     Both get() and put() would be O(n). FAILS.
 *
 *   LINKEDHASHMAP (Java built-in):
 *     ✅ Actually works! Java's LinkedHashMap with accessOrder=true IS an LRU cache.
 *     But interviewers want you to BUILD it from scratch. Don't use this.
 *
 *   HASHMAP + DOUBLY LINKED LIST — THE ANSWER:
 *     ✅ HashMap: O(1) key → node lookup
 *     ✅ DLL: O(1) insert at front, O(1) remove any node (given reference),
 *            O(1) remove from tail
 *     ✅ Together: every LRU operation is O(1)
 *
 *   THIS is the insight interviewers want you to arrive at.
 *
 *
 * -------------------------------------------------------
 * 1.4  CONSTRAINTS
 * -------------------------------------------------------
 *
 *   1 <= capacity <= 3000
 *   0 <= key <= 10^4
 *   0 <= value <= 10^5
 *   At most 2 * 10^5 calls to get and put
 *
 *   These constraints tell us:
 *     - Capacity can be as small as 1 (edge case!)
 *     - Keys and values are non-negative integers
 *     - Must handle up to 200,000 operations efficiently → O(1) is essential
 */
public class P1_ProblemDescription {
    // Reference document — no executable code.
    // Read this first, then move to P2_DataStructureDeepDive.
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP — PROBLEM CLARIFICATION (Minutes 0-3)          │
 * │                                                             │
 * │ Questions to ask the interviewer:                            │
 * │                                                             │
 * │ 1. "Are the keys and values always integers?" (Usually yes  │
 * │    for LeetCode, but Fullstory may use String keys.)        │
 * │                                                             │
 * │ 2. "Does get() update recency?" (YES — this is critical.   │
 * │    Accessing an item makes it most recently used.)          │
 * │                                                             │
 * │ 3. "Should put() on an existing key update the value?"      │
 * │    (YES — and it should also update recency.)               │
 * │                                                             │
 * │ 4. "Does this need to be thread-safe?" (Usually no for      │
 * │    coding rounds, but mention it shows awareness.)          │
 * │                                                             │
 * │ 5. "What should happen if capacity is 0?" (Clarify — it's   │
 * │    usually ≥ 1 per constraints.)                            │
 * └─────────────────────────────────────────────────────────────┘
 */

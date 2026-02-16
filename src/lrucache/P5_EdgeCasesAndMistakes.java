package lrucache;

/**
 * ============================================================
 * PART 5: EDGE CASES & COMMON MISTAKES
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * EDGE CASE 1: UPDATING AN EXISTING KEY
 * -------------------------------------------------------
 *
 * put(key, newValue) when key already exists.
 *
 * CORRECT behavior:
 *   1. Update the node's value to newValue
 *   2. Move the node to the front (mark as MRU)
 *   3. Do NOT create a new node or change the map size
 *
 * COMMON BUG: forgetting to move to front after update.
 *   If you only update the value without moving, the node stays
 *   at its old position in the recency list. It might get evicted
 *   even though it was just accessed via put().
 *
 *   Example of the bug:
 *     put(1, 1)  →  [1:1]
 *     put(2, 2)  →  [2:2] ↔ [1:1]
 *     put(1, 10) →  [2:2] ↔ [1:10]  ← BUG: 1 is still at back!
 *     put(3, 3)  →  evicts key 1    ← WRONG: 1 was just updated!
 *
 *   Correct: put(1, 10) should move [1:10] to front.
 *
 *
 * -------------------------------------------------------
 * EDGE CASE 2: GET ON NON-EXISTENT KEY
 * -------------------------------------------------------
 *
 * get(key) when key is not in the cache.
 *
 * CORRECT: return -1.
 * MISTAKE: throwing an exception or returning 0 or null.
 *
 * This is simple but make sure you check map.get(key) == null first.
 *
 *
 * -------------------------------------------------------
 * EDGE CASE 3: CAPACITY OF 1
 * -------------------------------------------------------
 *
 * Every put() of a new key evicts the previous entry.
 *
 *   put(1, 1): cache = [1]
 *   put(2, 2): evicts 1, cache = [2]
 *   get(1): -1
 *   put(2, 20): update, no eviction, cache = [2:20]
 *   get(2): 20
 *
 * Your code must handle this — especially the sentinel pattern.
 * With capacity=1 and one node: HEAD ↔ [node] ↔ TAIL.
 * On eviction: HEAD ↔ TAIL (back to empty). Must work correctly.
 *
 *
 * -------------------------------------------------------
 * EDGE CASE 4: PUT THEN IMMEDIATE GET
 * -------------------------------------------------------
 *
 *   put(5, 500)
 *   get(5) → should return 500 (not -1)
 *
 * This seems obvious but catches implementation bugs where
 * the node isn't properly added to the map.
 *
 *
 * -------------------------------------------------------
 * COMMON MISTAKE 5: NOT REMOVING FROM MAP ON EVICTION
 * -------------------------------------------------------
 *
 * When evicting the LRU node, you MUST remove it from BOTH:
 *   1. The doubly linked list (removeNode)
 *   2. The HashMap (map.remove(node.key))
 *
 * If you forget step 2:
 *   - The map still has a reference to the evicted node
 *   - get(evictedKey) finds the stale node and returns an old value
 *   - The map size grows beyond capacity (memory leak)
 *   - Wrong answers AND wasted memory
 *
 * This is one of the most common bugs. In an interview, explicitly
 * say: "I need to remove from both the list AND the map."
 *
 *
 * -------------------------------------------------------
 * COMMON MISTAKE 6: NOT STORING KEY IN NODE
 * -------------------------------------------------------
 *
 * If the Node only stores value (not key):
 *
 *   void evict() {
 *       Node lru = tail.prev;
 *       removeNode(lru);
 *       map.remove(???);  // HOW DO WE GET THE KEY?
 *   }
 *
 * Without the key stored in the node, you'd need to scan the
 * entire HashMap to find which entry points to this node → O(n).
 * That defeats the whole purpose of O(1) operations.
 *
 * Always store the key in the node. Interviewers specifically
 * look for this — explain WHY when you write the Node class.
 *
 *
 * -------------------------------------------------------
 * COMMON MISTAKE 7: POINTER ORDER IN LINKED LIST OPERATIONS
 * -------------------------------------------------------
 *
 * In addToFront, the 4 pointer updates must be in the right order:
 *
 *   CORRECT ORDER:
 *     1. node.prev = head
 *     2. node.next = head.next      // uses OLD head.next
 *     3. head.next.prev = node      // uses OLD head.next
 *     4. head.next = node           // NOW overwrite head.next
 *
 *   WRONG (if you do step 4 before step 2 or 3):
 *     1. node.prev = head
 *     4. head.next = node           // head.next now points to node
 *     2. node.next = head.next      // BUG: head.next is now node itself!
 *     3. head.next.prev = node      // BUG: setting node.prev = node
 *
 *   Result: node points to itself → infinite loop, corrupted list.
 *
 * RULE: update the OLD neighbor's pointers BEFORE overwriting
 *       any pointer that you still need to read.
 *
 * In removeNode, order doesn't matter (only 2 updates, no overlap).
 */
public class P5_EdgeCasesAndMistakes {
    // Reference document — study these before your interview.
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP — TESTING (Minutes 35-40)                      │
 * │                                                             │
 * │ Walk through a scenario tracing the linked list state:       │
 * │                                                             │
 * │ "Let me trace through capacity=2:                            │
 * │  put(1,1):  HEAD ↔ [1:1] ↔ TAIL.   Map: {1}               │
 * │  put(2,2):  HEAD ↔ [2:2] ↔ [1:1] ↔ TAIL.   Map: {1,2}    │
 * │  get(1):    HEAD ↔ [1:1] ↔ [2:2] ↔ TAIL.   (moved to front)│
 * │  put(3,3):  evicts key 2 (LRU).                            │
 * │             HEAD ↔ [3:3] ↔ [1:1] ↔ TAIL.   Map: {1,3}     │
 * │  get(2):    returns -1 ✓"                                   │
 * │                                                             │
 * │ This takes 2-3 minutes and proves your code works.          │
 * │ Draw the DLL state after each operation.                    │
 * └─────────────────────────────────────────────────────────────┘
 */

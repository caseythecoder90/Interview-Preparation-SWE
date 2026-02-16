package lrucache;

/**
 * ============================================================
 * PART 3: SOLUTION DESIGN — HELPER METHODS
 * ============================================================
 *
 * Before writing the full solution, decompose into small, testable pieces.
 * This is how an interviewer wants to see you think.
 *
 * Four helper methods:
 *   1. addToFront(Node node)  — insert node as MRU
 *   2. removeNode(Node node)  — unlink node from list
 *   3. moveToFront(Node node) — mark node as MRU (remove + add)
 *   4. evict()                — remove LRU node from both list and map
 *
 *
 * -------------------------------------------------------
 * HELPER 1: addToFront(Node node)
 * -------------------------------------------------------
 *
 * Purpose: insert a node right after the HEAD dummy, making it the MRU.
 *
 * Before:  HEAD ↔ [A] ↔ [B] ↔ TAIL
 *
 * After:   HEAD ↔ [NEW] ↔ [A] ↔ [B] ↔ TAIL
 *
 * Four pointer updates (ORDER MATTERS):
 *   1. node.prev = head           // new node's prev → head
 *   2. node.next = head.next      // new node's next → old first node [A]
 *   3. head.next.prev = node      // old first node's prev → new node
 *   4. head.next = node           // head's next → new node
 *
 * Why order matters:
 *   Step 3 uses head.next to find [A]. If step 4 runs first,
 *   head.next would already be overwritten to point to [NEW],
 *   and step 3 would incorrectly set NEW.prev = node (itself).
 *   ALWAYS update the OLD neighbors' pointers BEFORE overwriting head.next.
 *
 *   void addToFront(Node node) {
 *       node.prev = head;
 *       node.next = head.next;
 *       head.next.prev = node;
 *       head.next = node;
 *   }
 *
 *
 * -------------------------------------------------------
 * HELPER 2: removeNode(Node node)
 * -------------------------------------------------------
 *
 * Purpose: unlink a node from wherever it sits in the list.
 *
 * Before:  ... ↔ [A] ↔ [NODE] ↔ [B] ↔ ...
 *
 * After:   ... ↔ [A] ↔ [B] ↔ ...    (NODE is detached)
 *
 * Two pointer updates:
 *   1. node.prev.next = node.next   // A now points forward to B
 *   2. node.next.prev = node.prev   // B now points backward to A
 *
 * This works for ANY position because of dummy nodes:
 *   - Removing the first real node? node.prev = HEAD (valid, not null)
 *   - Removing the last real node? node.next = TAIL (valid, not null)
 *   - No special cases needed!
 *
 *   void removeNode(Node node) {
 *       node.prev.next = node.next;
 *       node.next.prev = node.prev;
 *   }
 *
 *
 * -------------------------------------------------------
 * HELPER 3: moveToFront(Node node)
 * -------------------------------------------------------
 *
 * Purpose: mark a node as most recently used by moving it to the front.
 *
 * Simply calls removeNode(node) then addToFront(node).
 *
 * Why decompose into two calls?
 *   - Each method is small and correct on its own
 *   - Less error-prone than trying to do both operations inline
 *   - Easier to test independently
 *   - In an interview, shows clean design thinking
 *
 *   void moveToFront(Node node) {
 *       removeNode(node);
 *       addToFront(node);
 *   }
 *
 *
 * -------------------------------------------------------
 * HELPER 4: evict()
 * -------------------------------------------------------
 *
 * Purpose: remove the LRU node (right before TAIL dummy) from
 *          BOTH the linked list AND the HashMap.
 *
 *   DLL before:  HEAD ↔ [C] ↔ [B] ↔ [A] ↔ TAIL
 *                                    ^LRU
 *   DLL after:   HEAD ↔ [C] ↔ [B] ↔ TAIL
 *
 *   Map before:  {A→nodeA, B→nodeB, C→nodeC}
 *   Map after:   {B→nodeB, C→nodeC}
 *
 * Steps:
 *   1. Get the LRU node: Node lru = tail.prev
 *   2. Remove it from the DLL: removeNode(lru)
 *   3. Remove it from the HashMap: map.remove(lru.key)
 *      ← THIS is why the Node stores the key!
 *
 *   void evict() {
 *       Node lru = tail.prev;
 *       removeNode(lru);
 *       map.remove(lru.key);
 *   }
 */
public class P3_HelperMethods {
    // Reference document — study the pointer diagrams above.
    // The full implementation is in P4_LRUCache.java.
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP — DESIGNING HELPERS (Minutes 8-12)             │
 * │                                                             │
 * │ Before writing any code, tell the interviewer:               │
 * │                                                             │
 * │ "Let me decompose this into helper methods first:            │
 * │  1. addToFront — insert a node as MRU (4 pointer updates)   │
 * │  2. removeNode — unlink a node from the list (2 pointers)   │
 * │  3. moveToFront — remove then add (combines 1 and 2)        │
 * │  4. evict — remove LRU from list AND map                    │
 * │                                                             │
 * │ Then get() is: find in map, moveToFront, return value.      │
 * │ And put() is: if exists → update + moveToFront;             │
 * │               if new → check capacity (evict?), create,     │
 * │               addToFront, add to map."                      │
 * │                                                             │
 * │ This shows the interviewer your DECOMPOSITION skill.         │
 * │ They care as much about HOW you design as about correctness. │
 * └─────────────────────────────────────────────────────────────┘
 */

package lrucache;

/**
 * ============================================================
 * PART 2: DATA STRUCTURE DEEP DIVE
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * 2A: HASHMAP — O(1) key lookup
 * -------------------------------------------------------
 *
 * The HashMap stores:  key → Node  (NOT key → value!)
 *
 * WHY key → Node (not key → value)?
 *   When we call get(key), we need to:
 *     1. Find the node in O(1) — HashMap gives us this
 *     2. Move that node to the front of the DLL — we need the NODE REFERENCE
 *   If the map stored key → value, we'd have the value but no way to find
 *   the node in the list without O(n) traversal. Storing the NODE directly
 *   gives us a handle into the linked list.
 *
 *
 * -------------------------------------------------------
 * 2B: DOUBLY LINKED LIST — O(1) ordering
 * -------------------------------------------------------
 *
 * WHY DOUBLY LINKED (not singly)?
 *   To remove a node from the middle of a list in O(1), you need to update
 *   BOTH the previous node's `next` pointer and the next node's `prev` pointer.
 *
 *   Singly linked: to remove node X, you need X's predecessor.
 *     Finding the predecessor requires traversal → O(n).
 *
 *   Doubly linked: node X has X.prev and X.next.
 *     X.prev.next = X.next  and  X.next.prev = X.prev → O(1).
 *
 *   THIS is why DLL, not SLL. It's the single most important insight.
 *
 *
 * SENTINEL / DUMMY NODES:
 *
 *   We use two dummy nodes: HEAD and TAIL that never hold real data.
 *
 *     Empty cache:
 *       HEAD ↔ TAIL
 *
 *     After adding nodes:
 *       HEAD ↔ [most recent] ↔ [middle] ↔ [least recent] ↔ TAIL
 *
 *   WHY dummy nodes?
 *     They eliminate ALL null checks. Without them, every insert and remove
 *     needs special cases: "if head is null...", "if removing the head...",
 *     "if removing the tail...", etc. With sentinels, every real node always
 *     has a valid prev and next pointer. This cuts edge cases in half.
 *
 *   HEAD.next = most recently used node (or TAIL if empty)
 *   TAIL.prev = least recently used node (or HEAD if empty)
 *
 *
 * NODE CLASS DESIGN:
 *
 *   class Node {
 *       int key;    ← MUST store the key!
 *       int value;
 *       Node prev;
 *       Node next;
 *   }
 *
 *   WHY does the Node store the KEY (not just the value)?
 *     When we evict the LRU node (tail.prev), we need to ALSO remove it
 *     from the HashMap. To remove from the HashMap, we need the key.
 *     If the node didn't store its key, we'd have to scan the entire
 *     HashMap to find which entry points to this node → O(n).
 *     Storing the key in the node makes eviction O(1).
 *
 *
 * -------------------------------------------------------
 * 2C: HOW THEY WORK TOGETHER — Visual Walkthrough
 * -------------------------------------------------------
 *
 * Capacity = 3.  MRU = Most Recently Used, LRU = Least Recently Used.
 *
 *
 * STEP 1: put(1, 1)
 *
 *   DLL:  HEAD ↔ [1:1] ↔ TAIL
 *                 ^MRU   ^LRU (same node — only one)
 *
 *   Map:  {1 → Node(1,1)}
 *
 *   Action: create node, addToFront, put in map. Size: 1/3.
 *
 *
 * STEP 2: put(2, 2)
 *
 *   DLL:  HEAD ↔ [2:2] ↔ [1:1] ↔ TAIL
 *                 ^MRU            ^LRU
 *
 *   Map:  {1 → Node(1,1), 2 → Node(2,2)}
 *
 *   Action: create node, addToFront, put in map. Size: 2/3.
 *
 *
 * STEP 3: put(3, 3)
 *
 *   DLL:  HEAD ↔ [3:3] ↔ [2:2] ↔ [1:1] ↔ TAIL
 *                 ^MRU                     ^LRU
 *
 *   Map:  {1 → Node(1,1), 2 → Node(2,2), 3 → Node(3,3)}
 *
 *   Action: create node, addToFront, put in map. Size: 3/3 (FULL).
 *
 *
 * STEP 4: get(1)  → returns 1
 *
 *   Before: HEAD ↔ [3:3] ↔ [2:2] ↔ [1:1] ↔ TAIL
 *   After:  HEAD ↔ [1:1] ↔ [3:3] ↔ [2:2] ↔ TAIL
 *                  ^MRU                     ^LRU
 *
 *   Map: unchanged (same nodes, just relinked)
 *
 *   Action: find node in map, moveToFront (removeNode + addToFront).
 *   Node [1:1] was near the tail (LRU). Now it's at the front (MRU).
 *   Node [2:2] is now the LRU.
 *
 *
 * STEP 5: put(4, 4)  → EVICTION happens!
 *
 *   Cache is full (3/3). Must evict LRU before inserting.
 *   LRU = TAIL.prev = [2:2].
 *
 *   Eviction: removeNode([2:2]) from DLL, remove key 2 from map.
 *   Then: create Node(4,4), addToFront, put in map.
 *
 *   Before evict: HEAD ↔ [1:1] ↔ [3:3] ↔ [2:2] ↔ TAIL
 *   After evict:  HEAD ↔ [1:1] ↔ [3:3] ↔ TAIL
 *   After insert:  HEAD ↔ [4:4] ↔ [1:1] ↔ [3:3] ↔ TAIL
 *                         ^MRU                     ^LRU
 *
 *   Map:  {1 → Node(1,1), 3 → Node(3,3), 4 → Node(4,4)}
 *   (key 2 is GONE)
 *
 *
 * STEP 6: get(2)  → returns -1
 *
 *   Key 2 was evicted in step 5. Map doesn't contain it. Return -1.
 *   DLL and Map: unchanged.
 *
 *
 * STEP 7: put(3, 30)  → UPDATE existing key
 *
 *   Key 3 already exists. Update its value from 3 to 30 and move to front.
 *
 *   Before: HEAD ↔ [4:4] ↔ [1:1] ↔ [3:3] ↔ TAIL
 *   After:  HEAD ↔ [3:30] ↔ [4:4] ↔ [1:1] ↔ TAIL
 *                  ^MRU                     ^LRU
 *
 *   Map:  {1 → Node(1,1), 3 → Node(3,30), 4 → Node(4,4)}
 *
 *   Action: find existing node in map, update value, moveToFront.
 *   NO eviction needed — we're updating, not adding a new key.
 */
public class P2_DataStructureDeepDive {
    // Reference document — no executable code.
    // Read this to understand the WHY behind every design choice.
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP — EXPLAINING THE APPROACH (Minutes 3-8)        │
 * │                                                             │
 * │ Say exactly this:                                           │
 * │                                                             │
 * │ "I'll use a HashMap and a Doubly Linked List together.      │
 * │  The HashMap maps each key to its node in the linked list,  │
 * │  giving O(1) lookup. The doubly linked list maintains       │
 * │  recency order — most recent at the front, least recent     │
 * │  at the back — with O(1) insert and remove. I'll use        │
 * │  dummy head and tail nodes to eliminate null-check edge      │
 * │  cases. Each node stores BOTH key and value, because when   │
 * │  I evict the tail node, I need its key to also remove it    │
 * │  from the HashMap."                                         │
 * │                                                             │
 * │ Then draw the empty state:  HEAD ↔ TAIL                     │
 * │ And walk through 2-3 operations on the whiteboard.          │
 * │ This takes 3-5 minutes and shows mastery.                   │
 * └─────────────────────────────────────────────────────────────┘
 */

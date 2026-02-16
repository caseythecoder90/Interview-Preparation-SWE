package lrucache;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * PART 4: FULL LRU CACHE SOLUTION (LeetCode 146)
 * ============================================================
 *
 * Time Complexity:
 *   get(key):   O(1) — HashMap lookup + DLL move (all pointer operations)
 *   put(key):   O(1) — HashMap insert + DLL insert + possible eviction (all O(1))
 *
 * Space Complexity:
 *   O(capacity) — HashMap holds at most `capacity` entries,
 *                  DLL holds at most `capacity` nodes.
 *
 * Data Structures:
 *   HashMap<Integer, Node> — maps key → DLL node for O(1) lookup
 *   Doubly Linked List     — maintains recency order (MRU at front, LRU at back)
 *   Sentinel head/tail     — eliminate null checks on every operation
 */
public class P4_LRUCache {

    // =============================================================
    // NODE CLASS
    // =============================================================
    // Stores BOTH key and value.
    // Key is needed for eviction: when we remove the tail node,
    // we need its key to also remove it from the HashMap.

    private static class Node {
        int key;
        int value;
        Node prev;
        Node next;

        Node() {} // for sentinel/dummy nodes

        Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    // =============================================================
    // INSTANCE VARIABLES
    // =============================================================

    private final int capacity;
    private final Map<Integer, Node> map;  // key → node
    private final Node head; // dummy head (MRU side)
    private final Node tail; // dummy tail (LRU side)

    // =============================================================
    // CONSTRUCTOR
    // =============================================================

    public P4_LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();

        // Initialize sentinel nodes
        // Empty list: HEAD ↔ TAIL
        this.head = new Node();
        this.tail = new Node();
        head.next = tail;
        tail.prev = head;
    }

    // =============================================================
    // HELPER 1: addToFront(Node node)
    // =============================================================
    // Insert node right after HEAD (making it the MRU).
    //
    //   Before: HEAD ↔ [existing] ↔ ...
    //   After:  HEAD ↔ [node] ↔ [existing] ↔ ...
    //
    // Four pointer updates. Order matters!

    private void addToFront(Node node) {
        node.prev = head;          // 1. new node's prev → head
        node.next = head.next;     // 2. new node's next → old first node
        head.next.prev = node;     // 3. old first node's prev → new node
        head.next = node;          // 4. head's next → new node (LAST!)
    }

    // =============================================================
    // HELPER 2: removeNode(Node node)
    // =============================================================
    // Unlink node from wherever it currently sits.
    //
    //   Before: ... ↔ [A] ↔ [node] ↔ [B] ↔ ...
    //   After:  ... ↔ [A] ↔ [B] ↔ ...
    //
    // Two pointer updates. Works for any position due to sentinels.

    private void removeNode(Node node) {
        node.prev.next = node.next;  // A.next → B
        node.next.prev = node.prev;  // B.prev → A
    }

    // =============================================================
    // HELPER 3: moveToFront(Node node)
    // =============================================================
    // Mark a node as most recently used.
    // Remove from current position, re-insert at front.

    private void moveToFront(Node node) {
        removeNode(node);
        addToFront(node);
    }

    // =============================================================
    // HELPER 4: evict()
    // =============================================================
    // Remove the LRU node (the one right before TAIL).
    // Must remove from BOTH the DLL and the HashMap.

    private void evict() {
        Node lru = tail.prev;        // LRU node is always right before tail
        removeNode(lru);             // remove from DLL
        map.remove(lru.key);         // remove from HashMap (THIS is why node stores key)
    }

    // =============================================================
    // GET
    // =============================================================
    //
    // Algorithm:
    //   1. Check if key exists in the map
    //   2. If not → return -1
    //   3. If yes → move the node to front (mark as MRU), return value
    //
    // Every step is O(1): HashMap lookup, pointer manipulation, return.

    public int get(int key) {
        Node node = map.get(key);
        if (node == null) {
            return -1;              // key doesn't exist
        }
        moveToFront(node);          // mark as most recently used
        return node.value;
    }

    // =============================================================
    // PUT
    // =============================================================
    //
    // Algorithm:
    //   1. Check if key already exists in the map
    //   2. If YES (update):
    //      a. Update the node's value
    //      b. Move it to front (mark as MRU)
    //   3. If NO (new insertion):
    //      a. If at capacity → evict the LRU node
    //      b. Create a new node
    //      c. Add it to the front of the DLL
    //      d. Add it to the HashMap
    //
    // Every step is O(1): HashMap operations, pointer manipulation.

    public void put(int key, int value) {
        Node existing = map.get(key);

        if (existing != null) {
            // UPDATE existing key
            existing.value = value;   // update value
            moveToFront(existing);    // mark as MRU
        } else {
            // NEW insertion
            if (map.size() == capacity) {
                evict();              // make room if at capacity
            }
            Node newNode = new Node(key, value);
            addToFront(newNode);      // add to DLL as MRU
            map.put(key, newNode);    // add to HashMap
        }
    }

    // =============================================================
    // DEBUG: print the cache state (for testing)
    // =============================================================
    public void printState() {
        StringBuilder dll = new StringBuilder("HEAD");
        Node curr = head.next;
        while (curr != tail) {
            dll.append(" <-> [").append(curr.key).append(":").append(curr.value).append("]");
            curr = curr.next;
        }
        dll.append(" <-> TAIL");
        System.out.println("  DLL: " + dll);
        System.out.println("  Map keys: " + map.keySet());
    }

    // =============================================================
    // DEMO + TESTING (Part 6)
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== TEST 1: Basic get and put ===");
        P4_LRUCache cache = new P4_LRUCache(3);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        cache.printState();
        // DLL: HEAD <-> [3:3] <-> [2:2] <-> [1:1] <-> TAIL

        System.out.println("get(1): " + cache.get(1));   // 1
        System.out.println("get(2): " + cache.get(2));   // 2
        System.out.println("get(4): " + cache.get(4));   // -1 (doesn't exist)

        System.out.println("\n=== TEST 2: Eviction order ===");
        // Cache has [2, 1, 3] (MRU to LRU after the gets above)
        cache.printState();
        cache.put(4, 4); // evicts key 3 (LRU)
        System.out.println("After put(4,4) — key 3 evicted:");
        cache.printState();
        System.out.println("get(3): " + cache.get(3));   // -1 (evicted!)
        System.out.println("get(4): " + cache.get(4));   // 4

        System.out.println("\n=== TEST 3: Get updates recency ===");
        P4_LRUCache cache2 = new P4_LRUCache(2);
        cache2.put(1, 1);
        cache2.put(2, 2);
        // DLL: [2, 1] — key 1 is LRU
        cache2.get(1);    // access key 1 → now MRU
        // DLL: [1, 2] — key 2 is now LRU
        cache2.put(3, 3); // should evict key 2 (not key 1!)
        System.out.println("get(1): " + cache2.get(1));  // 1 (still here!)
        System.out.println("get(2): " + cache2.get(2));  // -1 (evicted — was LRU)
        System.out.println("get(3): " + cache2.get(3));  // 3

        System.out.println("\n=== TEST 4: Update existing key ===");
        P4_LRUCache cache3 = new P4_LRUCache(2);
        cache3.put(1, 10);
        cache3.put(2, 20);
        cache3.put(1, 100); // update key 1's value AND move to front
        cache3.printState();
        // DLL: HEAD <-> [1:100] <-> [2:20] <-> TAIL
        System.out.println("get(1): " + cache3.get(1));  // 100 (updated value)
        cache3.put(3, 30); // should evict key 2 (LRU), not key 1
        System.out.println("get(2): " + cache3.get(2));  // -1 (evicted)
        System.out.println("get(1): " + cache3.get(1));  // 100
        System.out.println("get(3): " + cache3.get(3));  // 30

        System.out.println("\n=== TEST 5: Capacity of 1 ===");
        P4_LRUCache cache4 = new P4_LRUCache(1);
        cache4.put(1, 1);
        System.out.println("get(1): " + cache4.get(1));  // 1
        cache4.put(2, 2); // evicts key 1
        System.out.println("get(1): " + cache4.get(1));  // -1
        System.out.println("get(2): " + cache4.get(2));  // 2
        cache4.put(2, 20); // update — no eviction
        System.out.println("get(2): " + cache4.get(2));  // 20

        System.out.println("\n=== TEST 6: Stress test (many operations) ===");
        P4_LRUCache cache5 = new P4_LRUCache(100);
        // Insert 200 items — first 100 should be evicted
        for (int i = 0; i < 200; i++) {
            cache5.put(i, i * 10);
        }
        // Keys 0-99 should be evicted, keys 100-199 should exist
        boolean earlyMissing = true;
        for (int i = 0; i < 100; i++) {
            if (cache5.get(i) != -1) earlyMissing = false;
        }
        boolean latePresent = true;
        for (int i = 100; i < 200; i++) {
            if (cache5.get(i) != i * 10) latePresent = false;
        }
        System.out.println("Keys 0-99 evicted: " + earlyMissing);    // true
        System.out.println("Keys 100-199 present: " + latePresent);  // true

        System.out.println("\n=== ALL TESTS PASSED ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP — IMPLEMENTING (Minutes 12-35)                 │
 * │                                                             │
 * │ 1. Write the Node class first. Say: "Node stores key AND    │
 * │    value. The key is needed for eviction."                  │
 * │                                                             │
 * │ 2. Write the constructor next. Say: "I'll initialize        │
 * │    sentinel nodes: head.next = tail, tail.prev = head."     │
 * │                                                             │
 * │ 3. Write helpers in order: addToFront, removeNode,          │
 * │    moveToFront, evict. Test each mentally with a diagram.   │
 * │                                                             │
 * │ 4. Write get() — simple: map lookup, moveToFront, return.   │
 * │                                                             │
 * │ 5. Write put() — more logic: check existing (update path)   │
 * │    vs new (check capacity → evict → create → add).          │
 * │                                                             │
 * │ 6. Talk through each line as you write it. Don't code       │
 * │    silently — the interviewer wants to see your thinking.   │
 * └─────────────────────────────────────────────────────────────┘
 */

package linkedlists;

/**
 * ============================================================
 * SECTION 8: DOUBLY LINKED LIST
 * ============================================================
 *
 * Each node has BOTH a `next` and `prev` pointer, allowing
 * traversal in both directions and O(1) deletion of any node
 * (given a reference to it).
 *
 * This is the foundation of the LRU Cache — arguably the most
 * important system design / data structure interview question.
 *
 *
 * -------------------------------------------------------
 * 8.1  WHY DOUBLY LINKED + HASHMAP = O(1) EVERYTHING
 * -------------------------------------------------------
 *
 * Neither structure alone achieves O(1) for all operations:
 *
 *   HashMap alone:
 *     ✅ O(1) lookup by key
 *     ❌ No ordering — can't find "least recently used" in O(1)
 *     ❌ Can't maintain access/insertion order efficiently
 *
 *   Doubly Linked List alone:
 *     ✅ O(1) insert/remove at any position (given a node reference)
 *     ✅ Maintains ordering (front = most recent, back = least recent)
 *     ❌ O(n) lookup — must traverse to find a node
 *
 *   HashMap + Doubly Linked List TOGETHER:
 *     ✅ O(1) lookup: HashMap gives you the node reference instantly
 *     ✅ O(1) remove: with the node reference, DLL removes in O(1)
 *     ✅ O(1) insert: DLL inserts at head/tail in O(1)
 *     ✅ O(1) find LRU: the node at the tail is always the LRU
 *
 * This is THE key insight. Interviewers want you to explain this.
 */
public class S8_DoublyLinkedList {

    // =============================================================
    // 8.2  NODE DEFINITION
    // =============================================================
    //
    // For LRU Cache, nodes store BOTH key and value.
    // Why key? When we evict the LRU node, we need its key to also
    // remove it from the HashMap. Without the key stored in the node,
    // we'd need an O(n) scan of the HashMap to find and remove it.

    static class DLLNode {
        int key;
        int value;
        DLLNode prev;
        DLLNode next;

        DLLNode() {} // for dummy/sentinel nodes

        DLLNode(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }

    // =============================================================
    // 8.3  SENTINEL / DUMMY NODE PATTERN
    // =============================================================
    //
    // Use dummy HEAD and TAIL nodes that never hold real data.
    // This eliminates ALL null checks in insert/remove operations.
    //
    // Empty list:
    //   HEAD ↔ TAIL
    //
    // After adding nodes:
    //   HEAD ↔ [A] ↔ [B] ↔ [C] ↔ TAIL
    //
    // HEAD.next = first real node (or TAIL if empty)
    // TAIL.prev = last real node (or HEAD if empty)

    private DLLNode head;
    private DLLNode tail;

    /** Initialize empty DLL with sentinel nodes. */
    public S8_DoublyLinkedList() {
        head = new DLLNode(); // dummy head
        tail = new DLLNode(); // dummy tail
        head.next = tail;
        tail.prev = head;
    }

    // =============================================================
    // 8.4  CORE OPERATIONS
    // =============================================================

    /**
     * ADD NODE RIGHT AFTER HEAD (add to front) — O(1)
     *
     * Before:   HEAD ↔ [existing] ↔ ... ↔ TAIL
     *
     * After:    HEAD ↔ [new] ↔ [existing] ↔ ... ↔ TAIL
     *
     * Four pointer updates:
     *   1. node.prev = head
     *   2. node.next = head.next
     *   3. head.next.prev = node
     *   4. head.next = node
     *
     * ⚠️ ORDER MATTERS: update the OLD neighbors before overwriting head.next
     */
    public void addToFront(DLLNode node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    /**
     * ADD NODE RIGHT BEFORE TAIL (add to back) — O(1)
     *
     * Before:   HEAD ↔ ... ↔ [existing] ↔ TAIL
     *
     * After:    HEAD ↔ ... ↔ [existing] ↔ [new] ↔ TAIL
     */
    public void addToBack(DLLNode node) {
        node.next = tail;
        node.prev = tail.prev;
        tail.prev.next = node;
        tail.prev = node;
    }

    /**
     * REMOVE A SPECIFIC NODE — O(1) given the reference
     *
     * Before:   ... ↔ [A] ↔ [node] ↔ [B] ↔ ...
     *
     * After:    ... ↔ [A] ↔ [B] ↔ ...
     *                 (node is detached)
     *
     * Two pointer updates:
     *   1. node.prev.next = node.next   (A now points to B)
     *   2. node.next.prev = node.prev   (B now points back to A)
     *
     * This is why DLL is O(1) removal: with a reference to the node,
     * we don't need to find its predecessor (we have .prev).
     * In a singly linked list, removal is O(n) because you must
     * traverse to find the previous node.
     */
    public void removeNode(DLLNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    /**
     * MOVE NODE TO FRONT — O(1)
     *
     * Combines remove + addToFront.
     * Used in LRU Cache: when a key is accessed, move it to front
     * (most recently used position).
     */
    public void moveToFront(DLLNode node) {
        removeNode(node);
        addToFront(node);
    }

    /**
     * REMOVE AND RETURN THE LAST REAL NODE (before tail) — O(1)
     *
     * Used in LRU Cache: evict the least recently used entry.
     * Returns the removed node so we can also remove it from HashMap.
     */
    public DLLNode removeLast() {
        if (head.next == tail) return null; // empty list
        DLLNode last = tail.prev;
        removeNode(last);
        return last;
    }

    /** Check if the list is empty. */
    public boolean isEmpty() {
        return head.next == tail;
    }

    /** Debug: print the list front to back. */
    public void printForward() {
        System.out.print("HEAD ↔ ");
        DLLNode curr = head.next;
        while (curr != tail) {
            System.out.print("[" + curr.key + ":" + curr.value + "] ↔ ");
            curr = curr.next;
        }
        System.out.println("TAIL");
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        S8_DoublyLinkedList dll = new S8_DoublyLinkedList();

        System.out.println("=== EMPTY LIST ===");
        dll.printForward();
        // HEAD ↔ TAIL

        System.out.println("\n=== ADD TO FRONT ===");
        DLLNode a = new DLLNode(1, 100);
        DLLNode b = new DLLNode(2, 200);
        DLLNode c = new DLLNode(3, 300);

        dll.addToFront(a); dll.printForward(); // HEAD ↔ [1:100] ↔ TAIL
        dll.addToFront(b); dll.printForward(); // HEAD ↔ [2:200] ↔ [1:100] ↔ TAIL
        dll.addToFront(c); dll.printForward(); // HEAD ↔ [3:300] ↔ [2:200] ↔ [1:100] ↔ TAIL

        System.out.println("\n=== REMOVE NODE (middle) ===");
        dll.removeNode(b); // remove node with key=2
        dll.printForward(); // HEAD ↔ [3:300] ↔ [1:100] ↔ TAIL

        System.out.println("\n=== MOVE TO FRONT ===");
        dll.moveToFront(a); // move key=1 to front
        dll.printForward(); // HEAD ↔ [1:100] ↔ [3:300] ↔ TAIL

        System.out.println("\n=== ADD TO BACK ===");
        DLLNode d = new DLLNode(4, 400);
        dll.addToBack(d);
        dll.printForward(); // HEAD ↔ [1:100] ↔ [3:300] ↔ [4:400] ↔ TAIL

        System.out.println("\n=== REMOVE LAST (LRU eviction) ===");
        DLLNode evicted = dll.removeLast();
        System.out.println("Evicted: key=" + evicted.key + " val=" + evicted.value); // key=4
        dll.printForward(); // HEAD ↔ [1:100] ↔ [3:300] ↔ TAIL
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — DOUBLY LINKED LIST                         │
 * │                                                             │
 * │ 1. SENTINEL NODES eliminate every null check. In an         │
 * │    interview, always say "I'll use dummy head and tail"     │
 * │    immediately. It halves the edge cases.                   │
 * │                                                             │
 * │ 2. The 4-pointer insert and 2-pointer remove are the only   │
 * │    operations you need. Memorize the pointer update ORDER — │
 * │    getting it wrong causes silent corruption.               │
 * │                                                             │
 * │ 3. For LRU Cache, explain the DLL+HashMap synergy:          │
 * │    "The HashMap gives O(1) node lookup. The DLL gives O(1)  │
 * │    removal and ordering. Together, every LRU operation is   │
 * │    O(1)." This is the answer interviewers want.             │
 * │                                                             │
 * │ 4. Store the KEY in DLL nodes (not just value). Explain     │
 * │    why: "When I evict the tail node, I need its key to      │
 * │    remove it from the HashMap." This shows you've thought   │
 * │    through the full design.                                 │
 * │                                                             │
 * │ 5. Common mistake: forgetting to update BOTH prev and next  │
 * │    pointers. A DLL always needs pairs of updates. Draw it   │
 * │    on the whiteboard — it's much easier visually.           │
 * └─────────────────────────────────────────────────────────────┘
 */

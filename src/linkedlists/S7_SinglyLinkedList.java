package linkedlists;

/**
 * ============================================================
 * SECTION 7: SINGLY LINKED LIST
 * ============================================================
 *
 * A singly linked list is a chain of nodes where each node points
 * to the next. You can only traverse FORWARD (no going back).
 *
 * Structure:
 *   head → [1] → [2] → [3] → [4] → null
 *
 * Key properties:
 *   - No random access (can't do list[i] in O(1) like arrays)
 *   - Insert/delete at head is O(1)
 *   - Insert/delete at arbitrary position is O(n) (must find it first)
 *   - Uses exactly as much memory as needed (no wasted capacity)
 */
public class S7_SinglyLinkedList {

    // =============================================================
    // 7.2  CORE OPERATIONS
    // =============================================================

    /**
     * INSERT AT HEAD — O(1)
     *
     * Before: head → [2] → [3] → null
     * After:  head → [1] → [2] → [3] → null
     *
     * Two steps:
     *   1. newNode.next = head
     *   2. head = newNode
     */
    public static ListNode insertAtHead(ListNode head, int val) {
        ListNode newNode = new ListNode(val);
        newNode.next = head;
        return newNode; // new head
    }

    /**
     * INSERT AT TAIL — O(n) without tail pointer
     *
     * Must walk to the end of the list to find the last node.
     *
     * Before: head → [1] → [2] → null
     * After:  head → [1] → [2] → [3] → null
     */
    public static ListNode insertAtTail(ListNode head, int val) {
        ListNode newNode = new ListNode(val);
        if (head == null) return newNode;

        ListNode curr = head;
        while (curr.next != null) {
            curr = curr.next;
        }
        curr.next = newNode;
        return head;
    }

    /**
     * INSERT AT POSITION — O(n)
     *
     * Insert at index `pos` (0-based). pos=0 means insert at head.
     */
    public static ListNode insertAtPosition(ListNode head, int val, int pos) {
        if (pos == 0) return insertAtHead(head, val);

        ListNode curr = head;
        for (int i = 0; i < pos - 1 && curr != null; i++) {
            curr = curr.next;
        }
        if (curr == null) return head; // position out of bounds

        ListNode newNode = new ListNode(val);
        newNode.next = curr.next;
        curr.next = newNode;
        return head;
    }

    /**
     * DELETE BY VALUE — O(n)
     *
     * Find the node with the given value and remove it.
     * Uses a dummy head to simplify edge cases (deleting the head).
     *
     * Before: head → [1] → [2] → [3] → null   (delete 2)
     *
     *   prev     curr
     *    ↓        ↓
     *   [1]  →  [2]  →  [3]
     *
     * After:  prev.next = curr.next
     *         [1] → [3] → null
     */
    public static ListNode deleteByValue(ListNode head, int val) {
        // Dummy node simplifies deletion of head
        ListNode dummy = new ListNode(0, head);
        ListNode prev = dummy;
        ListNode curr = head;

        while (curr != null) {
            if (curr.val == val) {
                prev.next = curr.next; // bypass the node
                break;
            }
            prev = curr;
            curr = curr.next;
        }
        return dummy.next;
    }

    /**
     * SEARCH — O(n)
     *
     * Returns true if the value exists in the list.
     */
    public static boolean search(ListNode head, int val) {
        while (head != null) {
            if (head.val == val) return true;
            head = head.next;
        }
        return false;
    }

    /**
     * GET LENGTH — O(n)
     */
    public static int getLength(ListNode head) {
        int count = 0;
        while (head != null) {
            count++;
            head = head.next;
        }
        return count;
    }

    // =============================================================
    // 7.3  REVERSE A LINKED LIST (LC 206) ← TOP INTERVIEW QUESTION
    // =============================================================
    //
    // This is asked in ~30% of linked list interviews. Know it cold.

    /**
     * ITERATIVE REVERSE — the 3-pointer technique.
     *
     * Maintain three pointers: prev, curr, next.
     * At each step, reverse curr's pointer to point to prev.
     *
     * Walk-through:
     *
     *   Initial:  null ← prev   curr → [1] → [2] → [3] → null
     *
     *   Step 1:   next = curr.next         // save next (2)
     *             curr.next = prev         // reverse: 1 → null
     *             prev = curr              // advance prev to 1
     *             curr = next              // advance curr to 2
     *
     *             null ← [1] ← prev   curr → [2] → [3] → null
     *
     *   Step 2:   next = curr.next         // save next (3)
     *             curr.next = prev         // reverse: 2 → 1
     *             prev = curr              // advance prev to 2
     *             curr = next              // advance curr to 3
     *
     *             null ← [1] ← [2] ← prev   curr → [3] → null
     *
     *   Step 3:   next = curr.next         // save next (null)
     *             curr.next = prev         // reverse: 3 → 2
     *             prev = curr              // advance prev to 3
     *             curr = next              // advance curr to null
     *
     *             null ← [1] ← [2] ← [3] ← prev   curr = null
     *
     *   Done! prev is the new head.
     *
     * Time:  O(n)
     * Space: O(1)
     */
    public static ListNode reverseIterative(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;

        while (curr != null) {
            ListNode next = curr.next; // save
            curr.next = prev;          // reverse
            prev = curr;               // advance prev
            curr = next;               // advance curr
        }
        return prev; // new head
    }

    /**
     * RECURSIVE REVERSE.
     *
     * Intuition: assume the rest of the list is already reversed.
     * Then just fix the connection between the current node and the next.
     *
     *   Before recursion returns for node 2:
     *     [1] → [2] → [3] ← [4] ← [5]  (3,4,5 already reversed)
     *                   ↓
     *                  null
     *
     *   Fix: node.next.next = node   →   [2] ← [3]
     *        node.next = null        →   [1] → [2] → null (break forward link)
     *
     * Time:  O(n)
     * Space: O(n) — recursion stack
     */
    public static ListNode reverseRecursive(ListNode head) {
        // Base case: empty list or single node
        if (head == null || head.next == null) return head;

        // Reverse the rest of the list
        ListNode newHead = reverseRecursive(head.next);

        // Fix the connection
        head.next.next = head; // the node after me should point back to me
        head.next = null;      // I'm now the tail, so point to null

        return newHead; // the new head is always the old tail
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== BUILD AND TRAVERSE ===");
        ListNode list = ListNode.fromArray(1, 2, 3, 4, 5);
        System.out.println("Original: " + ListNode.toString(list));

        System.out.println("\n=== INSERT OPERATIONS ===");
        list = insertAtHead(list, 0);
        System.out.println("Insert 0 at head: " + ListNode.toString(list));

        list = insertAtTail(list, 6);
        System.out.println("Insert 6 at tail: " + ListNode.toString(list));

        list = insertAtPosition(list, 99, 3);
        System.out.println("Insert 99 at pos 3: " + ListNode.toString(list));

        System.out.println("\n=== DELETE AND SEARCH ===");
        list = deleteByValue(list, 99);
        System.out.println("Delete 99: " + ListNode.toString(list));

        System.out.println("Search 3: " + search(list, 3));   // true
        System.out.println("Search 99: " + search(list, 99)); // false
        System.out.println("Length: " + getLength(list));      // 7

        System.out.println("\n=== REVERSE (ITERATIVE) ===");
        ListNode rev1 = ListNode.fromArray(1, 2, 3, 4, 5);
        System.out.println("Before: " + ListNode.toString(rev1));
        rev1 = reverseIterative(rev1);
        System.out.println("After:  " + ListNode.toString(rev1));

        System.out.println("\n=== REVERSE (RECURSIVE) ===");
        ListNode rev2 = ListNode.fromArray(1, 2, 3, 4, 5);
        System.out.println("Before: " + ListNode.toString(rev2));
        rev2 = reverseRecursive(rev2);
        System.out.println("After:  " + ListNode.toString(rev2));
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — SINGLY LINKED LIST                         │
 * │                                                             │
 * │ 1. ALWAYS use a DUMMY HEAD node when deleting or modifying  │
 * │    the head of the list. It eliminates the "special case     │
 * │    when head is the node to delete" edge case. This is the  │
 * │    single most important linked list technique.             │
 * │                                                             │
 * │ 2. The iterative reverse (prev/curr/next) is THE most       │
 * │    important algorithm. It appears in: reverse list, reverse │
 * │    k-group, palindrome check, reorder list, etc.            │
 * │    Draw it out with arrows on the whiteboard.               │
 * │                                                             │
 * │ 3. Common edge cases to check:                              │
 * │    - Empty list (head == null)                              │
 * │    - Single node                                            │
 * │    - Deleting the head node                                 │
 * │    - Deleting the tail node                                 │
 * │                                                             │
 * │ 4. When asked "what's the trade-off vs arrays?":            │
 * │    LL: O(1) insert/delete at known position, no resizing    │
 * │    Array: O(1) random access, cache-friendly, less memory   │
 * │    per element (no next pointer overhead)                   │
 * └─────────────────────────────────────────────────────────────┘
 */

package linkedlists;

/**
 * ============================================================
 * SECTION 7.1: ListNode — Singly Linked List Node
 * ============================================================
 *
 * Standard LeetCode definition. Same structure used in 95%+ of
 * linked list interview problems.
 */
public class ListNode {
    int val;
    ListNode next;

    ListNode() {}

    ListNode(int val) {
        this.val = val;
    }

    ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }

    /** Helper: build a linked list from an array. */
    public static ListNode fromArray(int... vals) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        for (int v : vals) {
            curr.next = new ListNode(v);
            curr = curr.next;
        }
        return dummy.next;
    }

    /** Helper: print the list. */
    public static String toString(ListNode head) {
        StringBuilder sb = new StringBuilder();
        while (head != null) {
            sb.append(head.val).append(" → ");
            head = head.next;
        }
        sb.append("null");
        return sb.toString();
    }
}

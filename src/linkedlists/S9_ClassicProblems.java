package linkedlists;

/**
 * ============================================================
 * SECTION 9: CLASSIC LINKED LIST INTERVIEW PROBLEMS
 * ============================================================
 *
 * These 6 problems appear constantly across all companies.
 * Every one uses a clever pointer technique.
 */
public class S9_ClassicProblems {

    // =============================================================
    // 9.1  REVERSE A LINKED LIST (LC 206) — see also Section 7
    // =============================================================
    // Covered in S7_SinglyLinkedList. Here for reference:

    /** Iterative: prev/curr/next 3-pointer technique. O(n) time, O(1) space. */
    public static ListNode reverse(ListNode head) {
        ListNode prev = null, curr = head;
        while (curr != null) {
            ListNode next = curr.next;
            curr.next = prev;
            prev = curr;
            curr = next;
        }
        return prev;
    }

    // =============================================================
    // 9.2  DETECT A CYCLE (LC 141) — Floyd's Tortoise and Hare
    // =============================================================
    //
    // TWO POINTERS: slow moves 1 step, fast moves 2 steps.
    // If there's a cycle, fast will "lap" slow and they'll meet.
    // If no cycle, fast reaches null.
    //
    // WHY DOES THIS WORK?
    //   Once both pointers are inside the cycle:
    //   - Each step, the gap between fast and slow DECREASES by 1.
    //     (fast gains 1 step on slow per iteration)
    //   - So they're guaranteed to meet within one full loop of the cycle.
    //
    //   Think of it like two runners on a circular track:
    //   the faster one will always lap the slower one.
    //
    // Time:  O(n) — fast traverses at most 2n nodes
    // Space: O(1) — no extra data structures

    public static boolean hasCycle(ListNode head) {
        ListNode slow = head, fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;       // 1 step
            fast = fast.next.next;  // 2 steps

            if (slow == fast) return true; // they met → cycle
        }
        return false; // fast reached null → no cycle
    }

    /**
     * FIND THE START OF THE CYCLE (LC 142)
     *
     * After detecting the cycle (slow == fast), reset one pointer to head.
     * Move both at 1 step each. Where they meet is the CYCLE START.
     *
     * WHY? (Mathematical proof):
     *   Let D = distance from head to cycle start.
     *   Let C = cycle length.
     *   When slow and fast meet:
     *     - slow has traveled D + k steps inside cycle
     *     - fast has traveled D + k + mC steps (m full loops)
     *     - fast = 2 * slow → D + k + mC = 2(D + k) → mC = D + k → D = mC - k
     *   So if we start one pointer at head and one at the meeting point,
     *   both moving 1 step, they'll meet at the cycle start after D steps.
     */
    public static ListNode detectCycleStart(ListNode head) {
        ListNode slow = head, fast = head;

        // Phase 1: detect cycle
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) break;
        }

        // No cycle
        if (fast == null || fast.next == null) return null;

        // Phase 2: find cycle start
        slow = head;
        while (slow != fast) {
            slow = slow.next;
            fast = fast.next;
        }
        return slow; // cycle start
    }

    // =============================================================
    // 9.3  FIND THE MIDDLE NODE (LC 876)
    // =============================================================
    //
    // SLOW/FAST POINTER: slow moves 1, fast moves 2.
    // When fast reaches the end, slow is at the middle.
    //
    // For even-length lists, this returns the SECOND middle node
    // (which is what LeetCode expects).
    //
    //   [1] → [2] → [3] → [4] → [5] → null
    //                 ↑ slow         ↑ fast
    //
    // Time: O(n), Space: O(1)

    public static ListNode findMiddle(ListNode head) {
        ListNode slow = head, fast = head;

        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    // =============================================================
    // 9.4  MERGE TWO SORTED LISTS (LC 21)
    // =============================================================
    //
    // Use a DUMMY HEAD and a tail pointer. Compare the heads of both
    // lists, attach the smaller one to tail, advance that list.
    //
    // This is the merge step of merge sort applied to linked lists.
    //
    // Time: O(n + m), Space: O(1)

    public static ListNode mergeTwoSorted(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode tail = dummy;

        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                tail.next = l1;
                l1 = l1.next;
            } else {
                tail.next = l2;
                l2 = l2.next;
            }
            tail = tail.next;
        }

        // Attach the remaining list (one of them is non-null)
        tail.next = (l1 != null) ? l1 : l2;

        return dummy.next;
    }

    // =============================================================
    // 9.5  REMOVE NTH NODE FROM END (LC 19)
    // =============================================================
    //
    // TWO-POINTER GAP TECHNIQUE:
    //   1. Move `fast` pointer n steps ahead.
    //   2. Move both `fast` and `slow` together until fast reaches the end.
    //   3. slow is now at the node BEFORE the one to delete.
    //
    // The gap ensures slow stops at position (length - n).
    //
    //   n=2:  remove second from end
    //
    //   [1] → [2] → [3] → [4] → [5] → null
    //          ↑ slow         ↑ fast (gap of 2)
    //
    //   Move together until fast.next == null:
    //   [1] → [2] → [3] → [4] → [5] → null
    //                 ↑ slow         ↑ fast
    //
    //   slow.next = slow.next.next → removes [4]
    //
    // Time: O(n), Space: O(1)

    public static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head); // handles removing the head
        ListNode fast = dummy, slow = dummy;

        // Move fast n+1 steps ahead (so slow lands BEFORE the target)
        for (int i = 0; i <= n; i++) {
            fast = fast.next;
        }

        // Move together
        while (fast != null) {
            slow = slow.next;
            fast = fast.next;
        }

        // Delete the nth from end
        slow.next = slow.next.next;

        return dummy.next;
    }

    // =============================================================
    // 9.6  PALINDROME LINKED LIST (LC 234)
    // =============================================================
    //
    // Strategy:
    //   1. Find the middle (slow/fast pointer)
    //   2. Reverse the second half
    //   3. Compare first half and reversed second half
    //   4. (Optional) restore the list
    //
    // Time: O(n), Space: O(1) — modifies the list in place

    public static boolean isPalindrome(ListNode head) {
        if (head == null || head.next == null) return true;

        // Step 1: find the middle
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        // slow is now at the end of the first half

        // Step 2: reverse the second half
        ListNode secondHalf = reverse(slow.next);
        slow.next = null; // split the list

        // Step 3: compare both halves
        ListNode p1 = head, p2 = secondHalf;
        boolean result = true;
        while (p2 != null) { // second half may be shorter (odd length)
            if (p1.val != p2.val) {
                result = false;
                break;
            }
            p1 = p1.next;
            p2 = p2.next;
        }

        // Step 4: restore (optional, but good practice to mention)
        slow.next = reverse(secondHalf);

        return result;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== CYCLE DETECTION ===");
        ListNode cycleList = ListNode.fromArray(1, 2, 3, 4, 5);
        System.out.println("No cycle: " + hasCycle(cycleList)); // false

        // Create a cycle: 5 → 3
        ListNode node3 = cycleList.next.next; // node with val=3
        ListNode node5 = node3.next.next;     // node with val=5
        node5.next = node3;                   // create cycle
        System.out.println("With cycle: " + hasCycle(cycleList)); // true

        ListNode cycleStart = detectCycleStart(cycleList);
        System.out.println("Cycle starts at: " + cycleStart.val); // 3
        node5.next = null; // clean up cycle for further use

        System.out.println("\n=== FIND MIDDLE ===");
        ListNode mid1 = ListNode.fromArray(1, 2, 3, 4, 5);
        System.out.println("Middle of [1,2,3,4,5]: " + findMiddle(mid1).val); // 3

        ListNode mid2 = ListNode.fromArray(1, 2, 3, 4);
        System.out.println("Middle of [1,2,3,4]:   " + findMiddle(mid2).val); // 3 (second middle)

        System.out.println("\n=== MERGE TWO SORTED ===");
        ListNode l1 = ListNode.fromArray(1, 3, 5);
        ListNode l2 = ListNode.fromArray(2, 4, 6);
        ListNode merged = mergeTwoSorted(l1, l2);
        System.out.println("Merged: " + ListNode.toString(merged));
        // 1 → 2 → 3 → 4 → 5 → 6 → null

        System.out.println("\n=== REMOVE NTH FROM END ===");
        ListNode rem = ListNode.fromArray(1, 2, 3, 4, 5);
        rem = removeNthFromEnd(rem, 2);
        System.out.println("Remove 2nd from end: " + ListNode.toString(rem));
        // 1 → 2 → 3 → 5 → null

        System.out.println("\n=== PALINDROME CHECK ===");
        System.out.println("[1,2,3,2,1] palindrome? " +
            isPalindrome(ListNode.fromArray(1, 2, 3, 2, 1))); // true
        System.out.println("[1,2,3,4,5] palindrome? " +
            isPalindrome(ListNode.fromArray(1, 2, 3, 4, 5))); // false
        System.out.println("[1,2,2,1] palindrome?   " +
            isPalindrome(ListNode.fromArray(1, 2, 2, 1)));     // true
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — CLASSIC LL PROBLEMS                        │
 * │                                                             │
 * │ 1. FLOYD'S CYCLE DETECTION: don't just code it — explain    │
 * │    WHY it works. "Fast gains 1 step per iteration, so the   │
 * │    gap decreases by 1 each time until they meet."           │
 * │                                                             │
 * │ 2. SLOW/FAST is the universal linked list technique:        │
 * │    - Middle node: slow 1x, fast 2x, stop when fast ends    │
 * │    - Cycle: slow 1x, fast 2x, check if they meet           │
 * │    - Nth from end: offset fast by n, then move together     │
 * │                                                             │
 * │ 3. MERGE SORTED LISTS: always use a dummy head. The code    │
 * │    is twice as clean and handles all edge cases. This is    │
 * │    the same merge step as merge sort.                       │
 * │                                                             │
 * │ 4. PALINDROME: this combines THREE techniques (find middle, │
 * │    reverse, compare). If you can do this problem smoothly,  │
 * │    you've proven linked list fluency.                       │
 * │                                                             │
 * │ 5. Always mention: "I'll restore the list after modifying   │
 * │    it." Interviewers notice when you treat the input with   │
 * │    care. In production, you don't mutate inputs.            │
 * └─────────────────────────────────────────────────────────────┘
 */

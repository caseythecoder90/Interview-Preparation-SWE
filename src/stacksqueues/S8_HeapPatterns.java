package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 8: HEAP PATTERNS IN INTERVIEWS
 * ============================================================
 *
 * Five core patterns:
 *   1. Top K Frequent Elements (LC 347)
 *   2. K Closest Points to Origin (LC 973)
 *   3. Find Median from Data Stream (LC 295)
 *   4. Merge K Sorted Lists (LC 23)
 *   5. Task Scheduler (Fullstory pattern)
 */
public class S8_HeapPatterns {

    // =============================================================
    // 8.1  TOP K FREQUENT ELEMENTS (LC 347)
    // =============================================================
    //
    // Given an array and integer k, return the k most frequent elements.
    //
    // APPROACH: frequency map + min-heap of size K.
    //
    // WHY MIN-HEAP of size K (not max-heap of all)?
    //   - Max-heap of all N unique elements: O(n log n) to build + extract K
    //   - Min-heap of size K: O(n log k) — always keep only K candidates
    //   The min-heap acts as a "gatekeeper": its root is the LEAST frequent
    //   among the top K. When a more frequent element arrives, it kicks out
    //   the root. At the end, the heap contains exactly the top K.
    //
    // Walkthrough: nums = [1,1,1,2,2,3], k = 2
    //
    //   Step 1: frequency map → {1:3, 2:2, 3:1}
    //
    //   Step 2: process with min-heap (sorted by frequency):
    //     Process 1 (freq=3): heap = [(1,3)]             size=1 < k=2 → just add
    //     Process 2 (freq=2): heap = [(2,2), (1,3)]      size=2 = k → full
    //     Process 3 (freq=1): freq=1 < heap root freq=2  → skip (not more frequent)
    //
    //   Result: [1, 2]  (the two most frequent)
    //
    // Time: O(n log k), Space: O(n) for the frequency map

    public static int[] topKFrequent(int[] nums, int k) {
        // Step 1: build frequency map
        Map<Integer, Integer> freq = new HashMap<>();
        for (int n : nums) {
            freq.merge(n, 1, Integer::sum);
        }

        // Step 2: min-heap of size K, sorted by frequency
        PriorityQueue<Map.Entry<Integer, Integer>> minHeap =
            new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) {
                minHeap.poll(); // remove least frequent
            }
        }

        // Step 3: extract results
        int[] result = new int[k];
        for (int i = 0; i < k; i++) {
            result[i] = minHeap.poll().getKey();
        }
        return result;
    }

    // =============================================================
    // 8.2  K CLOSEST POINTS TO ORIGIN (LC 973)
    // =============================================================
    //
    // Given points on a 2D plane, find the K closest to origin (0,0).
    //
    // APPROACH: MAX-HEAP of size K, sorted by distance.
    //
    // Why MAX-heap this time?
    //   We want the K SMALLEST distances. The max-heap's root is the
    //   LARGEST distance among our K candidates — the "gatekeeper."
    //   If a new point is closer than the root, it kicks the root out.
    //
    // Distance formula: sqrt(x² + y²). But we compare x² + y² directly
    // (no sqrt needed — preserves relative ordering and avoids floating point).
    //
    // Time: O(n log k), Space: O(k)

    public static int[][] kClosest(int[][] points, int k) {
        // Max-heap by distance (farthest point at root)
        PriorityQueue<int[]> maxHeap = new PriorityQueue<>(
            (a, b) -> (b[0] * b[0] + b[1] * b[1]) - (a[0] * a[0] + a[1] * a[1])
        );

        for (int[] point : points) {
            maxHeap.offer(point);
            if (maxHeap.size() > k) {
                maxHeap.poll(); // remove farthest
            }
        }

        int[][] result = new int[k][2];
        for (int i = 0; i < k; i++) {
            result[i] = maxHeap.poll();
        }
        return result;
    }

    // =============================================================
    // 8.3  FIND MEDIAN FROM DATA STREAM (LC 295)
    // =============================================================
    //
    // Design a data structure that supports:
    //   addNum(int num) — add a number from the stream
    //   findMedian()    — return the median of all numbers so far
    //
    // THE TWO-HEAP TRICK:
    //
    //   Split the numbers into two halves:
    //     maxHeap (left half):  stores the SMALLER half. Root = largest of small half.
    //     minHeap (right half): stores the LARGER half.  Root = smallest of large half.
    //
    //   Invariants:
    //     1. maxHeap.size() == minHeap.size()  or  maxHeap.size() == minHeap.size() + 1
    //     2. Every element in maxHeap <= every element in minHeap
    //
    //   Median:
    //     If sizes are equal: average of both roots
    //     If maxHeap has one extra: maxHeap root
    //
    // Walkthrough: add 5, 2, 8, 1, 4
    //
    //   add(5): maxHeap=[5], minHeap=[]           median=5
    //   add(2): maxHeap=[2], minHeap=[5]          median=(2+5)/2=3.5
    //   add(8): maxHeap=[2,5], minHeap=[8]
    //           → rebalance: maxHeap=[2], minHeap=[5,8]
    //           → actually: add to maxHeap, move max to minHeap, then rebalance
    //           maxHeap=[5,2], minHeap=[8]        median=5
    //   add(1): maxHeap=[2,1], minHeap=[5,8]      median=(2+5)/2=3.5
    //   add(4): maxHeap=[4,2,1], minHeap=[5,8]    median=4
    //
    // Time:  O(log n) per addNum, O(1) for findMedian
    // Space: O(n)

    static class MedianFinder {
        // maxHeap: left half (smaller numbers). Root = largest of left.
        private PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        // minHeap: right half (larger numbers). Root = smallest of right.
        private PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        public void addNum(int num) {
            // Step 1: add to maxHeap first
            maxHeap.offer(num);

            // Step 2: ensure maxHeap root <= minHeap root
            // Move the largest from maxHeap to minHeap
            minHeap.offer(maxHeap.poll());

            // Step 3: rebalance sizes (maxHeap can have at most 1 extra)
            if (minHeap.size() > maxHeap.size()) {
                maxHeap.offer(minHeap.poll());
            }
        }

        public double findMedian() {
            if (maxHeap.size() > minHeap.size()) {
                return maxHeap.peek(); // odd total: maxHeap has the extra
            }
            return (maxHeap.peek() + minHeap.peek()) / 2.0; // even total: average
        }
    }

    // =============================================================
    // 8.4  MERGE K SORTED LISTS (LC 23)
    // =============================================================
    //
    // Given K sorted linked lists, merge them into ONE sorted list.
    //
    // APPROACH: min-heap of size K, holding one node from each list.
    //
    //   1. Initialize: add the HEAD of each list to the min-heap.
    //   2. Loop: poll the smallest node, add it to the result.
    //      If that node has a next, add next to the heap.
    //   3. Repeat until heap is empty.
    //
    // The heap always holds at most K nodes (one from each list).
    // Each poll + offer is O(log k). Total nodes: n.
    //
    // Time: O(n log k) where n = total nodes, k = number of lists
    // Space: O(k) for the heap

    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }

        static ListNode fromArray(int... vals) {
            ListNode dummy = new ListNode(0);
            ListNode curr = dummy;
            for (int v : vals) {
                curr.next = new ListNode(v);
                curr = curr.next;
            }
            return dummy.next;
        }

        static String toString(ListNode head) {
            StringBuilder sb = new StringBuilder();
            while (head != null) {
                sb.append(head.val);
                if (head.next != null) sb.append(" -> ");
                head = head.next;
            }
            return sb.toString();
        }
    }

    public static ListNode mergeKLists(ListNode[] lists) {
        // Min-heap sorted by node value
        PriorityQueue<ListNode> minHeap = new PriorityQueue<>(
            Comparator.comparingInt(node -> node.val)
        );

        // Add head of each non-empty list
        for (ListNode head : lists) {
            if (head != null) minHeap.offer(head);
        }

        ListNode dummy = new ListNode(0);
        ListNode tail = dummy;

        while (!minHeap.isEmpty()) {
            ListNode smallest = minHeap.poll();
            tail.next = smallest;
            tail = tail.next;

            // If this node has a next, add it to the heap
            if (smallest.next != null) {
                minHeap.offer(smallest.next);
            }
        }

        return dummy.next;
    }

    // =============================================================
    // 8.5  TASK SCHEDULER — Fullstory Pattern
    // =============================================================
    //
    // A task scheduler that always executes the highest-priority task next.
    // Maps to the Fullstory Task Scheduler coding problem.
    //
    // Uses a PriorityQueue to always give O(log n) insertion and O(log n)
    // extraction of the highest-priority task.

    static class TaskScheduler {
        private PriorityQueue<ScheduledTask> taskQueue;
        private int taskIdCounter = 0;

        TaskScheduler() {
            // Lower priority number = higher priority.
            // Tie-break by insertion order (lower ID first).
            taskQueue = new PriorityQueue<>((a, b) -> {
                if (a.priority != b.priority) return a.priority - b.priority;
                return a.id - b.id; // FIFO within same priority
            });
        }

        /** Schedule a task with a given priority. Returns a task ID. */
        public int schedule(String taskName, int priority) {
            int id = taskIdCounter++;
            taskQueue.offer(new ScheduledTask(id, taskName, priority));
            return id;
        }

        /** Execute and return the next highest-priority task. Null if empty. */
        public String executeNext() {
            ScheduledTask task = taskQueue.poll();
            return task != null ? task.name : null;
        }

        /** How many tasks are pending? */
        public int pendingCount() {
            return taskQueue.size();
        }

        static class ScheduledTask {
            int id;
            String name;
            int priority;

            ScheduledTask(int id, String name, int priority) {
                this.id = id;
                this.name = name;
                this.priority = priority;
            }
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== TOP K FREQUENT ELEMENTS ===");
        int[] nums = {1, 1, 1, 2, 2, 3};
        int[] topK = topKFrequent(nums, 2);
        System.out.println("Top 2 frequent in [1,1,1,2,2,3]: " + Arrays.toString(topK));
        // [1, 2] or [2, 1]

        System.out.println("\n=== K CLOSEST POINTS ===");
        int[][] points = {{3, 3}, {5, -1}, {-2, 4}};
        int[][] closest = kClosest(points, 2);
        System.out.println("2 closest to origin:");
        for (int[] p : closest) {
            System.out.println("  " + Arrays.toString(p));
        }
        // [3,3] and [-2,4] (distances: 18 and 20, vs 26 for [5,-1])

        System.out.println("\n=== FIND MEDIAN FROM DATA STREAM ===");
        MedianFinder mf = new MedianFinder();
        mf.addNum(5);
        System.out.println("add(5)  → median: " + mf.findMedian());  // 5.0
        mf.addNum(2);
        System.out.println("add(2)  → median: " + mf.findMedian());  // 3.5
        mf.addNum(8);
        System.out.println("add(8)  → median: " + mf.findMedian());  // 5.0
        mf.addNum(1);
        System.out.println("add(1)  → median: " + mf.findMedian());  // 3.5
        mf.addNum(4);
        System.out.println("add(4)  → median: " + mf.findMedian());  // 4.0

        System.out.println("\n=== MERGE K SORTED LISTS ===");
        ListNode[] lists = {
            ListNode.fromArray(1, 4, 5),
            ListNode.fromArray(1, 3, 4),
            ListNode.fromArray(2, 6)
        };
        ListNode merged = mergeKLists(lists);
        System.out.println("Merged: " + ListNode.toString(merged));
        // 1 -> 1 -> 2 -> 3 -> 4 -> 4 -> 5 -> 6

        System.out.println("\n=== TASK SCHEDULER ===");
        TaskScheduler scheduler = new TaskScheduler();
        scheduler.schedule("send-email", 3);       // low priority
        scheduler.schedule("process-payment", 1);   // critical
        scheduler.schedule("generate-report", 2);   // medium
        scheduler.schedule("log-event", 3);         // low priority

        System.out.println("Pending: " + scheduler.pendingCount()); // 4
        while (scheduler.pendingCount() > 0) {
            System.out.println("Execute: " + scheduler.executeNext());
        }
        // process-payment, generate-report, send-email, log-event
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — HEAP PATTERNS                               │
 * │                                                             │
 * │ 1. TOP K PATTERN: "I need the K largest → min-heap of K."   │
 * │    "I need the K smallest → max-heap of K." The heap root   │
 * │    is always the WORST of the K best — the gatekeeper.      │
 * │    Complexity: O(n log k), better than sorting O(n log n).  │
 * │                                                             │
 * │ 2. TWO-HEAP MEDIAN: the key invariant is that maxHeap holds │
 * │    the smaller half and minHeap holds the larger half. The   │
 * │    three-step add (add → balance tops → balance sizes) is   │
 * │    the cleanest implementation. Practice it.                │
 * │                                                             │
 * │ 3. MERGE K SORTED: explain why it's O(n log k) not O(nk):  │
 * │    "Each of the n elements does one heap insert (log k)     │
 * │    and one heap remove (log k). The heap never exceeds      │
 * │    size k." This is a huge improvement for large k.         │
 * │                                                             │
 * │ 4. TASK SCHEDULER: mention tie-breaking strategy. "When     │
 * │    priorities are equal, I use insertion order (FIFO) as a  │
 * │    tiebreaker." This shows attention to detail.             │
 * │                                                             │
 * │ 5. For distance/comparison problems: NEVER compute sqrt().  │
 * │    Compare squared distances — preserves ordering and is    │
 * │    faster. Mention this proactively.                        │
 * └─────────────────────────────────────────────────────────────┘
 */

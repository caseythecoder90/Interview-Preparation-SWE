package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 7: HEAP FUNDAMENTALS
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * 7.1  WHAT IS A HEAP?
 * -------------------------------------------------------
 *
 * A HEAP is a COMPLETE BINARY TREE with the HEAP PROPERTY:
 *
 *   MIN-HEAP: every parent <= both children (root = smallest)
 *   MAX-HEAP: every parent >= both children (root = largest)
 *
 * "Complete binary tree" means:
 *   - Every level is fully filled except possibly the last
 *   - The last level is filled LEFT TO RIGHT with no gaps
 *
 * A heap is NOT a BST. Siblings have no ordering relationship.
 * A min-heap only guarantees the root is the minimum — the second
 * smallest could be anywhere in the second level.
 *
 *
 * -------------------------------------------------------
 * 7.2  ARRAY REPRESENTATION
 * -------------------------------------------------------
 *
 * Because a heap is a COMPLETE binary tree, it maps perfectly to an array
 * with NO WASTED SPACE (no null gaps).
 *
 *   Tree view (min-heap):
 *
 *              1
 *            /   \
 *           3     5
 *          / \   /
 *         7   9 8
 *
 *   Array view:
 *     Index:  0  1  2  3  4  5
 *     Value: [1, 3, 5, 7, 9, 8]
 *
 *   INDEX FORMULAS (0-based):
 *     Left child:   2 * i + 1
 *     Right child:  2 * i + 2
 *     Parent:       (i - 1) / 2
 *
 *   Example:
 *     Node at index 1 (value 3):
 *       Left child:  2*1+1 = 3 (value 7)  ✓
 *       Right child: 2*1+2 = 4 (value 9)  ✓
 *       Parent:      (1-1)/2 = 0 (value 1) ✓
 *
 *   Why array works: a complete binary tree has no gaps when laid out
 *   level by level. Every index from 0 to n-1 is occupied.
 *
 *
 * -------------------------------------------------------
 * 7.3  HEAP OPERATIONS
 * -------------------------------------------------------
 *
 * INSERT (offer) — O(log n):
 *   1. Add new element at the END of the array (last position)
 *   2. SIFT UP: compare with parent, swap if smaller (min-heap)
 *   3. Repeat until heap property is restored or we reach the root
 *
 *   Example: insert 2 into min-heap [1, 3, 5, 7, 9, 8]
 *
 *     Step 1: Add 2 at index 6
 *       [1, 3, 5, 7, 9, 8, 2]
 *                            ^
 *     Step 2: Parent of 6 = (6-1)/2 = 2. arr[2]=5. 2 < 5 → swap
 *       [1, 3, 2, 7, 9, 8, 5]
 *               ^           ^
 *     Step 3: Parent of 2 = (2-1)/2 = 0. arr[0]=1. 2 > 1 → STOP
 *       [1, 3, 2, 7, 9, 8, 5]   ← valid min-heap
 *
 *     Tree view after insert:
 *              1
 *            /   \
 *           3     2       ← 2 sifted up to correct position
 *          / \   / \
 *         7   9 8   5
 *
 *
 * REMOVE MIN/MAX (poll) — O(log n):
 *   1. Replace root with the LAST element
 *   2. Remove the last position
 *   3. SIFT DOWN: compare with children, swap with SMALLER child (min-heap)
 *   4. Repeat until heap property is restored or we reach a leaf
 *
 *   Example: poll from min-heap [1, 3, 2, 7, 9, 8, 5]
 *
 *     Step 1: Replace root with last element (5). Remove last.
 *       [5, 3, 2, 7, 9, 8]
 *        ^
 *     Step 2: Children of 0 are 1(val=3) and 2(val=2). Smaller = 2(val=2). 5 > 2 → swap
 *       [2, 3, 5, 7, 9, 8]
 *        ^     ^
 *     Step 3: Children of 2 are 5(val=8). No right child. 5 < 8 → STOP
 *       [2, 3, 5, 7, 9, 8]   ← valid min-heap, returns 1
 *
 *
 * PEEK — O(1):
 *   Simply return the root element (index 0). Don't remove.
 *
 *
 * HEAPIFY (build heap from array) — O(n):
 *   Start from the last non-leaf node, sift down each node.
 *
 *   Why O(n) not O(n log n)?
 *   Most nodes are near the BOTTOM of the tree and sift down a SHORT distance.
 *     - n/2 nodes are leaves (sift distance 0)
 *     - n/4 nodes sift down 1 level
 *     - n/8 nodes sift down 2 levels
 *     - ...
 *   Sum: n/4 * 1 + n/8 * 2 + n/16 * 3 + ... = O(n)
 *   (This is a converging geometric series.)
 */
public class S7_HeapFundamentals {

    // =============================================================
    // 7.4  MANUAL MIN-HEAP IMPLEMENTATION
    // =============================================================
    // (For understanding — in interviews, use PriorityQueue)

    static class MinHeap {
        private int[] data;
        private int size;

        MinHeap(int capacity) {
            data = new int[capacity];
            size = 0;
        }

        public void offer(int val) {
            data[size] = val;
            siftUp(size);
            size++;
        }

        public int poll() {
            int min = data[0];
            size--;
            data[0] = data[size]; // move last to root
            siftDown(0);
            return min;
        }

        public int peek() {
            return data[0];
        }

        private void siftUp(int i) {
            while (i > 0) {
                int parent = (i - 1) / 2;
                if (data[i] < data[parent]) {
                    swap(i, parent);
                    i = parent;
                } else {
                    break;
                }
            }
        }

        private void siftDown(int i) {
            while (2 * i + 1 < size) { // while node has at least a left child
                int left = 2 * i + 1;
                int right = 2 * i + 2;
                int smallest = i;

                if (left < size && data[left] < data[smallest]) smallest = left;
                if (right < size && data[right] < data[smallest]) smallest = right;

                if (smallest != i) {
                    swap(i, smallest);
                    i = smallest;
                } else {
                    break;
                }
            }
        }

        private void swap(int i, int j) {
            int tmp = data[i];
            data[i] = data[j];
            data[j] = tmp;
        }

        public int size() { return size; }
        public boolean isEmpty() { return size == 0; }
    }

    // =============================================================
    // 7.5  JAVA'S PRIORITYQUEUE
    // =============================================================
    //
    // Java's PriorityQueue is a MIN-HEAP by default.
    // The smallest element is always at the top.
    //
    // API:
    //   offer(e)        O(log n)   — insert
    //   poll()          O(log n)   — remove and return min
    //   peek()          O(1)       — return min without removing
    //   size()          O(1)
    //   isEmpty()       O(1)
    //   remove(Object)  O(n)       — linear search to find the element!
    //   contains(Object) O(n)      — same: must search
    //
    // IMPORTANT GOTCHAS:
    //   1. Default is MIN-HEAP (smallest first). Many people expect max-heap.
    //   2. remove(Object) is O(n), not O(log n). It must FIND the element first.
    //   3. PriorityQueue is NOT sorted. Iterating does NOT give sorted order.
    //      Only poll() gives elements in order (each poll is O(log n)).
    //   4. You CANNOT efficiently access elements by index.

    // =============================================================
    // 7.6  CREATING MAX-HEAPS AND CUSTOM COMPARATORS
    // =============================================================

    static void customComparatorDemo() {
        // --- MAX-HEAP (largest element first) ---
        // Option 1: Collections.reverseOrder()
        PriorityQueue<Integer> maxHeap1 = new PriorityQueue<>(Collections.reverseOrder());

        // Option 2: Lambda comparator
        PriorityQueue<Integer> maxHeap2 = new PriorityQueue<>((a, b) -> b - a);
        // ⚠️ WARNING: (a, b) -> b - a can overflow for very large values.
        //   Safer: (a, b) -> Integer.compare(b, a)
        //   In interviews, (a, b) -> b - a is fine for typical inputs.

        // --- CUSTOM OBJECT: sort int[] by second element ---
        PriorityQueue<int[]> pqBySecond = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        pqBySecond.offer(new int[]{1, 50});
        pqBySecond.offer(new int[]{2, 10});
        pqBySecond.offer(new int[]{3, 30});
        // poll() → [2, 10] (smallest second element)

        // --- CUSTOM OBJECT: sort Map.Entry by value ---
        PriorityQueue<Map.Entry<String, Integer>> pqByValue =
            new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));

        // --- COMPARABLE vs COMPARATOR ---
        //
        // Comparable: the class itself defines its natural ordering.
        //   class Task implements Comparable<Task> {
        //       int priority;
        //       public int compareTo(Task other) { return this.priority - other.priority; }
        //   }
        //   PriorityQueue<Task> pq = new PriorityQueue<>(); // uses natural order
        //
        // Comparator: external comparison logic, passed to the PQ constructor.
        //   PriorityQueue<Task> pq = new PriorityQueue<>((a, b) -> a.deadline - b.deadline);
        //
        // USE Comparable when there's ONE obvious ordering (e.g., Task by priority).
        // USE Comparator when you need DIFFERENT orderings for different use cases.
    }

    static class Task implements Comparable<Task> {
        String name;
        int priority; // lower number = higher priority

        Task(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public int compareTo(Task other) {
            return Integer.compare(this.priority, other.priority);
        }

        @Override
        public String toString() {
            return name + "(p=" + priority + ")";
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== MANUAL MIN-HEAP ===");
        MinHeap heap = new MinHeap(10);
        for (int val : new int[]{5, 3, 8, 1, 9, 2}) {
            heap.offer(val);
        }
        System.out.print("Poll order: ");
        while (!heap.isEmpty()) {
            System.out.print(heap.poll() + " "); // 1 2 3 5 8 9
        }
        System.out.println();

        System.out.println("\n=== JAVA PRIORITYQUEUE (MIN-HEAP) ===");
        PriorityQueue<Integer> minPQ = new PriorityQueue<>();
        minPQ.offer(5);
        minPQ.offer(3);
        minPQ.offer(8);
        minPQ.offer(1);
        System.out.println("peek (min): " + minPQ.peek()); // 1
        System.out.print("Poll order: ");
        while (!minPQ.isEmpty()) {
            System.out.print(minPQ.poll() + " "); // 1 3 5 8
        }
        System.out.println();

        System.out.println("\n=== MAX-HEAP ===");
        PriorityQueue<Integer> maxPQ = new PriorityQueue<>(Collections.reverseOrder());
        maxPQ.offer(5);
        maxPQ.offer(3);
        maxPQ.offer(8);
        maxPQ.offer(1);
        System.out.println("peek (max): " + maxPQ.peek()); // 8
        System.out.print("Poll order: ");
        while (!maxPQ.isEmpty()) {
            System.out.print(maxPQ.poll() + " "); // 8 5 3 1
        }
        System.out.println();

        System.out.println("\n=== CUSTOM COMPARATOR (int[] by second element) ===");
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[1] - b[1]);
        pq.offer(new int[]{1, 50});
        pq.offer(new int[]{2, 10});
        pq.offer(new int[]{3, 30});
        System.out.print("Poll order: ");
        while (!pq.isEmpty()) {
            int[] arr = pq.poll();
            System.out.print(Arrays.toString(arr) + " "); // [2,10] [3,30] [1,50]
        }
        System.out.println();

        System.out.println("\n=== COMPARABLE TASK ===");
        PriorityQueue<Task> taskPQ = new PriorityQueue<>();
        taskPQ.offer(new Task("Low", 3));
        taskPQ.offer(new Task("Critical", 1));
        taskPQ.offer(new Task("Medium", 2));
        System.out.print("Execution order: ");
        while (!taskPQ.isEmpty()) {
            System.out.print(taskPQ.poll() + " ");
            // Critical(p=1) Medium(p=2) Low(p=3)
        }
        System.out.println();

        // Demonstrate the O(n) remove gotcha
        System.out.println("\n=== REMOVE(Object) IS O(n) ===");
        PriorityQueue<Integer> removePQ = new PriorityQueue<>(List.of(1, 5, 3, 7, 9));
        System.out.println("Before: peek=" + removePQ.peek() + " size=" + removePQ.size());
        removePQ.remove(5); // O(n) — scans the internal array
        System.out.println("After remove(5): peek=" + removePQ.peek() + " size=" + removePQ.size());

        // Demonstrate that iteration is NOT sorted
        System.out.println("\n=== ITERATION IS NOT SORTED ===");
        PriorityQueue<Integer> iterPQ = new PriorityQueue<>(List.of(5, 1, 8, 3));
        System.out.println("Iteration (NOT sorted): " + iterPQ);
        System.out.print("poll() order (sorted):  ");
        while (!iterPQ.isEmpty()) {
            System.out.print(iterPQ.poll() + " ");
        }
        System.out.println();
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — HEAP FUNDAMENTALS                           │
 * │                                                             │
 * │ 1. DEFAULT IS MIN-HEAP. This catches people constantly.      │
 * │    For "top K largest": use a MIN-heap of size K.           │
 * │    For "top K smallest": use a MAX-heap of size K.          │
 * │    The heap holds your K best candidates; the root is the   │
 * │    WORST of the K best (the "gatekeeper").                  │
 * │                                                             │
 * │ 2. Know the index formulas cold:                             │
 * │      left = 2i + 1,  right = 2i + 2,  parent = (i-1) / 2  │
 * │    You probably won't need to implement a heap from scratch, │
 * │    but explaining HOW it works shows deep understanding.    │
 * │                                                             │
 * │ 3. Heapify is O(n), NOT O(n log n). Be ready to explain     │
 * │    why: "most nodes are near the bottom and sift down a     │
 * │    short distance. The sum converges to O(n)."              │
 * │                                                             │
 * │ 4. PriorityQueue iteration is NOT sorted. Only poll()        │
 * │    gives elements in order. If you need sorted iteration,   │
 * │    use a TreeMap or sort the array.                          │
 * │                                                             │
 * │ 5. For comparators: prefer Integer.compare(a, b) over       │
 * │    a - b to avoid integer overflow. In interviews, a - b    │
 * │    is fine if you mention the overflow caveat.              │
 * └─────────────────────────────────────────────────────────────┘
 */

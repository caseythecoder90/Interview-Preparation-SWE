package linkedlists;

import java.util.*;

/**
 * ============================================================
 * SECTION 10: LINKED LIST AS QUEUE / DEQUE
 * ============================================================
 *
 * Java's LinkedList<E> implements List, Queue, AND Deque interfaces.
 * It's a doubly linked list under the hood, so all end operations are O(1).
 *
 *
 * -------------------------------------------------------
 * QUEUE (FIFO — First In, First Out)
 * -------------------------------------------------------
 *
 *   Enqueue (add to back):  offer(e)      O(1)
 *   Dequeue (remove front): poll()        O(1)
 *   Peek (see front):       peek()        O(1)
 *
 *   ⚠️ Use offer/poll/peek instead of add/remove/element.
 *   The offer/poll/peek variants return null on failure instead
 *   of throwing exceptions. Safer for interview code.
 *
 *   Visual:
 *     offer(1), offer(2), offer(3):
 *     FRONT → [1] → [2] → [3] ← BACK
 *
 *     poll() returns 1:
 *     FRONT → [2] → [3] ← BACK
 *
 *
 * -------------------------------------------------------
 * DEQUE (Double-Ended Queue — pronounced "deck")
 * -------------------------------------------------------
 *
 *   Insert at front:   offerFirst(e)   O(1)
 *   Insert at back:    offerLast(e)    O(1)
 *   Remove from front: pollFirst()     O(1)
 *   Remove from back:  pollLast()      O(1)
 *   Peek at front:     peekFirst()     O(1)
 *   Peek at back:      peekLast()      O(1)
 *
 *   A Deque can act as BOTH a Queue and a Stack:
 *     Queue (FIFO): offerLast + pollFirst
 *     Stack (LIFO): offerFirst + pollFirst  (or offerLast + pollLast)
 *
 *
 * -------------------------------------------------------
 * LinkedList vs ArrayDeque — WHEN TO USE WHICH
 * -------------------------------------------------------
 *
 * ArrayDeque is generally FASTER than LinkedList for Queue/Deque use:
 *   - ArrayDeque: backed by a circular array → cache-friendly, fewer allocations
 *   - LinkedList: each node is a separate heap object → poor cache locality
 *
 * When to use LinkedList:
 *   - Need null elements (ArrayDeque prohibits nulls)
 *   - Need List interface methods (get by index, ListIterator)
 *   - Need to remove from the middle frequently
 *
 * When to use ArrayDeque (DEFAULT CHOICE):
 *   - Queue or Stack functionality
 *   - Better performance in almost all cases
 *   - Lower memory overhead (no prev/next pointers per element)
 *
 * INTERVIEW RULE OF THUMB:
 *   - BFS queue:      Queue<X> q = new LinkedList<>()   ← conventional, everyone recognizes it
 *   - Stack:          Deque<X> s = new ArrayDeque<>()    ← prefer over Stack class
 *   - Both are fine in interviews. Mentioning ArrayDeque shows awareness.
 */
public class S10_QueueDeque {

    public static void main(String[] args) {

        // =============================================================
        // Queue operations
        // =============================================================
        System.out.println("=== QUEUE (FIFO) ===");

        Queue<Integer> queue = new LinkedList<>();

        queue.offer(1);  // enqueue
        queue.offer(2);
        queue.offer(3);
        System.out.println("Queue: " + queue);         // [1, 2, 3]
        System.out.println("peek: " + queue.peek());   // 1 (front, doesn't remove)
        System.out.println("poll: " + queue.poll());   // 1 (removes front)
        System.out.println("poll: " + queue.poll());   // 2
        System.out.println("Queue after polls: " + queue); // [3]
        System.out.println("size: " + queue.size());   // 1
        System.out.println("isEmpty: " + queue.isEmpty()); // false

        // poll on empty queue returns null (not exception)
        queue.poll(); // removes 3
        System.out.println("poll empty: " + queue.poll()); // null

        // =============================================================
        // Deque operations
        // =============================================================
        System.out.println("\n=== DEQUE (Double-Ended) ===");

        Deque<String> deque = new ArrayDeque<>();

        deque.offerFirst("B");  // front insert
        deque.offerFirst("A");  // front insert
        deque.offerLast("C");   // back insert
        deque.offerLast("D");   // back insert
        System.out.println("Deque: " + deque); // [A, B, C, D]

        System.out.println("peekFirst: " + deque.peekFirst());  // A
        System.out.println("peekLast:  " + deque.peekLast());   // D

        System.out.println("pollFirst: " + deque.pollFirst());  // A
        System.out.println("pollLast:  " + deque.pollLast());   // D
        System.out.println("After polls: " + deque);            // [B, C]

        // =============================================================
        // Using Deque as a Stack (preferred over java.util.Stack)
        // =============================================================
        System.out.println("\n=== DEQUE AS STACK (LIFO) ===");

        Deque<Integer> stack = new ArrayDeque<>();

        stack.push(1);  // push to front
        stack.push(2);
        stack.push(3);
        System.out.println("Stack: " + stack);          // [3, 2, 1]
        System.out.println("peek: " + stack.peek());    // 3
        System.out.println("pop:  " + stack.pop());     // 3
        System.out.println("pop:  " + stack.pop());     // 2
        System.out.println("After pops: " + stack);     // [1]

        // =============================================================
        // BFS with Queue — the bread and butter
        // =============================================================
        System.out.println("\n=== BFS PATTERN (Graph) ===");

        // Simulating BFS on a simple graph
        // Graph: 0→[1,2], 1→[3], 2→[3], 3→[]
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put(0, List.of(1, 2));
        graph.put(1, List.of(3));
        graph.put(2, List.of(3));
        graph.put(3, List.of());

        Queue<Integer> bfsQueue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        bfsQueue.offer(0);
        visited.add(0);

        System.out.print("BFS order: ");
        while (!bfsQueue.isEmpty()) {
            int node = bfsQueue.poll();
            System.out.print(node + " ");

            for (int neighbor : graph.getOrDefault(node, List.of())) {
                if (visited.add(neighbor)) { // add returns false if already present
                    bfsQueue.offer(neighbor);
                }
            }
        }
        System.out.println(); // 0 1 2 3

        // =============================================================
        // Sliding Window Maximum pattern with Deque (LC 239)
        // =============================================================
        System.out.println("\n=== MONOTONIC DEQUE PATTERN ===");
        // Deque stores indices. Front = max in current window.
        // We maintain decreasing order in the deque.
        int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
        int k = 3;
        List<Integer> maxes = new ArrayList<>();
        Deque<Integer> monoDeque = new ArrayDeque<>();

        for (int i = 0; i < nums.length; i++) {
            // Remove indices out of window
            while (!monoDeque.isEmpty() && monoDeque.peekFirst() < i - k + 1) {
                monoDeque.pollFirst();
            }
            // Remove smaller elements from back (maintain decreasing order)
            while (!monoDeque.isEmpty() && nums[monoDeque.peekLast()] <= nums[i]) {
                monoDeque.pollLast();
            }
            monoDeque.offerLast(i);

            if (i >= k - 1) {
                maxes.add(nums[monoDeque.peekFirst()]);
            }
        }
        System.out.println("Sliding window max (k=3): " + maxes);
        // [3, 3, 5, 5, 6, 7]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — QUEUE & DEQUE                              │
 * │                                                             │
 * │ 1. Use offer/poll/peek (not add/remove/element). The first  │
 * │    set returns null on failure; the second throws exceptions.│
 * │    Cleaner code, fewer try-catches.                         │
 * │                                                             │
 * │ 2. For stacks, use Deque<X> stack = new ArrayDeque<>().     │
 * │    java.util.Stack extends Vector, which is synchronized    │
 * │    and slow. The Java docs themselves recommend ArrayDeque.  │
 * │                                                             │
 * │ 3. The MONOTONIC DEQUE pattern (sliding window maximum)     │
 * │    is a common hard problem. The deque maintains a          │
 * │    decreasing sequence of candidates. Front = current max.  │
 * │                                                             │
 * │ 4. In BFS, "Queue<X> q = new LinkedList<>()" is idiomatic   │
 * │    Java. Don't overthink the implementation choice here —   │
 * │    interviewers care about the algorithm, not the Queue     │
 * │    backing.                                                 │
 * └─────────────────────────────────────────────────────────────┘
 */

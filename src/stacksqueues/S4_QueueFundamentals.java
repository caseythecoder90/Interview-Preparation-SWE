package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 4: QUEUE FUNDAMENTALS
 * ============================================================
 *
 * A QUEUE is a FIFO (First In, First Out) data structure.
 * The first element added is the first one removed.
 *
 *
 * -------------------------------------------------------
 * 4.1  WHAT IS A QUEUE?
 * -------------------------------------------------------
 *
 * Real-world analogies:
 *   - Checkout line at a store: first person in line is served first
 *   - Print queue: first document submitted prints first
 *   - Task processing: tasks are handled in submission order
 *   - Message queue (Kafka): messages consumed in order
 *
 * Why FIFO matters:
 *   FIFO is the natural ordering for anything LEVEL-BY-LEVEL or TIME-ORDERED.
 *   BFS explores nodes level by level → Queue.
 *   Events arrive in time order and should be processed in order → Queue.
 *   Rate limiter checks events in chronological order → Queue.
 *
 * Visual:
 *
 *   offer(1), offer(2), offer(3):
 *
 *     FRONT → [1] [2] [3] ← BACK
 *
 *   poll() returns 1 (first in, first out)
 *   poll() returns 2
 *   poll() returns 3
 *
 *
 * -------------------------------------------------------
 * 4.2  JAVA IMPLEMENTATIONS
 * -------------------------------------------------------
 *
 * 1. Queue<Integer> queue = new LinkedList<>();
 *    - Works fine. Each node is a heap object with prev/next pointers.
 *    - CONVENTIONAL for BFS in interviews — everyone recognizes this pattern.
 *    - Slightly worse cache performance due to pointer chasing.
 *
 * 2. Queue<Integer> queue = new ArrayDeque<>();  ← PREFERRED
 *    - Backed by a circular array. Cache-friendly, no node allocation.
 *    - Faster than LinkedList for queue operations.
 *    - Does NOT allow null elements (LinkedList does).
 *
 * 3. Deque<Integer> deque = new ArrayDeque<>();
 *    - Use when you need both-end access (offerFirst, offerLast, etc.).
 *    - Can function as both a Stack and a Queue.
 *
 * 4. BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
 *    - Thread-safe. Blocks on poll() when empty, blocks on offer() when full.
 *    - Used in producer-consumer patterns (relevant to Kafka experience).
 *    - Not for interview coding — mention in system design discussions.
 *
 * INTERVIEW CONVENTION:
 *   For BFS: Queue<X> q = new LinkedList<>() — idiomatic, universally recognized.
 *   For everything else: prefer ArrayDeque.
 *   Mentioning ArrayDeque shows you know the performance difference.
 *
 *
 * -------------------------------------------------------
 * 4.3  QUEUE API — SAFE vs UNSAFE METHODS
 * -------------------------------------------------------
 *
 * The Queue interface has TWO versions of each operation:
 *
 *   ┌────────────┬─────────────────────────┬──────────────────────────┐
 *   │ Operation  │ SAFE (returns null/false)│ UNSAFE (throws exception)│
 *   ├────────────┼─────────────────────────┼──────────────────────────┤
 *   │ Insert     │ offer(e) → false if full │ add(e) → IllegalState   │
 *   │ Remove     │ poll()   → null if empty │ remove() → NoSuchElement│
 *   │ Examine    │ peek()   → null if empty │ element() → NoSuchElement│
 *   └────────────┴─────────────────────────┴──────────────────────────┘
 *
 *   ALWAYS use the SAFE versions (offer / poll / peek) in interviews.
 *   - No try-catch needed
 *   - Cleaner code
 *   - Null return is easy to check
 *
 *
 * -------------------------------------------------------
 * 4.4  DEQUE (Double-Ended Queue — pronounced "deck")
 * -------------------------------------------------------
 *
 * A Deque allows add/remove from BOTH ends in O(1).
 *
 *   ┌──────────────────┬────────────────────────────────────┐
 *   │ Method           │ What it does                        │
 *   ├──────────────────┼────────────────────────────────────┤
 *   │ offerFirst(e)    │ Insert at front                     │
 *   │ offerLast(e)     │ Insert at back                      │
 *   │ pollFirst()      │ Remove and return front              │
 *   │ pollLast()       │ Remove and return back               │
 *   │ peekFirst()      │ Look at front (no remove)            │
 *   │ peekLast()       │ Look at back (no remove)             │
 *   └──────────────────┴────────────────────────────────────┘
 *
 * A Deque can function as BOTH:
 *   Queue (FIFO): offerLast + pollFirst
 *   Stack (LIFO): offerFirst + pollFirst (or push + pop)
 *
 * When to use Deque instead of Queue:
 *   - Sliding window problems (need to remove from both ends)
 *   - Monotonic deque (sliding window maximum)
 *   - When you need both stack and queue behavior
 */
public class S4_QueueFundamentals {

    public static void main(String[] args) {

        // =============================================================
        // Queue with LinkedList (conventional BFS pattern)
        // =============================================================
        System.out.println("=== QUEUE (LinkedList) ===");

        Queue<Integer> queue = new LinkedList<>();

        queue.offer(1);   // enqueue
        queue.offer(2);
        queue.offer(3);
        System.out.println("Queue: " + queue);              // [1, 2, 3]
        System.out.println("peek: " + queue.peek());        // 1 (front)
        System.out.println("poll: " + queue.poll());        // 1 (removes front)
        System.out.println("poll: " + queue.poll());        // 2
        System.out.println("After polls: " + queue);        // [3]
        System.out.println("size: " + queue.size());        // 1
        System.out.println("isEmpty: " + queue.isEmpty());  // false

        queue.poll(); // remove 3
        System.out.println("poll empty: " + queue.poll());  // null (safe!)
        // Compare: queue.remove() would throw NoSuchElementException

        // =============================================================
        // Queue with ArrayDeque (preferred for performance)
        // =============================================================
        System.out.println("\n=== QUEUE (ArrayDeque — preferred) ===");

        Queue<Integer> fastQueue = new ArrayDeque<>();
        fastQueue.offer(10);
        fastQueue.offer(20);
        fastQueue.offer(30);
        System.out.println("Queue: " + fastQueue);          // [10, 20, 30]
        System.out.println("poll: " + fastQueue.poll());    // 10
        System.out.println("Queue: " + fastQueue);          // [20, 30]

        // =============================================================
        // Deque operations — both ends
        // =============================================================
        System.out.println("\n=== DEQUE (Double-Ended) ===");

        Deque<String> deque = new ArrayDeque<>();

        deque.offerFirst("B");   // front
        deque.offerFirst("A");   // front
        deque.offerLast("C");    // back
        deque.offerLast("D");    // back
        System.out.println("Deque: " + deque);              // [A, B, C, D]

        System.out.println("peekFirst: " + deque.peekFirst());  // A
        System.out.println("peekLast:  " + deque.peekLast());   // D

        System.out.println("pollFirst: " + deque.pollFirst());  // A (remove front)
        System.out.println("pollLast:  " + deque.pollLast());   // D (remove back)
        System.out.println("After polls: " + deque);            // [B, C]

        // =============================================================
        // Deque as Stack
        // =============================================================
        System.out.println("\n=== DEQUE AS STACK ===");

        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);   // same as offerFirst
        stack.push(2);
        stack.push(3);
        System.out.println("Stack: " + stack);               // [3, 2, 1]
        System.out.println("pop: " + stack.pop());           // 3 (same as pollFirst)
        System.out.println("Stack: " + stack);               // [2, 1]

        // =============================================================
        // Deque as Queue
        // =============================================================
        System.out.println("\n=== DEQUE AS QUEUE ===");

        Deque<Integer> dequeAsQueue = new ArrayDeque<>();
        dequeAsQueue.offerLast(1);     // enqueue at back
        dequeAsQueue.offerLast(2);
        dequeAsQueue.offerLast(3);
        System.out.println("Queue: " + dequeAsQueue);       // [1, 2, 3]
        System.out.println("poll: " + dequeAsQueue.pollFirst()); // 1 (dequeue from front)

        // =============================================================
        // Common queue idiom: process until empty
        // =============================================================
        System.out.println("\n=== PROCESS UNTIL EMPTY ===");

        Queue<String> tasks = new LinkedList<>();
        tasks.offer("Process A");
        tasks.offer("Process B");
        tasks.offer("Process C");

        while (!tasks.isEmpty()) {
            System.out.println("Handling: " + tasks.poll());
        }
        // Handles: A, B, C (FIFO order)

        // =============================================================
        // Safe vs unsafe API comparison
        // =============================================================
        System.out.println("\n=== SAFE vs UNSAFE API ===");

        Queue<Integer> emptyQ = new ArrayDeque<>();

        // SAFE: returns null/false
        System.out.println("peek on empty: " + emptyQ.peek());     // null
        System.out.println("poll on empty: " + emptyQ.poll());     // null

        // UNSAFE: would throw
        try {
            emptyQ.element(); // throws NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("element() on empty: " + e.getClass().getSimpleName());
        }
        try {
            emptyQ.remove(); // throws NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("remove() on empty:  " + e.getClass().getSimpleName());
        }
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — QUEUE FUNDAMENTALS                          │
 * │                                                             │
 * │ 1. ALWAYS use offer/poll/peek (not add/remove/element).     │
 * │    The safe versions return null on failure instead of       │
 * │    throwing. Cleaner code, fewer edge cases.                │
 * │                                                             │
 * │ 2. For BFS: "Queue<X> q = new LinkedList<>()" is idiomatic. │
 * │    Don't overthink it — interviewers care about the         │
 * │    algorithm, not the Queue implementation.                 │
 * │                                                             │
 * │ 3. Know that Deque can be BOTH a Stack and a Queue:         │
 * │      Stack: push/pop (= addFirst/removeFirst)               │
 * │      Queue: offerLast/pollFirst                             │
 * │    This occasionally comes up: "implement a stack and       │
 * │    queue using the same data structure."                    │
 * │                                                             │
 * │ 4. If asked "LinkedList or ArrayDeque?", say:               │
 * │    "ArrayDeque — better cache locality and no per-node      │
 * │    allocation overhead. LinkedList only if I need nulls     │
 * │    or mid-list operations."                                 │
 * └─────────────────────────────────────────────────────────────┘
 */

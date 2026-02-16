package stacksqueues;

/**
 * ============================================================
 * SECTION 6: CIRCULAR QUEUE / RING BUFFER
 * ============================================================
 *
 * A circular queue (ring buffer) is a fixed-size queue backed by an array.
 * Head and tail pointers wrap around, reusing space left by dequeued elements.
 *
 *
 * -------------------------------------------------------
 * 6.1  WHY CIRCULAR?
 * -------------------------------------------------------
 *
 * In a naive array-based queue, dequeue leaves unused space at the front:
 *
 *   After enqueue(A), enqueue(B), enqueue(C), dequeue(), dequeue():
 *
 *     [ ] [ ] [C] [ ] [ ]     ← wasted space at [0], [1]
 *           ^head  ^tail
 *
 * A circular queue wraps the pointers around:
 *
 *     [D] [E] [C] [ ] [ ]     ← head and tail wrap past the end
 *               ^head  ^tail (pointing to next empty)
 *
 * This means the array NEVER wastes space until full.
 *
 *
 * -------------------------------------------------------
 * 6.2  HOW IT WORKS
 * -------------------------------------------------------
 *
 * Array of fixed capacity k:
 *   - head: index of the front element
 *   - tail: index where next element will be inserted
 *   - size: number of elements currently in the queue
 *
 * Wrapping uses modulo: (index + 1) % capacity
 *
 * Example (capacity = 4):
 *
 *   Initial:        [ ][ ][ ][ ]    head=0, tail=0, size=0
 *   enqueue(1):     [1][ ][ ][ ]    head=0, tail=1, size=1
 *   enqueue(2):     [1][2][ ][ ]    head=0, tail=2, size=2
 *   enqueue(3):     [1][2][3][ ]    head=0, tail=3, size=3
 *   dequeue() → 1:  [ ][2][3][ ]    head=1, tail=3, size=2
 *   dequeue() → 2:  [ ][ ][3][ ]    head=2, tail=3, size=1
 *   enqueue(4):     [ ][ ][3][4]    head=2, tail=0 (wrapped!), size=2
 *   enqueue(5):     [5][ ][3][4]    head=2, tail=1 (wrapped!), size=3
 *
 *
 * -------------------------------------------------------
 * 6.3  WHEN IS IT USEFUL?
 * -------------------------------------------------------
 *
 * - Bounded buffers (producer-consumer with fixed capacity)
 * - Streaming data (keep last N elements)
 * - Connection pools (fixed number of connections)
 * - OS: keyboard buffer, network packet buffers
 * - Interview: Design Circular Queue (LC 622)
 *
 * Less likely to be asked directly, but demonstrates systems knowledge.
 * Interviewers appreciate if you mention: "Under the hood, ArrayDeque
 * uses a circular array — I could implement one if needed."
 */
public class S6_CircularQueue {

    // =============================================================
    // 6.4  IMPLEMENTATION — Design Circular Queue (LC 622)
    // =============================================================

    static class MyCircularQueue {
        private int[] data;
        private int head;
        private int tail;
        private int size;
        private int capacity;

        public MyCircularQueue(int k) {
            this.data = new int[k];
            this.head = 0;
            this.tail = 0;
            this.size = 0;
            this.capacity = k;
        }

        /** Insert element at the rear. Returns true if successful. */
        public boolean enQueue(int value) {
            if (isFull()) return false;
            data[tail] = value;
            tail = (tail + 1) % capacity; // wrap around
            size++;
            return true;
        }

        /** Delete element from the front. Returns true if successful. */
        public boolean deQueue() {
            if (isEmpty()) return false;
            head = (head + 1) % capacity; // wrap around
            size--;
            return true;
        }

        /** Get the front item. Returns -1 if empty. */
        public int Front() {
            if (isEmpty()) return -1;
            return data[head];
        }

        /** Get the rear item. Returns -1 if empty. */
        public int Rear() {
            if (isEmpty()) return -1;
            // tail points to next empty slot, so rear is (tail - 1)
            // Use modulo to handle wrap-around when tail = 0
            return data[(tail - 1 + capacity) % capacity];
        }

        public boolean isEmpty() {
            return size == 0;
        }

        public boolean isFull() {
            return size == capacity;
        }

        /** Debug: show the array contents and pointers. */
        public void debug() {
            System.out.print("  [");
            for (int i = 0; i < capacity; i++) {
                if (i > 0) System.out.print(", ");
                // Check if this slot is occupied
                boolean occupied;
                if (size == 0) {
                    occupied = false;
                } else if (head < tail) {
                    occupied = i >= head && i < tail;
                } else { // wrapped or full
                    occupied = i >= head || i < tail;
                    if (size == capacity) occupied = true;
                }
                System.out.print(occupied ? String.valueOf(data[i]) : "_");
            }
            System.out.println("]  head=" + head + " tail=" + tail + " size=" + size);
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== CIRCULAR QUEUE ===");

        MyCircularQueue cq = new MyCircularQueue(4);

        System.out.println("enqueue(1): " + cq.enQueue(1)); cq.debug();
        System.out.println("enqueue(2): " + cq.enQueue(2)); cq.debug();
        System.out.println("enqueue(3): " + cq.enQueue(3)); cq.debug();
        System.out.println("enqueue(4): " + cq.enQueue(4)); cq.debug();
        System.out.println("enqueue(5): " + cq.enQueue(5)); // false — full
        cq.debug();

        System.out.println("\nFront: " + cq.Front()); // 1
        System.out.println("Rear:  " + cq.Rear());   // 4

        System.out.println("\ndequeue(): " + cq.deQueue()); cq.debug(); // removes 1
        System.out.println("dequeue(): " + cq.deQueue()); cq.debug(); // removes 2

        System.out.println("\nFront: " + cq.Front()); // 3

        // Now tail wraps around
        System.out.println("\nenqueue(5): " + cq.enQueue(5)); cq.debug(); // wraps!
        System.out.println("enqueue(6): " + cq.enQueue(6)); cq.debug(); // wraps!

        System.out.println("\nFront: " + cq.Front()); // 3
        System.out.println("Rear:  " + cq.Rear());   // 6

        System.out.println("\n=== FULL CYCLE ===");
        MyCircularQueue cq2 = new MyCircularQueue(3);
        cq2.enQueue(10); cq2.enQueue(20); cq2.enQueue(30);
        System.out.println("Full: " + cq2.isFull());        // true
        System.out.println("Front: " + cq2.Front());        // 10
        cq2.deQueue();
        System.out.println("After dequeue, Front: " + cq2.Front()); // 20
        cq2.enQueue(40); // wraps around
        cq2.debug();
        System.out.println("Rear: " + cq2.Rear());          // 40
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — CIRCULAR QUEUE                              │
 * │                                                             │
 * │ 1. The KEY operation is modulo wrapping:                     │
 * │      tail = (tail + 1) % capacity                           │
 * │    This one line is the difference between a circular and   │
 * │    non-circular queue.                                      │
 * │                                                             │
 * │ 2. For Rear(), remember tail points to the NEXT EMPTY slot. │
 * │    So rear = (tail - 1 + capacity) % capacity. The          │
 * │    "+ capacity" handles the case when tail = 0.             │
 * │                                                             │
 * │ 3. There are two ways to distinguish full from empty:        │
 * │    a. Track a separate `size` variable (simplest)            │
 * │    b. Waste one slot (full when (tail+1)%cap == head)       │
 * │    Use option (a) in interviews — it's clearer.             │
 * │                                                             │
 * │ 4. Mention: "ArrayDeque uses a circular array internally.   │
 * │    The difference is ArrayDeque RESIZES when full, while a  │
 * │    circular queue has a FIXED capacity."                    │
 * └─────────────────────────────────────────────────────────────┘
 */

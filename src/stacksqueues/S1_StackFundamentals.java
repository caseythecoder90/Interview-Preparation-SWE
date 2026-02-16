package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 1: STACK FUNDAMENTALS
 * ============================================================
 *
 * A STACK is a LIFO (Last In, First Out) data structure.
 * The last element added is the first one removed.
 *
 *
 * -------------------------------------------------------
 * 1.1  WHAT IS A STACK?
 * -------------------------------------------------------
 *
 * Real-world analogies:
 *   - Stack of plates: you always take from the top
 *   - Browser back button: most recent page is visited first
 *   - Undo history (Ctrl+Z): undoes the LAST action
 *   - Call stack: last function called is the first to return
 *
 * Why LIFO matters:
 *   LIFO is the natural ordering for anything with NESTING or MATCHING.
 *   When you open a parenthesis, the MOST RECENT open paren must close first.
 *   When you call a function, the MOST RECENT call must return first.
 *   This is why stacks appear in parsing, recursion, and tree traversals.
 *
 * Visual:
 *
 *   push(1), push(2), push(3):
 *
 *     | 3 |  <-- top (last in, first out)
 *     | 2 |
 *     | 1 |
 *     +---+
 *
 *   pop() returns 3
 *   pop() returns 2
 *   pop() returns 1
 *
 *
 * -------------------------------------------------------
 * 1.2  JAVA IMPLEMENTATIONS
 * -------------------------------------------------------
 *
 * PREFERRED (use this in interviews):
 *
 *   Deque<Integer> stack = new ArrayDeque<>();
 *
 * WHY ArrayDeque?
 *   1. ArrayDeque is backed by a resizable circular array.
 *      - Cache-friendly: contiguous memory, no pointer chasing.
 *      - No per-element allocation: no Node objects on the heap.
 *   2. Stack operations (push/pop/peek) are O(1) amortized.
 *   3. The Java docs themselves say: "This class is likely to be faster
 *      than Stack when used as a stack."
 *
 * LEGACY (do NOT use in interviews):
 *
 *   Stack<Integer> stack = new Stack<>();
 *
 * WHY NOT Stack class?
 *   1. Stack extends Vector, which is SYNCHRONIZED on every operation.
 *      This adds unnecessary overhead when you don't need thread safety.
 *   2. Vector allows random access (get(i)), which violates stack abstraction.
 *   3. It's a legacy class from Java 1.0 — the Java docs recommend ArrayDeque.
 *   4. Using Stack in an interview signals you haven't kept up with modern Java.
 *
 *
 * -------------------------------------------------------
 * 1.3  STACK API
 * -------------------------------------------------------
 *
 *   Method      │ What it does                      │ On empty stack
 *   ────────────┼───────────────────────────────────┼──────────────────
 *   push(e)     │ Add element to top                │ Always works
 *   pop()       │ Remove and return top              │ Throws NoSuchElementException
 *   peek()      │ Return top without removing        │ Returns null
 *   isEmpty()   │ Check if stack is empty            │ Returns true
 *   size()      │ Number of elements                 │ Returns 0
 *
 *   NOTE: When using Deque as a stack:
 *     push(e) = addFirst(e)    (adds to front/top)
 *     pop()   = removeFirst()  (removes from front/top)
 *     peek()  = peekFirst()    (looks at front/top)
 *
 *
 * -------------------------------------------------------
 * 1.4  HOW ARRAYDEQUE WORKS INTERNALLY
 * -------------------------------------------------------
 *
 * ArrayDeque uses a CIRCULAR ARRAY (ring buffer) with head and tail pointers.
 *
 *   Capacity = 8 (always a power of 2)
 *
 *   After push(A), push(B), push(C):
 *
 *     Index:  0   1   2   3   4   5   6   7
 *            [C] [B] [A] [ ] [ ] [ ] [ ] [ ]
 *              ^head          ^tail
 *
 *   push() decrements head (wrapping around with bitwise AND):
 *     head = (head - 1) & (array.length - 1)
 *
 *   pop() reads elements[head], then increments head:
 *     head = (head + 1) & (array.length - 1)
 *
 *   When the array fills up (head == tail), it DOUBLES the capacity
 *   and copies all elements to a new array. This makes push O(1) AMORTIZED.
 *
 * WHY is ArrayDeque faster than LinkedList for stack use?
 *   - ArrayDeque: elements are adjacent in memory → CPU cache hits
 *   - LinkedList: each node is a separate heap object → cache misses
 *   - ArrayDeque: no Node wrapper per element → less memory overhead
 *   - LinkedList: each element needs a Node with prev + next pointers (16+ bytes overhead)
 */
public class S1_StackFundamentals {

    public static void main(String[] args) {

        // =============================================================
        // Preferred: ArrayDeque as Stack
        // =============================================================
        System.out.println("=== ARRAYDEQUE AS STACK (PREFERRED) ===");

        Deque<Integer> stack = new ArrayDeque<>();

        stack.push(1);
        stack.push(2);
        stack.push(3);
        System.out.println("Stack: " + stack);              // [3, 2, 1]
        System.out.println("peek: " + stack.peek());        // 3
        System.out.println("pop:  " + stack.pop());         // 3
        System.out.println("pop:  " + stack.pop());         // 2
        System.out.println("After pops: " + stack);         // [1]
        System.out.println("size: " + stack.size());        // 1
        System.out.println("isEmpty: " + stack.isEmpty());  // false

        stack.pop(); // remove last element
        System.out.println("isEmpty: " + stack.isEmpty());  // true
        System.out.println("peek empty: " + stack.peek());  // null (NOT exception)

        // =============================================================
        // Legacy: Stack class (DON'T USE — shown for awareness only)
        // =============================================================
        System.out.println("\n=== LEGACY STACK CLASS (AVOID) ===");

        Stack<Integer> legacyStack = new Stack<>();
        legacyStack.push(10);
        legacyStack.push(20);
        System.out.println("Legacy stack: " + legacyStack); // [10, 20]
        // BAD: Stack allows random access — violates LIFO abstraction
        System.out.println("get(0): " + legacyStack.get(0)); // 10 ← shouldn't be possible
        // This is why Stack class is bad: it exposes Vector's full API

        // =============================================================
        // Common stack idiom: process until empty
        // =============================================================
        System.out.println("\n=== PROCESS UNTIL EMPTY ===");

        Deque<String> tasks = new ArrayDeque<>();
        tasks.push("task C");
        tasks.push("task B");
        tasks.push("task A");

        while (!tasks.isEmpty()) {
            System.out.println("Processing: " + tasks.pop());
        }
        // Processes: task A, task B, task C (LIFO order)

        // =============================================================
        // Stack of strings — reverse a word
        // =============================================================
        System.out.println("\n=== REVERSE WITH STACK ===");

        String word = "hello";
        Deque<Character> charStack = new ArrayDeque<>();
        for (char c : word.toCharArray()) {
            charStack.push(c);
        }
        StringBuilder reversed = new StringBuilder();
        while (!charStack.isEmpty()) {
            reversed.append(charStack.pop());
        }
        System.out.println(word + " reversed: " + reversed); // olleh
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — STACK FUNDAMENTALS                         │
 * │                                                             │
 * │ 1. ALWAYS write: Deque<X> stack = new ArrayDeque<>()        │
 * │    If an interviewer sees you use Stack<X>, they may think  │
 * │    you're unfamiliar with modern Java best practices.       │
 * │                                                             │
 * │ 2. Remember: peek() returns null on empty, pop() THROWS.   │
 * │    Always check isEmpty() before pop() in interview code.   │
 * │                                                             │
 * │ 3. "When should I use a stack?" → Ask yourself:             │
 * │    "Does this problem involve NESTING, MATCHING, or         │
 * │    MOST-RECENT ordering?" If yes → stack.                   │
 * │                                                             │
 * │ 4. ArrayDeque does NOT allow null elements. This is fine —  │
 * │    you should never need nulls in a stack.                  │
 * └─────────────────────────────────────────────────────────────┘
 */

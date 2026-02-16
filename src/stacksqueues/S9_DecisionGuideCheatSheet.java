package stacksqueues;

/**
 * ============================================================
 * SECTION 9: DECISION GUIDE & COMPLEXITY CHEAT SHEET
 * ============================================================
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ WHEN TO USE WHAT — DECISION GUIDE                                                           │
 * ├─────────────────────────────────────────────┬──────────────────────────┬─────────────────────┤
 * │ I need to...                                │ Use                      │ Why                 │
 * ├─────────────────────────────────────────────┼──────────────────────────┼─────────────────────┤
 * │ Match/balance brackets or delimiters        │ Stack                    │ LIFO = nesting      │
 * │ Parse nested expressions / markdown         │ Stack                    │ Track nested state  │
 * │ Find next greater/smaller element           │ Monotonic Stack          │ O(n) total work     │
 * │ Convert recursion to iteration              │ Explicit Stack           │ Simulates call stack│
 * │ Get min/max in O(1) alongside a stack       │ Two Stacks (MinStack)    │ Track min per level │
 * │ Undo/redo history                           │ Two Stacks               │ Undo=pop, redo=push │
 * ├─────────────────────────────────────────────┼──────────────────────────┼─────────────────────┤
 * │ BFS tree traversal (level by level)         │ Queue                    │ FIFO = level order  │
 * │ BFS on a grid (shortest path unweighted)    │ Queue                    │ BFS = shortest path │
 * │ Process items in arrival order              │ Queue                    │ FIFO = fairness     │
 * │ Rate limit by time window                   │ Queue of timestamps      │ Expire from front   │
 * │ Sliding window operations (both ends)       │ Deque                    │ Remove from both    │
 * │ Sliding window maximum/minimum              │ Monotonic Deque          │ O(n) total work     │
 * │ Fixed-size buffer / bounded queue           │ Circular Queue           │ No wasted space     │
 * ├─────────────────────────────────────────────┼──────────────────────────┼─────────────────────┤
 * │ Find the K largest/smallest elements        │ Min/Max heap of size K   │ O(n log k)          │
 * │ Maintain a running median                   │ Two heaps (max + min)    │ O(log n) per insert │
 * │ Merge K sorted sequences                    │ Min-heap of size K       │ O(n log k)          │
 * │ Process items by priority                   │ PriorityQueue            │ O(log n) per op     │
 * │ Find shortest path (weighted graph)         │ PriorityQueue (Dijkstra) │ Greedy + heap       │
 * │ Sort without full sort (partial sort)       │ Heap                     │ O(n + k log n)      │
 * └─────────────────────────────────────────────┴──────────────────────────┴─────────────────────┘
 *
 *
 * ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ COMPLEXITY CHEAT SHEET                                                                       │
 * ├────────────────────────┬──────────────────────┬───────────┬──────────┬───────────────────────┤
 * │ Data Structure         │ Operation            │ Average   │ Worst    │ Notes                 │
 * ├────────────────────────┼──────────────────────┼───────────┼──────────┼───────────────────────┤
 * │ STACK                  │                      │           │          │                       │
 * │ Stack (ArrayDeque)     │ push(e)              │ O(1)*     │ O(n)     │ *Amortized — resize   │
 * │ Stack (ArrayDeque)     │ pop()                │ O(1)      │ O(1)     │                       │
 * │ Stack (ArrayDeque)     │ peek()               │ O(1)      │ O(1)     │                       │
 * │ Stack (ArrayDeque)     │ isEmpty() / size()   │ O(1)      │ O(1)     │                       │
 * │ Stack (ArrayDeque)     │ Space                │           │ O(n)     │                       │
 * ├────────────────────────┼──────────────────────┼───────────┼──────────┼───────────────────────┤
 * │ QUEUE                  │                      │           │          │                       │
 * │ Queue (ArrayDeque)     │ offer(e)             │ O(1)*     │ O(n)     │ *Amortized — resize   │
 * │ Queue (ArrayDeque)     │ poll()               │ O(1)      │ O(1)     │                       │
 * │ Queue (ArrayDeque)     │ peek()               │ O(1)      │ O(1)     │                       │
 * │ Queue (LinkedList)     │ offer / poll / peek  │ O(1)      │ O(1)     │ No resize needed      │
 * │ Queue                  │ Space                │           │ O(n)     │                       │
 * ├────────────────────────┼──────────────────────┼───────────┼──────────┼───────────────────────┤
 * │ DEQUE                  │                      │           │          │                       │
 * │ Deque (ArrayDeque)     │ offerFirst/Last      │ O(1)*     │ O(n)     │ *Amortized            │
 * │ Deque (ArrayDeque)     │ pollFirst/Last       │ O(1)      │ O(1)     │                       │
 * │ Deque (ArrayDeque)     │ peekFirst/Last       │ O(1)      │ O(1)     │                       │
 * ├────────────────────────┼──────────────────────┼───────────┼──────────┼───────────────────────┤
 * │ PRIORITY QUEUE (HEAP)  │                      │           │          │                       │
 * │ PriorityQueue          │ offer(e)             │ O(log n)  │ O(log n) │ Sift up               │
 * │ PriorityQueue          │ poll()               │ O(log n)  │ O(log n) │ Sift down             │
 * │ PriorityQueue          │ peek()               │ O(1)      │ O(1)     │ Root element          │
 * │ PriorityQueue          │ remove(Object)       │ O(n)      │ O(n)     │ Linear search!        │
 * │ PriorityQueue          │ contains(Object)     │ O(n)      │ O(n)     │ Linear search!        │
 * │ PriorityQueue          │ heapify (build)      │ O(n)      │ O(n)     │ NOT O(n log n)        │
 * │ PriorityQueue          │ Space                │           │ O(n)     │                       │
 * ├────────────────────────┼──────────────────────┼───────────┼──────────┼───────────────────────┤
 * │ CIRCULAR QUEUE         │                      │           │          │                       │
 * │ Circular Queue         │ enqueue / dequeue    │ O(1)      │ O(1)     │ Fixed capacity        │
 * │ Circular Queue         │ front / rear / peek  │ O(1)      │ O(1)     │                       │
 * │ Circular Queue         │ Space                │           │ O(k)     │ k = fixed capacity    │
 * └────────────────────────┴──────────────────────┴───────────┴──────────┴───────────────────────┘
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ KEY COMPLEXITY THEMES TO REMEMBER                                   │
 * │                                                                     │
 * │ 1. STACK & QUEUE are O(1) for their core operations.               │
 * │    They're the fastest data structures for their use cases.        │
 * │    The only "cost" is that they restrict ACCESS: you can only      │
 * │    look at the top (stack) or front (queue).                       │
 * │                                                                     │
 * │ 2. PRIORITYQUEUE is O(log n) for insert/remove, O(1) for peek.    │
 * │    This is strictly better than sorting when you need the          │
 * │    min/max repeatedly. Full sort = O(n log n) once. Heap =        │
 * │    O(log n) per operation, and often k << n.                       │
 * │                                                                     │
 * │ 3. REMOVE(Object) on PriorityQueue is O(n).                       │
 * │    This catches people. If you need O(log n) arbitrary removal,   │
 * │    use a TreeMap (which is a balanced BST, not a heap).           │
 * │                                                                     │
 * │ 4. HEAPIFY is O(n), not O(n log n).                               │
 * │    Building a heap from an array is cheaper than inserting         │
 * │    elements one by one. This matters for algorithms like           │
 * │    heap sort where you start with all data available.             │
 * │                                                                     │
 * │ 5. MONOTONIC STACK/DEQUE achieves O(n) total.                     │
 * │    Even though there's a while loop inside the for loop,          │
 * │    each element is pushed and popped AT MOST ONCE.                │
 * │    Total pushes + total pops = O(n). Amortized O(1) per element.  │
 * │                                                                     │
 * │ 6. TOP K PATTERN: O(n log k) with a heap of size k.              │
 * │    Much better than sorting O(n log n) when k << n.               │
 * │    Also better than O(nk) naive approach.                         │
 * │                                                                     │
 * │ 7. ARRAYDEQUE vs LINKEDLIST for Queue:                            │
 * │    ArrayDeque: cache-friendly, fewer allocations, amortized O(1)  │
 * │    LinkedList: guaranteed O(1) (no resize), supports null          │
 * │    In practice, ArrayDeque is faster. In interviews, either works.│
 * └─────────────────────────────────────────────────────────────────────┘
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ FULLSTORY & VISA — WHAT TO EXPECT                                   │
 * │                                                                     │
 * │ FULLSTORY confirmed patterns:                                       │
 * │   - Rate Limiter → Queue (sliding window of timestamps)            │
 * │   - Message Queue → Queue or PriorityQueue                         │
 * │   - Task Scheduler → PriorityQueue                                 │
 * │   - Markdown Parser → Stack (nested formatting)                    │
 * │   - BFS tree traversal → Queue                                     │
 * │   - Iterative tree traversal → Stack                               │
 * │                                                                     │
 * │ COMMON VISA patterns:                                               │
 * │   - Top K problems → PriorityQueue                                 │
 * │   - Valid Parentheses → Stack                                      │
 * │   - BFS shortest path → Queue                                     │
 * │   - Expression evaluation → Stack                                  │
 * │   - Merge K sorted → PriorityQueue                                │
 * └─────────────────────────────────────────────────────────────────────┘
 */
public class S9_DecisionGuideCheatSheet {
    // This file is a reference document — no executable code needed.
    // Keep it open during practice sessions.
}

package corepatterns;

/**
 * ============================================================
 * SECTION 10: MASTER DECISION GUIDE & COMPLEXITY REFERENCE
 * ============================================================
 *
 *
 * ┌───────────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ MASTER DECISION TABLE — "What data structure / pattern should I use?"                              │
 * ├──────────────────────────────────────────────────┬────────────────────────────┬────────────────────┤
 * │ Problem type                                     │ Data structure / pattern   │ Why                │
 * ├──────────────────────────────────────────────────┼────────────────────────────┼────────────────────┤
 * │ O(1) lookup by key                               │ HashMap                    │ Direct hash access │
 * │ Need sorted keys or range queries                │ TreeMap                    │ Red-Black Tree     │
 * │ Need nearest key (floor/ceiling)                 │ TreeMap                    │ O(log n) neighbor  │
 * │ Need sorted set (no values)                      │ TreeSet                    │ Sorted collection  │
 * │ O(1) insert/remove at known position             │ Doubly Linked List         │ Pointer updates    │
 * │ LIFO processing, nesting, matching               │ Stack (ArrayDeque)         │ Last in, first out │
 * │ FIFO processing, BFS, level-order                │ Queue (LinkedList)         │ First in, first out│
 * │ Top K, running median, priority                  │ PriorityQueue / Heap       │ O(log n) min/max   │
 * │ O(1) everything (LRU Cache)                      │ HashMap + Doubly LL        │ Best of both       │
 * ├──────────────────────────────────────────────────┼────────────────────────────┼────────────────────┤
 * │ Sorted array pair finding                        │ Two pointers (opposite)    │ O(n) not O(n^2)    │
 * │ In-place array rearranging                       │ Two pointers (same dir)    │ Read/write pointers│
 * │ Sorted array squares                             │ Two pointers (opposite)    │ Fill from ends     │
 * │ Longest/shortest subarray with condition         │ Sliding window (variable)  │ Expand/contract    │
 * │ Subarray of fixed size K                         │ Sliding window (fixed)     │ Slide across       │
 * │ Frequency in a substring/window                  │ Sliding window + HashMap   │ Track counts       │
 * ├──────────────────────────────────────────────────┼────────────────────────────┼────────────────────┤
 * │ Shortest path (unweighted)                       │ BFS + Queue                │ Level = distance   │
 * │ Shortest path (weighted)                         │ Dijkstra + PriorityQueue   │ Greedy + heap      │
 * │ Explore all paths / all solutions                │ DFS + recursion/Stack      │ Exhaustive search  │
 * │ Tree traversal                                   │ DFS (recursion)            │ Natural recursion  │
 * │ Level-order tree traversal                       │ BFS + Queue                │ Level = queue round│
 * │ Connected components                             │ DFS or BFS                 │ Either works       │
 * │ Multi-source spreading (Rotting Oranges)         │ Multi-source BFS           │ All sources at once│
 * ├──────────────────────────────────────────────────┼────────────────────────────┼────────────────────┤
 * │ Parse structured text                            │ State machine + StringBuilder│ Char by char     │
 * │ Nested structures (brackets, tags)               │ Stack                      │ Push/pop context   │
 * │ Range queries by time/key                        │ TreeMap subMap             │ O(log n + k)       │
 * │ Rate limiting by time window                     │ Queue of timestamps        │ Expire from front  │
 * │ Task scheduling by priority                      │ PriorityQueue              │ Always pick best   │
 * └──────────────────────────────────────────────────┴────────────────────────────┴────────────────────┘
 *
 *
 * ┌───────────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ COMPLEXITY REFERENCE                                                                               │
 * ├──────────────────────────┬───────────────────────┬───────────┬──────────┬──────────────────────────┤
 * │ Data Structure           │ Operation             │ Average   │ Worst    │ Notes                    │
 * ├──────────────────────────┼───────────────────────┼───────────┼──────────┼──────────────────────────┤
 * │ HashMap                  │ get/put/remove        │ O(1)      │ O(n)*    │ *O(log n) Java 8+ trees  │
 * │ HashMap                  │ containsValue         │ O(n)      │ O(n)     │ Must scan all values     │
 * │ TreeMap                  │ get/put/remove        │ O(log n)  │ O(log n) │ Red-Black Tree           │
 * │ TreeMap                  │ floor/ceiling         │ O(log n)  │ O(log n) │                          │
 * │ TreeMap                  │ subMap iteration      │ O(log n+k)│ O(log n+k)│ k = results in range    │
 * │ TreeSet                  │ add/remove/contains   │ O(log n)  │ O(log n) │                          │
 * │ HashSet                  │ add/remove/contains   │ O(1)      │ O(n)     │                          │
 * ├──────────────────────────┼───────────────────────┼───────────┼──────────┼──────────────────────────┤
 * │ Stack (ArrayDeque)       │ push/pop/peek         │ O(1)*     │ O(n)     │ *Amortized               │
 * │ Queue (ArrayDeque)       │ offer/poll/peek       │ O(1)*     │ O(n)     │ *Amortized               │
 * │ PriorityQueue            │ offer/poll            │ O(log n)  │ O(log n) │                          │
 * │ PriorityQueue            │ peek                  │ O(1)      │ O(1)     │                          │
 * │ PriorityQueue            │ remove(Object)        │ O(n)      │ O(n)     │ Linear search!           │
 * ├──────────────────────────┼───────────────────────┼───────────┼──────────┼──────────────────────────┤
 * │ Singly Linked List       │ Insert/delete at head │ O(1)      │ O(1)     │                          │
 * │ Singly Linked List       │ Search/delete by value│ O(n)      │ O(n)     │                          │
 * │ Doubly Linked List       │ Insert/remove (given) │ O(1)      │ O(1)     │ Given node reference     │
 * │ Doubly Linked List       │ Search                │ O(n)      │ O(n)     │                          │
 * │ HashMap + DLL (LRU)      │ get/put/evict         │ O(1)      │ O(1)     │ Combined structure       │
 * ├──────────────────────────┼───────────────────────┼───────────┼──────────┼──────────────────────────┤
 * │ ALGORITHM PATTERNS       │                       │           │          │                          │
 * │ Two pointers             │ Single pass           │ O(n)      │ O(n)     │ Space: O(1)              │
 * │ Sliding window (fixed)   │ Single pass           │ O(n)      │ O(n)     │ Space: O(1) or O(k)      │
 * │ Sliding window (variable)│ Single pass           │ O(n)      │ O(n)     │ Space: O(k) for map      │
 * │ BFS (graph/grid)         │ Full traversal        │ O(V+E)    │ O(V+E)  │ Space: O(V)              │
 * │ DFS (graph/grid)         │ Full traversal        │ O(V+E)    │ O(V+E)  │ Space: O(V) stack        │
 * │ Monotonic stack/deque    │ Single pass           │ O(n)      │ O(n)     │ Each elem push+pop once  │
 * │ Top K with heap          │ Process all elements  │ O(n log k)│ O(n log k)│ Heap of size K          │
 * │ Binary search            │ Find target           │ O(log n)  │ O(log n) │ Sorted input required    │
 * └──────────────────────────┴───────────────────────┴───────────┴──────────┴──────────────────────────┘
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ FULLSTORY & VISA — CONFIRMED/EXPECTED PATTERNS                       │
 * │                                                                     │
 * │ FULLSTORY:                                                          │
 * │   - Log Storage System  → TreeMap (range queries by timestamp)     │
 * │   - Markdown Parser     → State machine + stack (nested formatting)│
 * │   - Rate Limiter        → Queue (sliding window of timestamps)     │
 * │   - LRU Cache           → HashMap + Doubly Linked List             │
 * │   - Task Scheduler      → PriorityQueue                           │
 * │   - Sorted Squares      → Two pointers (opposite ends)            │
 * │   - BFS/DFS traversals  → Queue / Stack                           │
 * │                                                                     │
 * │ VISA:                                                               │
 * │   - Top K problems      → PriorityQueue                           │
 * │   - Valid Parentheses   → Stack                                    │
 * │   - Two Sum variants    → HashMap or Two Pointers                  │
 * │   - Sliding window      → HashMap + two pointers                   │
 * │   - BFS shortest path   → Queue                                    │
 * │   - Tree traversals     → DFS recursion                           │
 * │   - Merge K Sorted      → PriorityQueue                           │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ PATTERN RECOGNITION CHEAT SHEET — Read the Problem, Pick the Tool   │
 * │                                                                     │
 * │ "Find the K largest/smallest..."     → Min/Max heap of size K      │
 * │ "Shortest path / minimum steps..."   → BFS                         │
 * │ "All possible / every combination..."→ DFS + backtracking          │
 * │ "Longest/shortest subarray..."       → Sliding window              │
 * │ "Sorted array, find pair..."         → Two pointers                │
 * │ "Match/balance brackets..."          → Stack                       │
 * │ "Next greater/smaller element..."    → Monotonic stack             │
 * │ "In a time range / between keys..."  → TreeMap subMap              │
 * │ "Most recent / least recently..."    → HashMap + DLL               │
 * │ "Parse this string format..."        → State machine               │
 * │ "Process by priority..."             → PriorityQueue               │
 * │ "Count frequency of..."             → HashMap                      │
 * └─────────────────────────────────────────────────────────────────────┘
 */
public class S10_DecisionGuideCheatSheet {
    // This file is a reference document — no executable code needed.
    // Keep it open during practice sessions.
}

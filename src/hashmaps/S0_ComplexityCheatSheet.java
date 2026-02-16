package hashmaps;

/**
 * ============================================================
 * PART C: HASHMAP & LINKED LIST COMPLEXITY CHEAT SHEET
 * ============================================================
 *
 * ┌───────────────────────────┬──────────────────────────┬──────────┬──────────┬───────────────────────────────────────────┐
 * │ Data Structure            │ Operation                │ Average  │ Worst    │ Notes                                     │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ HASHMAP                   │                          │          │          │                                           │
 * │ HashMap                   │ get(key)                 │ O(1)     │ O(n)*    │ *O(log n) in Java 8+ due to treeification │
 * │ HashMap                   │ put(key, value)          │ O(1)†    │ O(n)     │ †Amortized — includes rare O(n) resizes   │
 * │ HashMap                   │ remove(key)              │ O(1)     │ O(n)     │ Same as get — find bucket, walk chain     │
 * │ HashMap                   │ containsKey(key)         │ O(1)     │ O(n)     │ Same as get                               │
 * │ HashMap                   │ containsValue(value)     │ O(n)     │ O(n)     │ Must scan ALL buckets and chains          │
 * │ HashMap                   │ size()                   │ O(1)     │ O(1)     │ Maintained as a counter                   │
 * │ HashMap                   │ resize/rehash            │ O(n)     │ O(n)     │ Rehashes every entry into new table       │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ HASHSET                   │                          │          │          │                                           │
 * │ HashSet                   │ add(element)             │ O(1)     │ O(n)     │ Backed by HashMap — same complexity       │
 * │ HashSet                   │ remove(element)          │ O(1)     │ O(n)     │                                           │
 * │ HashSet                   │ contains(element)        │ O(1)     │ O(n)     │                                           │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ LINKEDHASHMAP             │                          │          │          │                                           │
 * │ LinkedHashMap             │ get/put/remove           │ O(1)     │ O(n)     │ Same as HashMap + linked list maintenance │
 * │ LinkedHashMap             │ Iteration                │ O(n)     │ O(n)     │ Iterates in insertion order (not bucket)  │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ TREEMAP                   │                          │          │          │                                           │
 * │ TreeMap                   │ get/put/remove           │ O(log n) │ O(log n) │ Red-Black Tree — always balanced          │
 * │ TreeMap                   │ firstKey/lastKey         │ O(log n) │ O(log n) │                                           │
 * │ TreeMap                   │ floorKey/ceilingKey      │ O(log n) │ O(log n) │                                           │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ SINGLY LINKED LIST        │                          │          │          │                                           │
 * │ Singly LL                 │ Insert at head           │ O(1)     │ O(1)     │ Just update head pointer                  │
 * │ Singly LL                 │ Insert at tail           │ O(n)     │ O(n)     │ O(1) if you maintain a tail pointer       │
 * │ Singly LL                 │ Insert at position       │ O(n)     │ O(n)     │ Must traverse to position                 │
 * │ Singly LL                 │ Delete by value/position │ O(n)     │ O(n)     │ Must find + find predecessor              │
 * │ Singly LL                 │ Search                   │ O(n)     │ O(n)     │ Linear scan                               │
 * │ Singly LL                 │ Reverse                  │ O(n)     │ O(n)     │ Single pass with 3 pointers               │
 * │ Singly LL                 │ Find middle              │ O(n)     │ O(n)     │ Slow/fast pointer — single pass           │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ DOUBLY LINKED LIST        │                          │          │          │                                           │
 * │ Doubly LL                 │ Insert at head/tail      │ O(1)     │ O(1)     │ Direct pointer access with sentinels      │
 * │ Doubly LL                 │ Remove given node ref    │ O(1)     │ O(1)     │ KEY ADVANTAGE: no need to find predecessor│
 * │ Doubly LL                 │ Search                   │ O(n)     │ O(n)     │ Still linear — no random access           │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ LINKEDLIST AS QUEUE/DEQUE │                          │          │          │                                           │
 * │ Queue (LinkedList)        │ offer (enqueue)          │ O(1)     │ O(1)     │                                           │
 * │ Queue (LinkedList)        │ poll (dequeue)           │ O(1)     │ O(1)     │                                           │
 * │ Queue (LinkedList)        │ peek                     │ O(1)     │ O(1)     │                                           │
 * │ Deque (ArrayDeque)        │ offerFirst/offerLast     │ O(1)†    │ O(n)     │ †Amortized — occasional resize            │
 * │ Deque (ArrayDeque)        │ pollFirst/pollLast       │ O(1)     │ O(1)     │                                           │
 * ├───────────────────────────┼──────────────────────────┼──────────┼──────────┼───────────────────────────────────────────┤
 * │ COMBINED STRUCTURES       │                          │          │          │                                           │
 * │ HashMap + DLL (LRU Cache) │ get                      │ O(1)     │ O(1)*    │ *O(1) because HashMap → node ref → done   │
 * │ HashMap + DLL (LRU Cache) │ put                      │ O(1)     │ O(1)*    │ HashMap insert + DLL add to front         │
 * │ HashMap + DLL (LRU Cache) │ evict LRU               │ O(1)     │ O(1)*    │ DLL remove tail + HashMap remove by key   │
 * └───────────────────────────┴──────────────────────────┴──────────┴──────────┴───────────────────────────────────────────┘
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ KEY COMPLEXITY THEMES TO REMEMBER                                   │
 * │                                                                     │
 * │ 1. HASHMAP O(1) IS AMORTIZED AND AVERAGE:                          │
 * │    The O(1) assumes good hash distribution and load factor < 1.     │
 * │    Worst case (all keys collide) is O(n) for chained lists or       │
 * │    O(log n) for treeified chains (Java 8+). In practice, O(1).     │
 * │                                                                     │
 * │ 2. SINGLY vs DOUBLY LINKED: the key difference is deletion.        │
 * │    SLL delete = O(n) (must find predecessor by traversal).          │
 * │    DLL delete = O(1) (predecessor is node.prev).                    │
 * │    This is WHY LRU Cache uses a DLL, not a SLL.                     │
 * │                                                                     │
 * │ 3. HASHMAP + DOUBLY LINKED LIST is the O(1)-everything trick:      │
 * │    HashMap gives O(1) lookup → DLL node reference.                  │
 * │    DLL gives O(1) insert/remove at ends + O(1) remove given node.   │
 * │    Neither alone achieves all O(1). Together they do.               │
 * │                                                                     │
 * │ 4. ARRAYDEQUE vs LINKEDLIST as Queue:                               │
 * │    ArrayDeque is faster (cache locality, fewer allocations).        │
 * │    LinkedList allows nulls and mid-list operations.                 │
 * │    Default to ArrayDeque unless you need LinkedList features.       │
 * │                                                                     │
 * │ 5. containsValue() IS O(n): this catches people. containsKey()     │
 * │    is O(1), but value lookup has no index — must scan everything.   │
 * │    If you need value lookups, build a reverse map.                  │
 * └─────────────────────────────────────────────────────────────────────┘
 */
public class S0_ComplexityCheatSheet {
    // This file is a reference document — no executable code needed.
    // Keep it open during practice sessions.
}

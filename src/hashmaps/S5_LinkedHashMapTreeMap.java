package hashmaps;

import java.util.*;

/**
 * ============================================================
 * SECTION 5: LINKEDHASHMAP AND TREEMAP
 * ============================================================
 *
 * When plain HashMap isn't enough: you need ordering.
 *
 * HashMap:       no ordering guarantees. Fastest.
 * LinkedHashMap: maintains INSERTION order (or ACCESS order). O(1) ops.
 * TreeMap:       sorted by KEY (natural ordering or Comparator). O(log n) ops.
 */
public class S5_LinkedHashMapTreeMap {

    // =============================================================
    // 5.1  LINKEDHASHMAP — Insertion Order + LRU Cache
    // =============================================================
    //
    // LinkedHashMap = HashMap + doubly linked list threading through all entries.
    // Iteration order = insertion order (by default).
    //
    // With constructor parameter accessOrder=true:
    //   Iteration order = MOST RECENTLY ACCESSED last.
    //   Every get() or put() moves the entry to the end.
    //   This is exactly LRU (Least Recently Used) ordering!
    //
    // Time complexity: same as HashMap (O(1) for get/put/remove)
    // Space overhead: two extra pointers per entry (prev/next in linked list)

    /**
     * LRU Cache in 5 lines using LinkedHashMap.
     *
     * This is a REAL interview trick. If asked to implement LRU Cache
     * and the interviewer says "use whatever Java gives you," this is it.
     * (Most interviewers want the manual HashMap + DoublyLinkedList approach
     * from Section 8, but know this exists.)
     */
    static class LRUCacheSimple<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LRUCacheSimple(int capacity) {
            // initialCapacity, loadFactor, accessOrder=true (LRU mode)
            super(capacity, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity; // auto-remove LRU entry when over capacity
        }
    }

    // =============================================================
    // 5.2  TREEMAP — Sorted Keys, Range Queries
    // =============================================================
    //
    // TreeMap is backed by a Red-Black Tree.
    //   - Keys are sorted (natural ordering or custom Comparator).
    //   - get, put, remove: O(log n)
    //   - firstKey, lastKey, floorKey, ceilingKey: O(log n)
    //   - subMap, headMap, tailMap: O(log n) to create the view
    //
    // When to use TreeMap over HashMap:
    //   - You need keys in sorted order
    //   - You need range queries ("all keys between 5 and 10")
    //   - You need floor/ceiling ("nearest key ≤ x" or "nearest key ≥ x")
    //
    // When NOT to: if you just need key-value storage, HashMap is faster.

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        // --- LinkedHashMap: insertion order ---
        System.out.println("=== LINKEDHASHMAP — Insertion Order ===");
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("banana", 2);
        linked.put("apple", 1);
        linked.put("cherry", 3);

        // Iteration order = insertion order
        System.out.print("Iteration: ");
        linked.forEach((k, v) -> System.out.print(k + " "));
        System.out.println(); // banana apple cherry (insertion order)

        // Compare with HashMap (unpredictable order)
        Map<String, Integer> plain = new HashMap<>(linked);
        System.out.print("HashMap:   ");
        plain.forEach((k, v) -> System.out.print(k + " "));
        System.out.println(); // order may vary

        // --- LinkedHashMap: LRU Cache ---
        System.out.println("\n=== LRU CACHE (LinkedHashMap) ===");
        LRUCacheSimple<Integer, String> lru = new LRUCacheSimple<>(3);
        lru.put(1, "one");
        lru.put(2, "two");
        lru.put(3, "three");
        System.out.println("Initial: " + lru); // {1=one, 2=two, 3=three}

        lru.get(1);  // access key 1 → moves to end (most recently used)
        System.out.println("After get(1): " + lru); // {2=two, 3=three, 1=one}

        lru.put(4, "four"); // capacity exceeded → evicts LRU (key 2)
        System.out.println("After put(4): " + lru); // {3=three, 1=one, 4=four}
        System.out.println("get(2): " + lru.get(2)); // null — evicted

        // --- TreeMap ---
        System.out.println("\n=== TREEMAP — Sorted Keys ===");
        TreeMap<Integer, String> tree = new TreeMap<>();
        tree.put(5, "five");
        tree.put(2, "two");
        tree.put(8, "eight");
        tree.put(1, "one");
        tree.put(6, "six");

        // Iteration order = sorted by key
        System.out.println("Sorted: " + tree);
        // {1=one, 2=two, 5=five, 6=six, 8=eight}

        // Navigation methods (all O(log n)):
        System.out.println("firstKey:     " + tree.firstKey());     // 1
        System.out.println("lastKey:      " + tree.lastKey());      // 8
        System.out.println("floorKey(4):  " + tree.floorKey(4));    // 2  (largest key ≤ 4)
        System.out.println("ceilingKey(4):" + tree.ceilingKey(4));   // 5  (smallest key ≥ 4)
        System.out.println("lowerKey(5):  " + tree.lowerKey(5));    // 2  (largest key < 5)
        System.out.println("higherKey(5): " + tree.higherKey(5));   // 6  (smallest key > 5)

        // Range queries:
        System.out.println("subMap(2, 6): " + tree.subMap(2, 6));   // {2=two, 5=five}
        // subMap is [fromKey, toKey) — inclusive start, exclusive end
        System.out.println("headMap(5):   " + tree.headMap(5));     // {1=one, 2=two}
        System.out.println("tailMap(5):   " + tree.tailMap(5));     // {5=five, 6=six, 8=eight}

        // --- TreeMap with custom comparator ---
        System.out.println("\n=== TREEMAP — Reverse Order ===");
        TreeMap<Integer, String> reversed = new TreeMap<>(Comparator.reverseOrder());
        reversed.putAll(tree);
        System.out.println("Reversed: " + reversed);
        // {8=eight, 6=six, 5=five, 2=two, 1=one}
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — MAP VARIANTS                               │
 * │                                                             │
 * │ 1. If asked "implement LRU Cache," mention that Java's      │
 * │    LinkedHashMap does it, but then implement the manual      │
 * │    version (HashMap + DLL) to show depth.                   │
 * │                                                             │
 * │ 2. TreeMap.floorKey() and ceilingKey() are powerful for      │
 * │    interval problems, calendar booking, and range queries.   │
 * │    Know they exist — they can turn O(n) scans into O(log n).│
 * │                                                             │
 * │ 3. Quick decision guide:                                    │
 * │    Need speed + no ordering?    → HashMap                   │
 * │    Need insertion order?        → LinkedHashMap              │
 * │    Need sorted keys?            → TreeMap                   │
 * │    Need sorted + fast get/put?  → doesn't exist (trade-off) │
 * │                                                             │
 * │ 4. TreeMap subMap/headMap/tailMap return VIEWS, not copies.  │
 * │    Modifying the view modifies the original. Mention this.  │
 * └─────────────────────────────────────────────────────────────┘
 */

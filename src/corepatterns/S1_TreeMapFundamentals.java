package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 1: TREEMAP FUNDAMENTALS
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * 1.1  WHAT IS A TREEMAP?
 * -------------------------------------------------------
 *
 * A TreeMap is a Map implementation backed by a RED-BLACK TREE.
 * Keys are always sorted — either by natural order (Comparable)
 * or by a custom Comparator provided at construction.
 *
 *   HashMap vs TreeMap:
 *   ┌──────────────┬────────────┬────────────────────────────┐
 *   │              │ HashMap    │ TreeMap                     │
 *   ├──────────────┼────────────┼────────────────────────────┤
 *   │ Ordering     │ None       │ Sorted by key               │
 *   │ get/put      │ O(1) avg   │ O(log n) always             │
 *   │ Range query  │ ✗ N/A      │ ✓ subMap, headMap, tailMap  │
 *   │ floor/ceiling│ ✗ N/A      │ ✓ O(log n)                  │
 *   │ Iteration    │ Unordered  │ Sorted order                │
 *   │ Null keys    │ 1 allowed  │ ✗ Not allowed               │
 *   │ Backing      │ Hash table │ Red-Black Tree              │
 *   └──────────────┴────────────┴────────────────────────────┘
 *
 * WHEN TO CHOOSE TreeMap:
 *   - You need keys in sorted order
 *   - You need range queries (all keys between X and Y)
 *   - You need floor/ceiling lookups ("nearest key to X")
 *   - You need first/last key access
 *   If you don't need any of these → use HashMap (faster).
 *
 *
 * -------------------------------------------------------
 * 1.2  RED-BLACK TREE (high-level)
 * -------------------------------------------------------
 *
 * A Red-Black Tree is a SELF-BALANCING BST that guarantees:
 *   - Height is always O(log n)
 *   - Therefore all operations are O(log n) — never degrades to O(n)
 *
 * How it balances: each node is colored red or black, with rules that
 * prevent any path from root to leaf from being more than 2x longer
 * than any other. After inserts/deletes, the tree rebalances with
 * rotations and recoloring.
 *
 * YOU DON'T NEED TO IMPLEMENT ONE IN INTERVIEWS.
 * Just explain: "TreeMap uses a Red-Black Tree internally, which
 * guarantees O(log n) for all operations because the tree stays balanced."
 *
 *
 * -------------------------------------------------------
 * 1.3  TREEMAP API — COMPLETE COVERAGE
 * -------------------------------------------------------
 *
 * We'll walk through every important method using this example TreeMap:
 *
 *   Keys: {10, 20, 30, 40, 50}
 *
 *     TreeMap tree:
 *            30
 *           /  \
 *         20    40
 *        /        \
 *      10          50
 */
public class S1_TreeMapFundamentals {

    public static void main(String[] args) {

        // =============================================================
        // BASIC OPERATIONS — O(log n) each
        // =============================================================
        System.out.println("=== BASIC OPERATIONS ===");

        TreeMap<Integer, String> map = new TreeMap<>();

        map.put(30, "thirty");
        map.put(10, "ten");
        map.put(50, "fifty");
        map.put(20, "twenty");
        map.put(40, "forty");

        System.out.println("get(30): " + map.get(30));             // thirty
        System.out.println("containsKey(25): " + map.containsKey(25)); // false
        System.out.println("size: " + map.size());                 // 5
        System.out.println("Sorted iteration: " + map);            // {10=ten, 20=twenty, ...}

        map.remove(30);
        System.out.println("After remove(30): " + map);
        map.put(30, "thirty"); // re-add for later demos

        // =============================================================
        // SORTED ACCESS — firstKey, lastKey
        // =============================================================
        System.out.println("\n=== SORTED ACCESS ===");

        System.out.println("firstKey: " + map.firstKey());         // 10 (smallest)
        System.out.println("lastKey:  " + map.lastKey());          // 50 (largest)
        System.out.println("firstEntry: " + map.firstEntry());     // 10=ten
        System.out.println("lastEntry:  " + map.lastEntry());      // 50=fifty

        // =============================================================
        // NEIGHBOR LOOKUPS — floor, ceiling, lower, higher
        // =============================================================
        //
        // These are the CRITICAL interview methods.
        //
        //   floorKey(k):   greatest key ≤ k   ("round DOWN")
        //   ceilingKey(k): smallest key ≥ k   ("round UP")
        //   lowerKey(k):   greatest key < k   (strictly less)
        //   higherKey(k):  smallest key > k   (strictly greater)
        //
        //   Returns null if no such key exists.
        //
        //   Keys: {10, 20, 30, 40, 50}
        //
        //   ┌────────────┬──────────┬─────────────┬──────────┬─────────────┐
        //   │ Query key  │ floorKey │ ceilingKey   │ lowerKey │ higherKey   │
        //   ├────────────┼──────────┼─────────────┼──────────┼─────────────┤
        //   │ 25         │ 20       │ 30          │ 20       │ 30          │
        //   │ 30 (exact) │ 30       │ 30          │ 20       │ 40          │
        //   │ 5          │ null     │ 10          │ null     │ 10          │
        //   │ 55         │ 50       │ null        │ 50       │ null        │
        //   └────────────┴──────────┴─────────────┴──────────┴─────────────┘

        System.out.println("\n=== NEIGHBOR LOOKUPS ===");
        System.out.println("Keys: " + map.keySet()); // [10, 20, 30, 40, 50]

        System.out.println("\n--- Query key = 25 ---");
        System.out.println("  floorKey(25):   " + map.floorKey(25));   // 20
        System.out.println("  ceilingKey(25): " + map.ceilingKey(25)); // 30
        System.out.println("  lowerKey(25):   " + map.lowerKey(25));   // 20
        System.out.println("  higherKey(25):  " + map.higherKey(25));  // 30

        System.out.println("\n--- Query key = 30 (exact match) ---");
        System.out.println("  floorKey(30):   " + map.floorKey(30));   // 30 (includes exact)
        System.out.println("  ceilingKey(30): " + map.ceilingKey(30)); // 30 (includes exact)
        System.out.println("  lowerKey(30):   " + map.lowerKey(30));   // 20 (strictly less)
        System.out.println("  higherKey(30):  " + map.higherKey(30));  // 40 (strictly greater)

        System.out.println("\n--- Query key = 5 (below all keys) ---");
        System.out.println("  floorKey(5):    " + map.floorKey(5));    // null
        System.out.println("  ceilingKey(5):  " + map.ceilingKey(5));  // 10

        System.out.println("\n--- Query key = 55 (above all keys) ---");
        System.out.println("  floorKey(55):   " + map.floorKey(55));   // 50
        System.out.println("  ceilingKey(55): " + map.ceilingKey(55)); // null

        // Entry versions return Map.Entry (key + value)
        System.out.println("\n--- Entry versions ---");
        System.out.println("  floorEntry(25):   " + map.floorEntry(25));   // 20=twenty
        System.out.println("  ceilingEntry(25): " + map.ceilingEntry(25)); // 30=thirty

        // =============================================================
        // RANGE OPERATIONS — subMap, headMap, tailMap
        // =============================================================
        //
        // These return VIEWS — changes to the view affect the original map!
        //
        // Keys: {10, 20, 30, 40, 50}
        //
        //   subMap(20, 40):         keys in [20, 40) = {20, 30}
        //   subMap(20, true, 40, true): keys in [20, 40] = {20, 30, 40}
        //   headMap(30):            keys < 30 = {10, 20}
        //   headMap(30, true):      keys <= 30 = {10, 20, 30}
        //   tailMap(30):            keys >= 30 = {30, 40, 50}
        //   tailMap(30, false):     keys > 30 = {40, 50}

        System.out.println("\n=== RANGE OPERATIONS ===");

        System.out.println("subMap(20, 40):            " + map.subMap(20, 40));
        // {20=twenty, 30=thirty}  — 20 inclusive, 40 EXCLUSIVE

        System.out.println("subMap(20, true, 40, true):" + map.subMap(20, true, 40, true));
        // {20=twenty, 30=thirty, 40=forty}  — both inclusive

        System.out.println("headMap(30):               " + map.headMap(30));
        // {10=ten, 20=twenty}  — strictly less than 30

        System.out.println("headMap(30, true):         " + map.headMap(30, true));
        // {10=ten, 20=twenty, 30=thirty}  — less than or equal

        System.out.println("tailMap(30):               " + map.tailMap(30));
        // {30=thirty, 40=forty, 50=fifty}  — greater than or equal

        System.out.println("tailMap(30, false):        " + map.tailMap(30, false));
        // {40=forty, 50=fifty}  — strictly greater

        // VIEWS affect the original:
        System.out.println("\n--- Views are LIVE ---");
        SortedMap<Integer, String> sub = map.subMap(20, 40);
        System.out.println("subMap view: " + sub);         // {20=twenty, 30=thirty}
        map.put(25, "twenty-five"); // modify original
        System.out.println("After adding 25 to original:");
        System.out.println("  subMap view: " + sub);       // {20=twenty, 25=twenty-five, 30=thirty}
        map.remove(25); // clean up

        // =============================================================
        // NAVIGATION & ITERATION
        // =============================================================
        System.out.println("\n=== NAVIGATION ===");

        System.out.println("navigableKeySet: " + map.navigableKeySet());
        // [10, 20, 30, 40, 50]

        System.out.println("descendingMap:   " + map.descendingMap());
        // {50=fifty, 40=forty, 30=thirty, 20=twenty, 10=ten}

        // Iterating a TreeMap ALWAYS gives sorted order
        System.out.print("Sorted for-each: ");
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            System.out.print(entry.getKey() + " ");
        }
        System.out.println(); // 10 20 30 40 50

        // =============================================================
        // CUSTOM COMPARATOR — Reverse Order
        // =============================================================
        System.out.println("\n=== CUSTOM COMPARATOR ===");

        TreeMap<Integer, String> reversed = new TreeMap<>(Comparator.reverseOrder());
        reversed.put(1, "one");
        reversed.put(3, "three");
        reversed.put(2, "two");
        System.out.println("Reverse order: " + reversed); // {3=three, 2=two, 1=one}
        System.out.println("firstKey (largest): " + reversed.firstKey()); // 3

        // PITFALL: if Comparator says two keys are "equal" (returns 0),
        // TreeMap treats them as THE SAME KEY and overwrites.
        TreeMap<String, Integer> byLength = new TreeMap<>(Comparator.comparingInt(String::length));
        byLength.put("cat", 1);
        byLength.put("dog", 2); // "dog".length() == "cat".length() → OVERWRITES "cat"!
        System.out.println("\nBy-length TreeMap: " + byLength); // {dog=2} — "cat" is GONE
        // This is because Comparator.comparingInt("cat".length() vs "dog".length()) returns 0,
        // and TreeMap uses the comparator for equality, not .equals().

        // Fix: add a tiebreaker to the comparator
        TreeMap<String, Integer> byLengthFixed = new TreeMap<>(
            Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())
        );
        byLengthFixed.put("cat", 1);
        byLengthFixed.put("dog", 2);
        System.out.println("Fixed by-length: " + byLengthFixed); // {cat=1, dog=2}
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — TREEMAP FUNDAMENTALS                        │
 * │                                                             │
 * │ 1. TreeMap's KILLER FEATURE is neighbor lookups:             │
 * │    floorKey/ceilingKey in O(log n). No other Map offers     │
 * │    this. If a problem needs "find the nearest key to X,"   │
 * │    think TreeMap immediately.                               │
 * │                                                             │
 * │ 2. Range queries (subMap) are O(log n + k) where k is the  │
 * │    number of results. HashMap has no equivalent.            │
 * │                                                             │
 * │ 3. COMMON PITFALL: subMap, headMap, tailMap return VIEWS.   │
 * │    Modifying the view modifies the original map. Mention    │
 * │    this in interviews — it shows you understand the API.    │
 * │                                                             │
 * │ 4. Custom Comparator pitfall: if two keys compare as equal  │
 * │    (comparator returns 0), TreeMap overwrites. Always add   │
 * │    a tiebreaker: .thenComparing(naturalOrder()).             │
 * │                                                             │
 * │ 5. floor vs lower: floor includes exact match, lower does   │
 * │    not. ceiling vs higher: same distinction. Draw the       │
 * │    number line if you're unsure.                            │
 * └─────────────────────────────────────────────────────────────┘
 */

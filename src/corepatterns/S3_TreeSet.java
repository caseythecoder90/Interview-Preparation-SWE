package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 3: TREESET
 * ============================================================
 *
 * TreeSet is to TreeMap as HashSet is to HashMap.
 *   - Backed by a Red-Black Tree (actually backed by a TreeMap internally)
 *   - Elements are always sorted
 *   - All operations are O(log n)
 *   - Supports floor, ceiling, subSet, headSet, tailSet
 *
 * Use TreeSet when you need a SORTED COLLECTION without key-value pairs.
 *
 *   ┌──────────────┬────────────┬──────────────────────────────┐
 *   │              │ HashSet    │ TreeSet                       │
 *   ├──────────────┼────────────┼──────────────────────────────┤
 *   │ Ordering     │ None       │ Sorted                        │
 *   │ add/remove   │ O(1) avg   │ O(log n)                      │
 *   │ contains     │ O(1) avg   │ O(log n)                      │
 *   │ floor/ceiling│ ✗ N/A      │ ✓ O(log n)                    │
 *   │ subSet       │ ✗ N/A      │ ✓ O(log n + k)                │
 *   │ Null elements│ 1 allowed  │ ✗ Not allowed                 │
 *   └──────────────┴────────────┴──────────────────────────────┘
 */
public class S3_TreeSet {

    public static void main(String[] args) {

        // =============================================================
        // Basic operations
        // =============================================================
        System.out.println("=== BASIC OPERATIONS ===");

        TreeSet<Integer> set = new TreeSet<>();
        set.add(30);
        set.add(10);
        set.add(50);
        set.add(20);
        set.add(40);

        System.out.println("TreeSet: " + set);              // [10, 20, 30, 40, 50] ← sorted!
        System.out.println("contains(30): " + set.contains(30)); // true
        System.out.println("size: " + set.size());           // 5

        set.remove(30);
        System.out.println("After remove(30): " + set);     // [10, 20, 40, 50]
        set.add(30); // re-add

        // =============================================================
        // Sorted access
        // =============================================================
        System.out.println("\n=== SORTED ACCESS ===");
        System.out.println("first: " + set.first());         // 10 (smallest)
        System.out.println("last:  " + set.last());          // 50 (largest)

        // =============================================================
        // Neighbor lookups — same as TreeMap
        // =============================================================
        System.out.println("\n=== NEIGHBOR LOOKUPS ===");
        System.out.println("floor(25):   " + set.floor(25));     // 20 (greatest ≤ 25)
        System.out.println("ceiling(25): " + set.ceiling(25));   // 30 (smallest ≥ 25)
        System.out.println("lower(30):   " + set.lower(30));     // 20 (strictly <)
        System.out.println("higher(30):  " + set.higher(30));    // 40 (strictly >)

        // =============================================================
        // Range operations
        // =============================================================
        System.out.println("\n=== RANGE OPERATIONS ===");
        System.out.println("subSet(20, 40):            " + set.subSet(20, 40));
        // [20, 30] — 20 inclusive, 40 exclusive

        System.out.println("subSet(20, true, 40, true):" + set.subSet(20, true, 40, true));
        // [20, 30, 40] — both inclusive

        System.out.println("headSet(30):               " + set.headSet(30));
        // [10, 20] — strictly < 30

        System.out.println("tailSet(30):               " + set.tailSet(30));
        // [30, 40, 50] — ≥ 30

        // =============================================================
        // Descending and polling
        // =============================================================
        System.out.println("\n=== DESCENDING & POLLING ===");
        System.out.println("descendingSet: " + set.descendingSet()); // [50, 40, 30, 20, 10]

        // pollFirst/pollLast: remove and return min/max
        TreeSet<Integer> copy = new TreeSet<>(set);
        System.out.println("pollFirst: " + copy.pollFirst());  // 10 (removes smallest)
        System.out.println("pollLast:  " + copy.pollLast());   // 50 (removes largest)
        System.out.println("After polls: " + copy);            // [20, 30, 40]

        // =============================================================
        // Use case: maintaining a sorted window of values
        // =============================================================
        System.out.println("\n=== SORTED WINDOW EXAMPLE ===");
        // Track the K most recent scores, always able to get min/max
        TreeSet<Integer> scores = new TreeSet<>();
        int[] incoming = {85, 92, 78, 95, 88, 70, 99};
        int windowSize = 4;

        for (int score : incoming) {
            scores.add(score);
            if (scores.size() > windowSize) {
                scores.pollFirst(); // remove smallest to maintain window
            }
            if (scores.size() == windowSize) {
                System.out.println("Window: " + scores +
                    " | min=" + scores.first() + " max=" + scores.last());
            }
        }
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — TREESET                                     │
 * │                                                             │
 * │ 1. TreeSet is perfect for "maintain sorted order + fast     │
 * │    min/max." PriorityQueue also gives min/max, but TreeSet  │
 * │    additionally supports floor/ceiling and range queries.   │
 * │                                                             │
 * │ 2. TreeSet does NOT allow duplicates (just like HashSet).   │
 * │    If you need duplicates with sorted order, use a          │
 * │    TreeMap<Value, Integer> (value → count).                 │
 * │                                                             │
 * │ 3. pollFirst/pollLast make TreeSet act like a sorted deque. │
 * │    Useful for sliding window problems where you need to     │
 * │    track sorted order within the window.                    │
 * └─────────────────────────────────────────────────────────────┘
 */

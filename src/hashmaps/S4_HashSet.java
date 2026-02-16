package hashmaps;

import java.util.*;

/**
 * ============================================================
 * SECTION 4: HASHSET
 * ============================================================
 *
 * A HashSet is just a HashMap where you only care about KEYS.
 * Internally: HashSet<E> is backed by HashMap<E, Object> where
 * every value is a shared dummy constant (PRESENT = new Object()).
 *
 * All operations delegate to the underlying HashMap:
 *   set.add(e)      →  map.put(e, PRESENT)
 *   set.contains(e) →  map.containsKey(e)
 *   set.remove(e)   →  map.remove(e)
 *
 * Complexity: add, remove, contains are all O(1) average.
 *
 * WHEN TO USE HashSet vs HashMap:
 *   HashSet: "Does this element exist?" (membership test)
 *   HashMap: "What value is associated with this key?" (key→value mapping)
 *
 *   If you only need to track existence/uniqueness → HashSet.
 *   If you need to associate data with each key → HashMap.
 */
public class S4_HashSet {

    public static void main(String[] args) {

        // =============================================================
        // Basic operations — all O(1) average
        // =============================================================

        System.out.println("=== BASIC OPERATIONS ===");
        Set<Integer> set = new HashSet<>();

        set.add(1);
        set.add(2);
        set.add(3);
        set.add(2); // duplicate — ignored, returns false
        System.out.println("Set: " + set);                    // [1, 2, 3]
        System.out.println("contains(2): " + set.contains(2)); // true
        System.out.println("contains(4): " + set.contains(4)); // false

        set.remove(2);
        System.out.println("After remove(2): " + set);         // [1, 3]
        System.out.println("size: " + set.size());              // 2

        // =============================================================
        // PATTERN 1: Deduplication
        // =============================================================

        System.out.println("\n=== DEDUPLICATION ===");
        int[] nums = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5};
        Set<Integer> unique = new HashSet<>();
        for (int n : nums) unique.add(n);
        System.out.println("Unique elements: " + unique);
        // {1, 2, 3, 4, 5, 6, 9}

        // =============================================================
        // PATTERN 2: Complement/pair lookup (Two Sum variant)
        // =============================================================

        System.out.println("\n=== COMPLEMENT LOOKUP ===");
        // "Does any pair sum to target?"
        int[] arr = {2, 7, 11, 15};
        int target = 9;
        Set<Integer> seen = new HashSet<>();
        for (int n : arr) {
            int complement = target - n;
            if (seen.contains(complement)) {
                System.out.println("Pair found: " + complement + " + " + n + " = " + target);
                break;
            }
            seen.add(n);
        }

        // =============================================================
        // PATTERN 3: Cycle detection (visited set)
        // =============================================================
        //
        // Used in graph problems, detecting infinite loops, etc.
        //
        //   Set<int[]> visited = new HashSet<>();
        //   while (condition) {
        //       if (!visited.add(state))  // add returns false if already present
        //           break; // cycle detected!
        //   }
        //
        // Note: add() returns boolean — if false, the element was already present.
        // This is a convenient one-liner for "add and check if new."

        System.out.println("\n=== CYCLE DETECTION WITH add() ===");
        // Floyd's cycle detection for numbers (Happy Number problem)
        int n = 19; // is 19 a happy number?
        Set<Integer> seenNumbers = new HashSet<>();
        while (n != 1) {
            if (!seenNumbers.add(n)) { // returns false if already seen → cycle
                System.out.println(n + " starts a cycle. Not happy.");
                break;
            }
            int sum = 0;
            while (n > 0) { sum += (n % 10) * (n % 10); n /= 10; }
            n = sum;
        }
        if (n == 1) System.out.println("19 is a happy number!"); // ← this prints

        // =============================================================
        // Set operations: intersection, union, difference
        // =============================================================

        System.out.println("\n=== SET OPERATIONS ===");
        Set<Integer> a = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        Set<Integer> b = new HashSet<>(Arrays.asList(3, 4, 5, 6, 7));

        // INTERSECTION: elements in both sets
        Set<Integer> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        System.out.println("Intersection: " + intersection); // [3, 4, 5]

        // UNION: elements in either set
        Set<Integer> union = new HashSet<>(a);
        union.addAll(b);
        System.out.println("Union: " + union); // [1, 2, 3, 4, 5, 6, 7]

        // DIFFERENCE: elements in A but not in B
        Set<Integer> difference = new HashSet<>(a);
        difference.removeAll(b);
        System.out.println("Difference (A-B): " + difference); // [1, 2]

        // =============================================================
        // Converting between Set and other collections
        // =============================================================

        System.out.println("\n=== CONVERSIONS ===");

        // Array → Set
        String[] words = {"cat", "bat", "cat", "rat"};
        Set<String> wordSet = new HashSet<>(Arrays.asList(words));
        System.out.println("Array to Set: " + wordSet); // [bat, rat, cat]

        // Set → Array
        String[] back = wordSet.toArray(new String[0]);
        System.out.println("Set to Array: " + Arrays.toString(back));

        // Set → List
        List<String> wordList = new ArrayList<>(wordSet);
        System.out.println("Set to List: " + wordList);
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — HASHSET                                    │
 * │                                                             │
 * │ 1. Use set.add() return value as a "check and insert"       │
 * │    one-liner: if (!seen.add(x)) means "already seen."       │
 * │                                                             │
 * │ 2. HashSet is the go-to for O(1) membership testing. If     │
 * │    you're doing repeated contains() checks against a        │
 * │    collection, convert it to a HashSet first.               │
 * │                                                             │
 * │ 3. In graph/BFS/DFS problems, the "visited" set is almost   │
 * │    always a HashSet. Know this pattern cold.                 │
 * │                                                             │
 * │ 4. For problems like "find duplicates in array," the one-   │
 * │    pass HashSet approach is cleaner than sorting.            │
 * │    Time: O(n), Space: O(n) vs Sort: O(n log n), O(1).      │
 * │    Know both trade-offs.                                    │
 * └─────────────────────────────────────────────────────────────┘
 */

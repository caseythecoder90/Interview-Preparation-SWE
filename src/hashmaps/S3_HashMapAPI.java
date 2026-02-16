package hashmaps;

import java.util.*;

/**
 * ============================================================
 * SECTION 3: HASHMAP API MASTERY
 * ============================================================
 *
 * Every method you'd use in an interview, with patterns and examples.
 * Knowing these fluently saves 5-10 minutes per problem.
 */
public class S3_HashMapAPI {

    public static void main(String[] args) {

        // =============================================================
        // 3.1  put() vs putIfAbsent()
        // =============================================================

        System.out.println("=== put vs putIfAbsent ===");
        Map<String, Integer> map = new HashMap<>();

        // put: always sets the value (overwrites if key exists)
        map.put("apple", 1);
        map.put("apple", 2);  // overwrites → value is now 2
        System.out.println("After put overwrite: " + map.get("apple")); // 2

        // putIfAbsent: only sets if key is NOT already present
        map.putIfAbsent("apple", 99);   // key exists → does nothing
        map.putIfAbsent("banana", 3);   // key absent → sets it
        System.out.println("apple (unchanged): " + map.get("apple"));   // 2
        System.out.println("banana (new):      " + map.get("banana"));  // 3

        // Interview use: building first-occurrence maps
        //   "Store the first index where each character appears"
        //   for (int i = 0; i < s.length(); i++)
        //       firstIndex.putIfAbsent(s.charAt(i), i);

        // =============================================================
        // 3.2  get() vs getOrDefault()
        // =============================================================

        System.out.println("\n=== get vs getOrDefault ===");

        // get: returns null if key not found
        System.out.println("get missing: " + map.get("cherry")); // null

        // getOrDefault: returns a default value instead of null
        int val = map.getOrDefault("cherry", 0);
        System.out.println("getOrDefault missing: " + val); // 0

        // Interview use: frequency counting WITHOUT null checks
        //   int count = freq.getOrDefault(ch, 0);
        //   freq.put(ch, count + 1);
        // This is THE most common HashMap pattern in interviews.

        // =============================================================
        // 3.3  containsKey() vs containsValue()
        // =============================================================

        System.out.println("\n=== containsKey vs containsValue ===");

        map.put("cherry", 5);
        System.out.println("containsKey(\"cherry\"): " + map.containsKey("cherry")); // true
        System.out.println("containsValue(5):      " + map.containsValue(5));        // true

        // ⚠️  CRITICAL DIFFERENCE:
        //   containsKey()   → O(1) average (hash lookup)
        //   containsValue() → O(n) always (must scan all values)
        // NEVER use containsValue() in a hot loop. If you need value lookups,
        // build a reverse map.

        // =============================================================
        // 3.4  remove()
        // =============================================================

        System.out.println("\n=== remove ===");

        // remove(key): removes and returns the value
        Integer removed = map.remove("cherry");
        System.out.println("Removed: " + removed);                    // 5
        System.out.println("After remove: " + map.containsKey("cherry")); // false

        // remove(key, value): conditional removal — only removes if value matches
        map.put("grape", 10);
        boolean removedConditional = map.remove("grape", 99); // value doesn't match
        System.out.println("Conditional remove (wrong val): " + removedConditional); // false
        System.out.println("grape still here: " + map.get("grape"));                // 10

        map.remove("grape", 10); // correct value → removes
        System.out.println("grape after correct remove: " + map.get("grape"));       // null

        // =============================================================
        // 3.5  merge() — elegant counting / combining
        // =============================================================

        System.out.println("\n=== merge ===");

        // merge(key, value, remappingFunction)
        // If key absent: sets the value
        // If key present: applies the function to combine old and new values
        Map<String, Integer> freq = new HashMap<>();
        String[] words = {"the", "cat", "sat", "on", "the", "mat", "the"};

        for (String word : words) {
            freq.merge(word, 1, Integer::sum); // same as: old + new
        }
        System.out.println("Word frequencies: " + freq);
        // {mat=1, the=3, sat=1, cat=1, on=1}

        // This is cleaner than the get+put pattern for counting.
        // Interview use: frequency maps, combining values.

        // =============================================================
        // 3.6  compute() — update a value in place
        // =============================================================

        System.out.println("\n=== compute ===");

        // compute(key, (k, v) -> newValue)
        // Applies the function to the current value (null if absent) and sets the result.
        // If the function returns null, the entry is REMOVED.
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 85);

        scores.compute("Alice", (k, v) -> v + 10); // 85 + 10 = 95
        System.out.println("Alice after compute: " + scores.get("Alice")); // 95

        // compute with null value (key doesn't exist yet)
        scores.compute("Bob", (k, v) -> (v == null) ? 50 : v + 10);
        System.out.println("Bob (new): " + scores.get("Bob")); // 50

        // =============================================================
        // 3.7  computeIfAbsent() — the adjacency list builder
        // =============================================================

        System.out.println("\n=== computeIfAbsent ===");

        // computeIfAbsent(key, mappingFunction)
        // If key is absent, compute a value and put it.
        // If key is present, return existing value. Does NOT overwrite.

        // THE most useful method for building adjacency lists and grouping:
        Map<Integer, List<Integer>> graph = new HashMap<>();

        // Old way (verbose):
        //   if (!graph.containsKey(1)) graph.put(1, new ArrayList<>());
        //   graph.get(1).add(2);

        // Clean way:
        graph.computeIfAbsent(1, k -> new ArrayList<>()).add(2);
        graph.computeIfAbsent(1, k -> new ArrayList<>()).add(3);
        graph.computeIfAbsent(2, k -> new ArrayList<>()).add(4);
        System.out.println("Graph: " + graph);
        // {1=[2, 3], 2=[4]}

        // Also great for Group Anagrams:
        //   Map<String, List<String>> groups = new HashMap<>();
        //   for (String s : strs) {
        //       char[] chars = s.toCharArray();
        //       Arrays.sort(chars);
        //       String key = new String(chars);
        //       groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        //   }

        // =============================================================
        // 3.8  Iteration patterns
        // =============================================================

        System.out.println("\n=== Iteration ===");
        Map<String, Integer> data = Map.of("a", 1, "b", 2, "c", 3);

        // entrySet() — iterate over key-value pairs (MOST COMMON in interviews)
        System.out.print("entrySet: ");
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            System.out.print(entry.getKey() + "=" + entry.getValue() + " ");
        }
        System.out.println();

        // keySet() — iterate over keys only
        System.out.print("keySet:   ");
        for (String key : data.keySet()) {
            System.out.print(key + " ");
        }
        System.out.println();

        // values() — iterate over values only
        System.out.print("values:   ");
        for (int v : data.values()) {
            System.out.print(v + " ");
        }
        System.out.println();

        // forEach (lambda) — cleanest syntax
        System.out.print("forEach:  ");
        data.forEach((k, v) -> System.out.print(k + "→" + v + " "));
        System.out.println();

        // =============================================================
        // 3.9  Immutable maps — quick initialization
        // =============================================================

        System.out.println("\n=== Immutable Maps ===");

        // Map.of() — up to 10 key-value pairs (Java 9+)
        Map<String, Integer> small = Map.of("x", 1, "y", 2, "z", 3);
        System.out.println("Map.of: " + small);
        // small.put("w", 4); // throws UnsupportedOperationException!

        // Map.ofEntries() — any number of entries (Java 9+)
        Map<String, Integer> larger = Map.ofEntries(
            Map.entry("a", 1),
            Map.entry("b", 2),
            Map.entry("c", 3),
            Map.entry("d", 4)
        );
        System.out.println("Map.ofEntries: " + larger);

        // Use case: creating lookup tables, test data in interviews.
        // "Let me quickly set up a test map" → Map.of() looks clean on whiteboard.
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — HASHMAP API                                │
 * │                                                             │
 * │ 1. The #1 pattern: getOrDefault + put for counting.         │
 * │    freq.put(ch, freq.getOrDefault(ch, 0) + 1)              │
 * │    Or even cleaner: freq.merge(ch, 1, Integer::sum)         │
 * │                                                             │
 * │ 2. computeIfAbsent is THE way to build adjacency lists and  │
 * │    grouping maps. One line replaces the "check, create,     │
 * │    add" boilerplate. Use it.                                │
 * │                                                             │
 * │ 3. Iterate with entrySet() when you need both key and       │
 * │    value. It's more efficient than keySet() + get()         │
 * │    (avoids a second hash lookup per entry).                 │
 * │                                                             │
 * │ 4. Never use containsValue() in a loop — it's O(n).        │
 * │    Build a reverse map if you need value → key lookups.     │
 * │                                                             │
 * │ 5. Map.of() for quick test data on a whiteboard looks       │
 * │    professional and saves time. Know it exists.             │
 * └─────────────────────────────────────────────────────────────┘
 */

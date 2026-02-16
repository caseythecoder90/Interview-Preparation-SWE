package hashmaps;

import java.util.*;

/**
 * ============================================================
 * SECTION 6: COMMON INTERVIEW PATTERNS USING HASHMAPS
 * ============================================================
 *
 * These 6 patterns cover ~80% of HashMap problems in interviews.
 * Recognize the pattern → apply the template → solve in minutes.
 */
public class S6_CommonPatterns {

    // =============================================================
    // 6.1  FREQUENCY COUNTING
    // =============================================================
    //
    // "Count occurrences of each element."
    // Used in: anagram detection, majority element, top-K frequent, etc.
    //
    // Time: O(n), Space: O(k) where k = distinct elements

    public static Map<Character, Integer> frequencyCount(String s) {
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : s.toCharArray()) {
            freq.merge(c, 1, Integer::sum);
            // Equivalent to:
            // freq.put(c, freq.getOrDefault(c, 0) + 1);
        }
        return freq;
    }

    // =============================================================
    // 6.2  TWO-SUM PATTERN (Complement Lookup)
    // =============================================================
    //
    // "Find two elements that satisfy a condition."
    // Key insight: as you iterate, store what you've SEEN.
    // For each element, check if its COMPLEMENT is in the map.
    //
    // Time: O(n) single pass, Space: O(n)
    //
    // This pattern generalizes: two sum, two sum II, pairs with diff k, etc.

    /** LC 1: Two Sum — return indices of two numbers that add to target. */
    public static int[] twoSum(int[] nums, int target) {
        // Map: value → index (so we can return indices)
        Map<Integer, Integer> seen = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (seen.containsKey(complement)) {
                return new int[]{seen.get(complement), i};
            }
            seen.put(nums[i], i);
        }
        return new int[]{}; // no solution
    }

    // =============================================================
    // 6.3  PREFIX SUM + HASHMAP
    // =============================================================
    //
    // "Count/find subarrays with a given sum."
    //
    // Key insight: if prefixSum[j] - prefixSum[i] == k,
    // then the subarray from i+1 to j sums to k.
    // So we need: prefixSum[j] - k exists in our previous prefix sums.
    //
    // Store a HashMap of { prefixSum → count of times we've seen this sum }.
    //
    // Time: O(n), Space: O(n)

    /** LC 560: Subarray Sum Equals K — count subarrays summing to k. */
    public static int subarraySumEqualsK(int[] nums, int k) {
        // Map: prefix sum → how many times this sum has occurred
        Map<Integer, Integer> prefixCounts = new HashMap<>();
        prefixCounts.put(0, 1); // base case: empty prefix has sum 0

        int currentSum = 0;
        int count = 0;

        for (int num : nums) {
            currentSum += num;

            // If (currentSum - k) was seen before, there's a subarray summing to k
            count += prefixCounts.getOrDefault(currentSum - k, 0);

            // Record current prefix sum
            prefixCounts.merge(currentSum, 1, Integer::sum);
        }
        return count;
    }

    // =============================================================
    // 6.4  GROUPING / BUCKETING
    // =============================================================
    //
    // "Group elements by some key."
    // Pattern: compute a key for each element, group by that key.
    //
    // Time: O(n * k) where k = key computation cost, Space: O(n)

    /** LC 49: Group Anagrams — group words that are anagrams of each other. */
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> groups = new HashMap<>();

        for (String s : strs) {
            // The key: sorted characters (anagrams produce the same sorted string)
            char[] chars = s.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);

            // computeIfAbsent: create list if absent, then add
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }
        return new ArrayList<>(groups.values());
    }

    // =============================================================
    // 6.5  SLIDING WINDOW + HASHMAP
    // =============================================================
    //
    // "Find the longest/shortest substring with some constraint."
    // Use a HashMap to track character frequencies in the current window.
    // Expand the window right, shrink from left when constraint is violated.
    //
    // Time: O(n), Space: O(k) where k = alphabet size

    /** LC 3: Longest Substring Without Repeating Characters. */
    public static int lengthOfLongestSubstring(String s) {
        // Map: character → its most recent index in the window
        Map<Character, Integer> lastIndex = new HashMap<>();
        int maxLen = 0;
        int windowStart = 0;

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            char c = s.charAt(windowEnd);

            // If we've seen this char and it's in our current window, shrink
            if (lastIndex.containsKey(c) && lastIndex.get(c) >= windowStart) {
                windowStart = lastIndex.get(c) + 1; // move past the duplicate
            }

            lastIndex.put(c, windowEnd);
            maxLen = Math.max(maxLen, windowEnd - windowStart + 1);
        }
        return maxLen;
    }

    // =============================================================
    // 6.6  GRAPH ADJACENCY LIST
    // =============================================================
    //
    // Most graph problems in interviews represent edges as a list of pairs.
    // You convert them to an adjacency list using a HashMap.
    //
    // Map<Integer, List<Integer>> for unweighted graphs
    // Map<Integer, List<int[]>>   for weighted graphs (int[] = {neighbor, weight})

    /** Build an undirected adjacency list from edge pairs. */
    public static Map<Integer, List<Integer>> buildAdjList(int[][] edges) {
        Map<Integer, List<Integer>> graph = new HashMap<>();

        for (int[] edge : edges) {
            int u = edge[0], v = edge[1];
            // Undirected: add both directions
            graph.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            graph.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        }
        return graph;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== FREQUENCY COUNTING ===");
        System.out.println(frequencyCount("mississippi"));
        // {p=2, s=4, i=4, m=1}

        System.out.println("\n=== TWO SUM ===");
        int[] result = twoSum(new int[]{2, 7, 11, 15}, 9);
        System.out.println("Indices: " + Arrays.toString(result)); // [0, 1]

        System.out.println("\n=== SUBARRAY SUM EQUALS K ===");
        System.out.println("Count (k=3): " +
            subarraySumEqualsK(new int[]{1, 1, 1}, 2)); // 2
        System.out.println("Count (k=2): " +
            subarraySumEqualsK(new int[]{1, 2, 3, -2, 5}, 5)); // 3

        System.out.println("\n=== GROUP ANAGRAMS ===");
        String[] words = {"eat", "tea", "tan", "ate", "nat", "bat"};
        System.out.println(groupAnagrams(words));
        // [[bat], [eat, tea, ate], [tan, nat]]  (order may vary)

        System.out.println("\n=== LONGEST SUBSTRING WITHOUT REPEATING ===");
        System.out.println("\"abcabcbb\" → " + lengthOfLongestSubstring("abcabcbb")); // 3
        System.out.println("\"bbbbb\"    → " + lengthOfLongestSubstring("bbbbb"));    // 1
        System.out.println("\"pwwkew\"   → " + lengthOfLongestSubstring("pwwkew"));   // 3

        System.out.println("\n=== ADJACENCY LIST ===");
        int[][] edges = {{0,1}, {0,2}, {1,3}, {2,3}};
        Map<Integer, List<Integer>> graph = buildAdjList(edges);
        graph.forEach((node, neighbors) ->
            System.out.println("  " + node + " → " + neighbors));
        // 0 → [1, 2], 1 → [0, 3], 2 → [0, 3], 3 → [1, 2]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — HASHMAP PATTERNS                           │
 * │                                                             │
 * │ 1. TWO SUM is the most important pattern. It's the template │
 * │    for "find complement in one pass." Know it in your sleep.│
 * │                                                             │
 * │ 2. PREFIX SUM + HASHMAP turns O(n²) subarray problems into  │
 * │    O(n). The key insight: "have I seen (currentSum - k)     │
 * │    before?" Always initialize with {0: 1} for the empty     │
 * │    prefix — forgetting this is the #1 bug.                  │
 * │                                                             │
 * │ 3. GROUP ANAGRAMS shows the "canonical key" pattern:        │
 * │    transform each element into a key that's the same for    │
 * │    all "equivalent" elements. Sorted string, frequency       │
 * │    array, prime product — all work.                         │
 * │                                                             │
 * │ 4. SLIDING WINDOW + HASHMAP: the HashMap tracks the window  │
 * │    state (frequencies, last index, etc.). The window shrinks │
 * │    and grows in O(1) per step because HashMap updates are    │
 * │    O(1). Total: O(n).                                       │
 * │                                                             │
 * │ 5. ADJACENCY LIST: always use computeIfAbsent. It's clean,  │
 * │    one-line, and shows you know the API.                    │
 * └─────────────────────────────────────────────────────────────┘
 */

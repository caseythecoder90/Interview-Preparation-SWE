package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 8: SLIDING WINDOW
 * ============================================================
 *
 * Maintain a "window" (subarray/substring) that slides through data,
 * expanding or contracting to meet a condition.
 *
 * WHEN TO RECOGNIZE IT:
 *   - "longest/shortest subarray/substring with condition X"
 *   - "find all subarrays of size K"
 *   - "maximum sum of subarray of size K"
 *   - Anything involving CONTIGUOUS sequences with constraints
 *
 * Two types:
 *   1. FIXED-SIZE window — window size K is given
 *   2. VARIABLE-SIZE window — window grows/shrinks based on a condition
 */
public class S8_SlidingWindow {

    // =============================================================
    // 8.1  FIXED-SIZE WINDOW — Max Sum of Subarray of Size K
    // =============================================================
    //
    // Template:
    //   1. Initialize window with first K elements
    //   2. Slide: add right element, remove left element
    //   3. Track the answer at each position
    //
    // Walkthrough: nums = [2, 1, 5, 1, 3, 2], k = 3
    //
    //   Window [2, 1, 5]: sum = 8
    //   Slide → [1, 5, 1]: sum = 8 - 2 + 1 = 7
    //   Slide → [5, 1, 3]: sum = 7 - 1 + 3 = 9  ← max
    //   Slide → [1, 3, 2]: sum = 9 - 5 + 2 = 6
    //
    //   Answer: 9
    //
    // Time: O(n), Space: O(1)

    public static int maxSumSubarray(int[] nums, int k) {
        // Initialize: sum of first k elements
        int windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += nums[i];
        }
        int maxSum = windowSum;

        // Slide: add right, remove left
        for (int i = k; i < nums.length; i++) {
            windowSum += nums[i];       // add new right element
            windowSum -= nums[i - k];   // remove old left element
            maxSum = Math.max(maxSum, windowSum);
        }
        return maxSum;
    }

    // =============================================================
    // 8.2  VARIABLE-SIZE WINDOW — Longest Substring Without Repeating (LC 3)
    // =============================================================
    //
    // Find the length of the longest substring without repeating characters.
    //
    // Template:
    //   1. Expand: move right pointer, add character to window
    //   2. Contract: if condition violated (duplicate found), move left until valid
    //   3. Track the answer (window size) at each valid state
    //
    // Use a HashMap<Character, Integer> to track the last index of each character.
    // When a repeat is found, move left to max(left, lastIndex + 1).
    //
    // Walkthrough: "abcabcbb"
    //
    //   right=0 'a': map={a:0}, window="a",    len=1
    //   right=1 'b': map={a:0,b:1}, window="ab",   len=2
    //   right=2 'c': map={a:0,b:1,c:2}, window="abc",  len=3
    //   right=3 'a': repeat! left = max(0, 0+1) = 1
    //                map={a:3,b:1,c:2}, window="bca",  len=3
    //   right=4 'b': repeat! left = max(1, 1+1) = 2
    //                map={a:3,b:4,c:2}, window="cab",  len=3
    //   right=5 'c': repeat! left = max(2, 2+1) = 3
    //                map={a:3,b:4,c:5}, window="abc",  len=3
    //   right=6 'b': repeat! left = max(3, 4+1) = 5
    //                map={a:3,b:6,c:5}, window="cb",   len=2
    //   right=7 'b': repeat! left = max(5, 6+1) = 7
    //                map={a:3,b:7,c:5}, window="b",    len=1
    //
    //   Answer: 3

    public static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastIndex = new HashMap<>();
        int maxLen = 0;
        int left = 0;

        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);

            // If we've seen this character and it's within our current window
            if (lastIndex.containsKey(c) && lastIndex.get(c) >= left) {
                left = lastIndex.get(c) + 1; // shrink window past the duplicate
            }

            lastIndex.put(c, right);
            maxLen = Math.max(maxLen, right - left + 1);
        }
        return maxLen;
    }

    // =============================================================
    // 8.3  VARIABLE WINDOW + FREQUENCY MAP — Minimum Window Substring (LC 76)
    // =============================================================
    //
    // Given strings s and t, find the minimum window in s that contains
    // all characters of t (including duplicates).
    //
    // Pattern: expand right until window contains all chars of t,
    //          then shrink left to minimize window size.
    //
    // Use two frequency maps:
    //   - need: character frequencies required from t
    //   - window: character frequencies in current window
    //   - Track how many characters are "satisfied" (have enough count)
    //
    // Time: O(|s| + |t|), Space: O(|s| + |t|)

    public static String minWindow(String s, String t) {
        if (s.length() < t.length()) return "";

        // Count required character frequencies
        Map<Character, Integer> need = new HashMap<>();
        for (char c : t.toCharArray()) {
            need.merge(c, 1, Integer::sum);
        }

        Map<Character, Integer> window = new HashMap<>();
        int have = 0, required = need.size(); // unique chars that must be satisfied
        int minLen = Integer.MAX_VALUE;
        int minStart = 0;
        int left = 0;

        for (int right = 0; right < s.length(); right++) {
            // Expand: add right character to window
            char c = s.charAt(right);
            window.merge(c, 1, Integer::sum);

            // Check if this character's count is now satisfied
            if (need.containsKey(c) && window.get(c).intValue() == need.get(c).intValue()) {
                have++;
            }

            // Contract: while window is valid, try to shrink from left
            while (have == required) {
                // Update best answer
                if (right - left + 1 < minLen) {
                    minLen = right - left + 1;
                    minStart = left;
                }

                // Shrink: remove left character
                char leftChar = s.charAt(left);
                window.merge(leftChar, -1, Integer::sum);
                if (need.containsKey(leftChar) &&
                        window.get(leftChar) < need.get(leftChar)) {
                    have--;
                }
                left++;
            }
        }

        return minLen == Integer.MAX_VALUE ? "" : s.substring(minStart, minStart + minLen);
    }

    // =============================================================
    // 8.4  FIXED WINDOW + FREQUENCY — Find All Anagrams (LC 438)
    // =============================================================
    //
    // Given strings s and p, find all start indices of p's anagrams in s.
    //
    // Fixed window of size p.length(). Use frequency arrays (faster than HashMap
    // for bounded charsets). Slide and compare.

    public static List<Integer> findAnagrams(String s, String p) {
        List<Integer> result = new ArrayList<>();
        if (s.length() < p.length()) return result;

        int[] pFreq = new int[26];
        int[] wFreq = new int[26];

        // Initialize frequency of pattern and first window
        for (int i = 0; i < p.length(); i++) {
            pFreq[p.charAt(i) - 'a']++;
            wFreq[s.charAt(i) - 'a']++;
        }
        if (Arrays.equals(pFreq, wFreq)) result.add(0);

        // Slide the window
        for (int i = p.length(); i < s.length(); i++) {
            wFreq[s.charAt(i) - 'a']++;           // add right
            wFreq[s.charAt(i - p.length()) - 'a']--; // remove left
            if (Arrays.equals(pFreq, wFreq)) {
                result.add(i - p.length() + 1);
            }
        }
        return result;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== FIXED WINDOW: MAX SUM ===");
        System.out.println("Max sum (k=3): " +
            maxSumSubarray(new int[]{2, 1, 5, 1, 3, 2}, 3)); // 9

        System.out.println("\n=== VARIABLE WINDOW: LONGEST SUBSTRING ===");
        System.out.println("\"abcabcbb\": " + lengthOfLongestSubstring("abcabcbb")); // 3
        System.out.println("\"bbbbb\":    " + lengthOfLongestSubstring("bbbbb"));    // 1
        System.out.println("\"pwwkew\":   " + lengthOfLongestSubstring("pwwkew"));   // 3
        System.out.println("\"\":         " + lengthOfLongestSubstring(""));         // 0

        System.out.println("\n=== MIN WINDOW SUBSTRING ===");
        System.out.println("s=\"ADOBECODEBANC\", t=\"ABC\": " +
            minWindow("ADOBECODEBANC", "ABC")); // "BANC"
        System.out.println("s=\"a\", t=\"a\": " + minWindow("a", "a")); // "a"
        System.out.println("s=\"a\", t=\"aa\": " + minWindow("a", "aa")); // ""

        System.out.println("\n=== FIND ALL ANAGRAMS ===");
        System.out.println("s=\"cbaebabacd\", p=\"abc\": " +
            findAnagrams("cbaebabacd", "abc")); // [0, 6]
        System.out.println("s=\"abab\", p=\"ab\": " +
            findAnagrams("abab", "ab")); // [0, 1, 2]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — SLIDING WINDOW                              │
 * │                                                             │
 * │ 1. FIXED vs VARIABLE: if the problem gives you the window   │
 * │    size, it's fixed. If it says "longest/shortest," it's    │
 * │    variable. This determines your template.                 │
 * │                                                             │
 * │ 2. Variable window template:                                │
 * │      for (right = 0; right < n; right++)                    │
 * │        expand window (add right element)                    │
 * │        while (window is invalid)                            │
 * │          contract window (remove left element), left++      │
 * │        update answer                                        │
 * │    Memorize this. It covers 80% of sliding window problems. │
 * │                                                             │
 * │ 3. For substring problems: use HashMap<Character, Integer>  │
 * │    for tracking. For bounded charsets (lowercase only), use │
 * │    int[26] — faster and cleaner.                            │
 * │                                                             │
 * │ 4. Minimum Window Substring is the HARDEST sliding window   │
 * │    problem. The key insight: expand until valid, then shrink│
 * │    until invalid, tracking the best valid window seen.      │
 * │    The "have/required" counter avoids re-checking all       │
 * │    frequencies each step.                                   │
 * └─────────────────────────────────────────────────────────────┘
 */

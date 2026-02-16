package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 7: TWO POINTERS
 * ============================================================
 *
 * Use two pointers (indices) to traverse data, typically from
 * opposite ends or at different speeds. Reduces O(n²) brute
 * force to O(n) for many problems.
 *
 * WHEN TO RECOGNIZE IT:
 *   - Sorted array problems
 *   - Palindrome checks
 *   - Pair finding (two sum on sorted input)
 *   - Partitioning / rearranging elements
 *   - Removing duplicates in-place
 *   - Merging sorted sequences
 *
 *
 * Three main variations:
 *   1. OPPOSITE ENDS — start from both ends, move inward
 *   2. SAME DIRECTION — one pointer moves faster or conditionally
 *   3. MERGE — two pointers, one per sorted sequence
 */
public class S7_TwoPointers {

    // =============================================================
    // 7.1  OPPOSITE ENDS — Two Sum II (sorted array, LC 167)
    // =============================================================
    //
    // Given a SORTED array and target sum, find two numbers that add up to target.
    //
    // Brute force: check every pair → O(n²)
    // Two pointers: start left=0, right=end. If sum < target, move left up.
    //               If sum > target, move right down. O(n).
    //
    // WHY IT WORKS: array is sorted.
    //   - If sum is too small, moving left right increases the sum.
    //   - If sum is too large, moving right left decreases the sum.
    //   - We never skip a valid pair because we only shrink the window
    //     from the side where the correction is needed.
    //
    // Walkthrough: nums = [2, 7, 11, 15], target = 9
    //
    //   left=0, right=3: 2 + 15 = 17 > 9  → move right
    //   left=0, right=2: 2 + 11 = 13 > 9  → move right
    //   left=0, right=1: 2 + 7  = 9  = 9  → FOUND! Return [0, 1]

    public static int[] twoSumSorted(int[] nums, int target) {
        int left = 0, right = nums.length - 1;

        while (left < right) {
            int sum = nums[left] + nums[right];
            if (sum == target) {
                return new int[]{left, right};
            } else if (sum < target) {
                left++;   // need a bigger sum
            } else {
                right--;  // need a smaller sum
            }
        }
        return new int[]{-1, -1}; // not found
    }

    // =============================================================
    // 7.1b  OPPOSITE ENDS — Valid Palindrome (LC 125)
    // =============================================================
    //
    // Check if a string is a palindrome, considering only alphanumeric
    // characters and ignoring case.
    //
    // Two pointers from both ends, skip non-alphanumeric, compare.

    public static boolean isPalindrome(String s) {
        int left = 0, right = s.length() - 1;

        while (left < right) {
            // Skip non-alphanumeric from left
            while (left < right && !Character.isLetterOrDigit(s.charAt(left))) left++;
            // Skip non-alphanumeric from right
            while (left < right && !Character.isLetterOrDigit(s.charAt(right))) right--;

            if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                return false;
            }
            left++;
            right--;
        }
        return true;
    }

    // =============================================================
    // 7.1c  OPPOSITE ENDS — Container With Most Water (LC 11)
    // =============================================================
    //
    // Given heights, find two lines that form a container holding the most water.
    // Area = min(height[left], height[right]) * (right - left)
    //
    // Greedy: always move the SHORTER side inward (moving the taller side
    // can only decrease the area since the width shrinks).

    public static int maxArea(int[] height) {
        int left = 0, right = height.length - 1;
        int maxWater = 0;

        while (left < right) {
            int water = Math.min(height[left], height[right]) * (right - left);
            maxWater = Math.max(maxWater, water);

            // Move the shorter side (greedy: taller side can't help if other is short)
            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }
        return maxWater;
    }

    // =============================================================
    // 7.2  SAME DIRECTION — Move Zeroes (LC 283)
    // =============================================================
    //
    // Move all zeroes to the end while maintaining relative order of non-zeroes.
    // Do it in-place.
    //
    // Pattern: slow pointer = write position, fast pointer = read position.
    //   - Fast scans through the array.
    //   - When fast finds a non-zero, write it at slow's position and advance slow.
    //   - After the loop, fill remaining positions with zeroes.
    //
    // Walkthrough: [0, 1, 0, 3, 12]
    //   fast=0, val=0:  skip
    //   fast=1, val=1:  write at slow=0 → [1, 1, 0, 3, 12], slow=1
    //   fast=2, val=0:  skip
    //   fast=3, val=3:  write at slow=1 → [1, 3, 0, 3, 12], slow=2
    //   fast=4, val=12: write at slow=2 → [1, 3, 12, 3, 12], slow=3
    //   Fill zeroes from slow=3 → [1, 3, 12, 0, 0]

    public static void moveZeroes(int[] nums) {
        int slow = 0; // write position

        // Move all non-zeroes to the front
        for (int fast = 0; fast < nums.length; fast++) {
            if (nums[fast] != 0) {
                nums[slow++] = nums[fast];
            }
        }

        // Fill remaining with zeroes
        while (slow < nums.length) {
            nums[slow++] = 0;
        }
    }

    // =============================================================
    // 7.2b  SAME DIRECTION — Remove Duplicates from Sorted Array (LC 26)
    // =============================================================
    //
    // Remove duplicates in-place from a sorted array. Return new length.
    // slow = last unique element's position. fast scans for new unique values.

    public static int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;

        int slow = 0; // position of last unique element

        for (int fast = 1; fast < nums.length; fast++) {
            if (nums[fast] != nums[slow]) {
                slow++;
                nums[slow] = nums[fast];
            }
        }
        return slow + 1; // length of unique portion
    }

    // =============================================================
    // 7.3  MERGE — Merge Two Sorted Arrays
    // =============================================================
    //
    // Two pointers, one per array, always advance the one with the smaller value.
    // This is the merge step of merge sort.

    public static int[] mergeSorted(int[] a, int[] b) {
        int[] result = new int[a.length + b.length];
        int i = 0, j = 0, k = 0;

        while (i < a.length && j < b.length) {
            if (a[i] <= b[j]) {
                result[k++] = a[i++];
            } else {
                result[k++] = b[j++];
            }
        }
        while (i < a.length) result[k++] = a[i++];
        while (j < b.length) result[k++] = b[j++];
        return result;
    }

    // =============================================================
    // 7.4  SORTED ARRAY SQUARES (Confirmed Fullstory Phone Screen)
    // =============================================================
    //
    // Given a sorted array (may contain negatives), return squares in sorted order.
    //
    // Input:  [-4, -1, 0, 3, 10]
    // Output: [0, 1, 9, 16, 100]
    //
    // KEY INSIGHT: the largest squares are at the EXTREMES (far left or far right).
    //   - left = 0 (most negative → large positive square)
    //   - right = end (most positive → large positive square)
    //   - Compare |left| vs |right|, put the larger square at the END of result,
    //     and move that pointer inward.
    //
    // Walkthrough: [-4, -1, 0, 3, 10]
    //   left=0(-4), right=4(10): |-4|=4 vs |10|=10 → 100 at pos 4, right--
    //   left=0(-4), right=3(3):  |-4|=4 vs |3|=3   → 16 at pos 3, left++
    //   left=1(-1), right=3(3):  |-1|=1 vs |3|=3   → 9 at pos 2, right--
    //   left=1(-1), right=2(0):  |-1|=1 vs |0|=0   → 1 at pos 1, left++
    //   left=2(0), right=2(0):   one element left   → 0 at pos 0
    //
    //   Result: [0, 1, 9, 16, 100] ✓
    //
    // Time: O(n), Space: O(n) for the result array

    public static int[] sortedSquares(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        int left = 0, right = n - 1;
        int pos = n - 1; // fill result from the end (largest first)

        while (left <= right) {
            int leftSq = nums[left] * nums[left];
            int rightSq = nums[right] * nums[right];

            if (leftSq > rightSq) {
                result[pos] = leftSq;
                left++;
            } else {
                result[pos] = rightSq;
                right--;
            }
            pos--;
        }
        return result;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== TWO SUM (SORTED) ===");
        int[] pair = twoSumSorted(new int[]{2, 7, 11, 15}, 9);
        System.out.println("Indices: " + Arrays.toString(pair)); // [0, 1]

        System.out.println("\n=== VALID PALINDROME ===");
        System.out.println("'A man, a plan, a canal: Panama': " +
            isPalindrome("A man, a plan, a canal: Panama")); // true
        System.out.println("'race a car': " + isPalindrome("race a car")); // false

        System.out.println("\n=== CONTAINER WITH MOST WATER ===");
        System.out.println("Max water: " + maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7})); // 49

        System.out.println("\n=== MOVE ZEROES ===");
        int[] zeros = {0, 1, 0, 3, 12};
        moveZeroes(zeros);
        System.out.println(Arrays.toString(zeros)); // [1, 3, 12, 0, 0]

        System.out.println("\n=== REMOVE DUPLICATES ===");
        int[] dups = {1, 1, 2, 3, 3, 3, 4};
        int newLen = removeDuplicates(dups);
        System.out.println("Length: " + newLen); // 4
        System.out.print("Array:  ");
        for (int i = 0; i < newLen; i++) System.out.print(dups[i] + " ");
        System.out.println(); // 1 2 3 4

        System.out.println("\n=== MERGE SORTED ===");
        int[] merged = mergeSorted(new int[]{1, 3, 5}, new int[]{2, 4, 6});
        System.out.println(Arrays.toString(merged)); // [1, 2, 3, 4, 5, 6]

        System.out.println("\n=== SORTED ARRAY SQUARES (Fullstory) ===");
        System.out.println(Arrays.toString(sortedSquares(new int[]{-4, -1, 0, 3, 10})));
        // [0, 1, 9, 16, 100]
        System.out.println(Arrays.toString(sortedSquares(new int[]{-7, -3, 2, 3, 11})));
        // [4, 9, 9, 49, 121]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — TWO POINTERS                                │
 * │                                                             │
 * │ 1. Opposite-end two pointers REQUIRES sorted input (or a    │
 * │    monotonic property). If the array isn't sorted, sort it  │
 * │    first or use a HashMap instead.                          │
 * │                                                             │
 * │ 2. Same-direction two pointers: slow = write position,      │
 * │    fast = read position. This is the pattern for in-place   │
 * │    array modification (move zeroes, remove duplicates).     │
 * │                                                             │
 * │ 3. SORTED SQUARES: fill the result from the END, not the   │
 * │    beginning. The largest values are at the extremes of a   │
 * │    sorted array with negatives. This is the key insight —  │
 * │    explain it clearly in the Fullstory phone screen.        │
 * │                                                             │
 * │ 4. Two pointers is almost always O(n) time, O(1) space.    │
 * │    When an interviewer hears your approach and time         │
 * │    complexity, they know you've identified the pattern.     │
 * └─────────────────────────────────────────────────────────────┘
 */

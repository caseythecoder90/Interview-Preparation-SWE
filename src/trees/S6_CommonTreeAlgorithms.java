package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 6: COMMON TREE ALGORITHMS (INTERVIEW FAVORITES)
 * ============================================================
 *
 * These are the "greatest hits" of tree interview problems. Every one of
 * these has appeared at top tech companies. They all follow a small number
 * of recursive patterns.
 */
public class S6_CommonTreeAlgorithms {

    // =============================================================
    // 6.1  MAXIMUM DEPTH OF A BINARY TREE (LC 104)
    // =============================================================
    //
    // The simplest tree recursion. Often used as a warm-up or phone screen.
    //
    // Time:  O(n)
    // Space: O(h)

    /** Recursive — the classic one-liner. */
    public static int maxDepth(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepth(root.left), maxDepth(root.right));
    }

    /** BFS approach — count levels. */
    public static int maxDepthBFS(TreeNode root) {
        if (root == null) return 0;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        int depth = 0;

        while (!queue.isEmpty()) {
            int size = queue.size();
            depth++;
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                if (node.left != null)  queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
        }
        return depth;
    }

    // =============================================================
    // 6.2  INVERT A BINARY TREE (LC 226)
    // =============================================================
    //
    // Swap left and right children at every node, recursively.
    // (The famous "Google: 90% of our engineers use the software you wrote
    //  (Homebrew), but you can't invert a binary tree on a whiteboard
    //  so f*** off." — Max Howell)
    //
    // Time:  O(n)
    // Space: O(h)

    public static TreeNode invertTree(TreeNode root) {
        if (root == null) return null;

        // Swap children
        TreeNode temp = root.left;
        root.left = root.right;
        root.right = temp;

        // Recurse on both (now-swapped) children
        invertTree(root.left);
        invertTree(root.right);

        return root;
    }

    // =============================================================
    // 6.3  SAME TREE (LC 100)
    // =============================================================
    //
    // Two trees are the same if they have identical structure AND values.
    //
    // Time:  O(min(n1, n2))
    // Space: O(min(h1, h2))

    public static boolean isSameTree(TreeNode p, TreeNode q) {
        if (p == null && q == null) return true;   // both empty = same
        if (p == null || q == null) return false;   // one empty, one not = different
        if (p.val != q.val) return false;           // different values = different

        return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
    }

    // =============================================================
    // 6.4  SUBTREE OF ANOTHER TREE (LC 572)
    // =============================================================
    //
    // Check if tree t is a subtree of tree s.
    // For every node in s, check if the subtree rooted there equals t.
    //
    // Time:  O(n * m) where n = nodes in s, m = nodes in t
    //        For each of n nodes in s, we might compare up to m nodes.
    // Space: O(n) — recursion on s can go n deep (skewed tree)
    //
    // Note: there's an O(n + m) solution using tree hashing or serialization,
    // but the naive approach is expected in most interviews.

    public static boolean isSubtree(TreeNode s, TreeNode t) {
        if (s == null) return false;

        // Check if tree rooted at current node equals t
        if (isSameTree(s, t)) return true;

        // Otherwise, check left and right subtrees
        return isSubtree(s.left, t) || isSubtree(s.right, t);
    }

    // =============================================================
    // 6.5  DIAMETER OF A BINARY TREE (LC 543)
    // =============================================================
    //
    // The diameter is the LONGEST PATH between any two nodes (measured in edges).
    // This path may or may NOT pass through the root.
    //
    // Key insight: at each node, the longest path THROUGH that node =
    //   leftHeight + rightHeight (the path goes down-left and down-right).
    //
    // We compute height bottom-up and track the maximum diameter seen so far.
    // This is the same "return useful info from recursion" pattern as balanced check.
    //
    // Time:  O(n)
    // Space: O(h)

    private static int diameter;

    public static int diameterOfBinaryTree(TreeNode root) {
        diameter = 0;
        heightForDiameter(root);
        return diameter;
    }

    private static int heightForDiameter(TreeNode node) {
        if (node == null) return 0;

        int leftHeight = heightForDiameter(node.left);
        int rightHeight = heightForDiameter(node.right);

        // The path through this node has length leftHeight + rightHeight
        diameter = Math.max(diameter, leftHeight + rightHeight);

        // Return the height of this subtree
        return 1 + Math.max(leftHeight, rightHeight);
    }

    // =============================================================
    // 6.6  PATH SUM (LC 112)
    // =============================================================
    //
    // Does a root-to-LEAF path exist where the values sum to targetSum?
    //
    // ⚠️  Common mistake: forgetting the LEAF requirement.
    //     A leaf is a node with NO children. You can't stop at an internal node.
    //
    // Time:  O(n)
    // Space: O(h)

    public static boolean hasPathSum(TreeNode root, int targetSum) {
        if (root == null) return false;

        // Subtract current value from target as we go
        targetSum -= root.val;

        // If this is a leaf and remaining sum is 0, we found the path
        if (root.left == null && root.right == null) {
            return targetSum == 0;
        }

        // Check either subtree
        return hasPathSum(root.left, targetSum) || hasPathSum(root.right, targetSum);
    }

    // =============================================================
    // 6.7  ALL ROOT-TO-LEAF PATHS (LC 257)
    // =============================================================
    //
    // Collect every path from root to a leaf as a list of values.
    // Classic backtracking: add current node to path, recurse, remove it.
    //
    // Time:  O(n) — visit every node, but copying paths is O(n * h) total
    // Space: O(h) for recursion + O(n * h) for storing all paths

    public static List<List<Integer>> allPaths(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root != null) {
            findPaths(root, new ArrayList<>(), result);
        }
        return result;
    }

    private static void findPaths(TreeNode node, List<Integer> currentPath,
                                   List<List<Integer>> result) {
        currentPath.add(node.val);

        // If leaf, save a copy of the current path
        if (node.left == null && node.right == null) {
            result.add(new ArrayList<>(currentPath)); // COPY — don't add the reference
        } else {
            if (node.left != null)  findPaths(node.left, currentPath, result);
            if (node.right != null) findPaths(node.right, currentPath, result);
        }

        // BACKTRACK: remove current node before returning to parent
        currentPath.remove(currentPath.size() - 1);
    }

    // =============================================================
    // 6.8  MAXIMUM PATH SUM (LC 124) ← HARD, VERY COMMON
    // =============================================================
    //
    // Find the maximum sum path where the path can start and end at ANY node.
    // The path must go through at least one node and follow parent-child connections.
    //
    // Key insight: at each node, we have two concepts:
    //
    //   1. "Max gain from this node" (what we RETURN to the parent):
    //      The max sum of a path that STARTS at this node and goes down
    //      one side (left OR right). We return this because a path through
    //      the parent can only extend in one direction.
    //      = node.val + max(leftGain, rightGain, 0)
    //
    //   2. "Max path through this node" (what we TRACK globally):
    //      The max sum of a path that goes through this node as the "bend."
    //      = node.val + max(leftGain, 0) + max(rightGain, 0)
    //      This is a candidate for the global answer.
    //
    // The max(gain, 0) is crucial: if a subtree has negative sum, we don't
    // include it (we're better off not going into that subtree at all).
    //
    // Time:  O(n) — visit every node once
    // Space: O(h) — recursion stack

    private static int maxSum;

    public static int maxPathSum(TreeNode root) {
        maxSum = Integer.MIN_VALUE; // could be all negative values
        maxGain(root);
        return maxSum;
    }

    /**
     * Returns the maximum "gain" starting from this node going downward
     * (single path, not forking). Updates maxSum as a side effect.
     */
    private static int maxGain(TreeNode node) {
        if (node == null) return 0;

        // Max gain from left/right subtrees; clamp to 0 (don't take negative paths)
        int leftGain = Math.max(maxGain(node.left), 0);
        int rightGain = Math.max(maxGain(node.right), 0);

        // Path through this node as the "highest point" (potential answer)
        int pathThroughNode = node.val + leftGain + rightGain;
        maxSum = Math.max(maxSum, pathThroughNode);

        // Return the max gain if we continue upward (can only go one direction)
        return node.val + Math.max(leftGain, rightGain);
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        /*
         *           4
         *          / \
         *         2   6
         *        / \ / \
         *       1  3 5  7
         */
        TreeNode root = new TreeNode(4,
            new TreeNode(2, new TreeNode(1), new TreeNode(3)),
            new TreeNode(6, new TreeNode(5), new TreeNode(7)));

        System.out.println("=== MAX DEPTH ===");
        System.out.println("Recursive: " + maxDepth(root));     // 3
        System.out.println("BFS:       " + maxDepthBFS(root));  // 3

        System.out.println("\n=== SAME TREE ===");
        TreeNode root2 = new TreeNode(4,
            new TreeNode(2, new TreeNode(1), new TreeNode(3)),
            new TreeNode(6, new TreeNode(5), new TreeNode(7)));
        System.out.println("Same tree: " + isSameTree(root, root2)); // true

        System.out.println("\n=== SUBTREE ===");
        TreeNode sub = new TreeNode(2, new TreeNode(1), new TreeNode(3));
        System.out.println("Is subtree: " + isSubtree(root, sub)); // true

        System.out.println("\n=== DIAMETER ===");
        System.out.println("Diameter: " + diameterOfBinaryTree(root)); // 4 (path: 1→2→4→6→7)

        System.out.println("\n=== PATH SUM (target=7) ===");
        System.out.println("Has path sum 7: " + hasPathSum(root, 7)); // true (4→2→1)

        System.out.println("\n=== ALL ROOT-TO-LEAF PATHS ===");
        System.out.println(allPaths(root));
        // [[4,2,1], [4,2,3], [4,6,5], [4,6,7]]

        System.out.println("\n=== MAX PATH SUM ===");
        System.out.println("Max path sum: " + maxPathSum(root)); // 22 (3+2+4+6+7)

        // Test with negative values
        TreeNode neg = new TreeNode(-10,
            new TreeNode(9),
            new TreeNode(20, new TreeNode(15), new TreeNode(7)));
        System.out.println("Max path sum (with negatives): " + maxPathSum(neg)); // 42 (15+20+7)
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — COMMON TREE ALGORITHMS                     │
 * │                                                             │
 * │ 1. MAX PATH SUM is a top-5 tree interview question. The key │
 * │    is understanding the difference between what you RETURN   │
 * │    to the parent (single direction) and what you TRACK       │
 * │    globally (forking path). Practice explaining this out     │
 * │    loud — interviewers often ask you to walk through it.    │
 * │                                                             │
 * │ 2. DIAMETER uses the exact same pattern as max path sum —   │
 * │    compute something at each node using children's results,  │
 * │    update a global answer, return single-direction info up.  │
 * │    If you can do one, you can do the other.                 │
 * │                                                             │
 * │ 3. For PATH SUM, always ask: "Must it be root-to-leaf?"    │
 * │    LC 112 = root-to-leaf. LC 437 = any downward path.       │
 * │    LC 124 = any path at all. Each requires a different       │
 * │    approach.                                                │
 * │                                                             │
 * │ 4. For ALL PATHS: don't forget to BACKTRACK (remove the     │
 * │    last element). And copy the path when adding to results  │
 * │    — if you just add the reference, it'll be empty later.  │
 * │                                                             │
 * │ 5. INVERT TREE: despite being "easy," make sure you can     │
 * │    write it in 60 seconds. It's often a warm-up or part of  │
 * │    a larger problem. Some candidates fumble it under stress.│
 * └─────────────────────────────────────────────────────────────┘
 */

package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 1: TREE FUNDAMENTALS
 * ============================================================
 *
 * -------------------------------------------------------
 * 1.2  TYPES OF BINARY TREES
 * -------------------------------------------------------
 *
 * BINARY TREE (General)
 *   Each node has at most 2 children. No ordering constraint.
 *   → This is the broadest category. Every other type below is a special case.
 *   → When to care: when a problem says "binary tree" with no other qualifier,
 *     you CANNOT assume any ordering.
 *
 * BINARY SEARCH TREE (BST)
 *   For every node: all values in LEFT subtree < node < all values in RIGHT subtree.
 *   → This is the key property interviews test. Note: it's ALL values in subtree,
 *     not just the immediate children (a common mistake — see Section 5.1).
 *   → When to care: search/insert/delete in O(log n) average; sorted output via inorder.
 *
 * COMPLETE BINARY TREE
 *   Every level is fully filled except possibly the last, which fills left-to-right.
 *   → A binary heap is always a complete binary tree.
 *   → When to care: can be stored in an array (parent i → children 2i+1, 2i+2).
 *     Count-nodes problem exploits this (Section 8.5).
 *
 * FULL BINARY TREE
 *   Every node has either 0 or 2 children — no single-child nodes.
 *   → When to care: rarely asked directly, but useful for proofs and Huffman coding.
 *
 * BALANCED BINARY TREE
 *   Heights of left and right subtrees differ by at most 1, recursively for every node.
 *   → AVL trees enforce this. Red-Black trees are "approximately" balanced.
 *   → When to care: guarantees O(log n) height. If an interviewer says "assume balanced,"
 *     they're telling you the height is O(log n).
 *
 * PERFECT BINARY TREE
 *   Every level is fully filled. All leaves at the same depth.
 *   → Has exactly 2^h - 1 nodes (where h = number of levels).
 *   → When to care: theoretical discussions, complexity proofs.
 *
 * -------------------------------------------------------
 * 1.3  TREE HEIGHT vs DEPTH
 * -------------------------------------------------------
 *
 * DEPTH of a node  = number of edges from the ROOT down to that node.
 *   - Root has depth 0.
 *   - Depth is measured TOP-DOWN.
 *
 * HEIGHT of a node = number of edges from that node down to its deepest leaf.
 *   - A leaf has height 0.
 *   - Height is measured BOTTOM-UP.
 *
 * HEIGHT OF THE TREE = height of the root = max depth of any node.
 *
 * ⚠️  OFF-BY-ONE PITFALL:
 *   Some definitions count NODES instead of EDGES, so "height" = edges + 1.
 *   LeetCode's "Maximum Depth of Binary Tree" (LC 104) counts NODES, not edges.
 *   A single-node tree: depth=0 edges, or depth=1 nodes. ALWAYS clarify with
 *   your interviewer which convention they use.
 */
public class S1_TreeFundamentals {

    // ---------------------------------------------------------
    // HEIGHT OF A TREE (counting edges, bottom-up)
    // ---------------------------------------------------------

    /**
     * Returns the height of the tree rooted at this node (edge-count convention).
     * An empty tree has height -1. A single node has height 0.
     *
     * Time:  O(n) — must visit every node
     * Space: O(h) — recursion stack, where h = height
     */
    public static int height(TreeNode root) {
        if (root == null) return -1;  // base case: empty tree
        return 1 + Math.max(height(root.left), height(root.right));
    }

    /**
     * Same thing but counting NODES (LeetCode convention for "max depth").
     * An empty tree has depth 0. A single node has depth 1.
     */
    public static int maxDepthNodes(TreeNode root) {
        if (root == null) return 0;
        return 1 + Math.max(maxDepthNodes(root.left), maxDepthNodes(root.right));
    }

    // ---------------------------------------------------------
    // HEIGHT — ITERATIVE (BFS / level-order)
    // ---------------------------------------------------------

    /**
     * Compute height iteratively using BFS.
     * Count the number of levels — that gives max depth in node-count convention.
     *
     * Time:  O(n)
     * Space: O(w) where w = max width of tree (up to n/2 at the last level)
     */
    public static int heightIterative(TreeNode root) {
        if (root == null) return 0;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        int levels = 0;

        while (!queue.isEmpty()) {
            int size = queue.size(); // number of nodes at this level
            levels++;
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                if (node.left != null)  queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
        }
        return levels; // node-count convention; subtract 1 for edge-count
    }

    // ---------------------------------------------------------
    // DEPTH OF A SPECIFIC NODE
    // ---------------------------------------------------------

    /**
     * Find the depth of a specific node (edge-count convention).
     * Returns -1 if the target is not found in the tree.
     *
     * Time:  O(n) worst case
     * Space: O(h) recursion stack
     */
    public static int depthOf(TreeNode root, int target) {
        if (root == null) return -1;
        if (root.val == target) return 0;

        int leftDepth = depthOf(root.left, target);
        if (leftDepth != -1) return 1 + leftDepth;

        int rightDepth = depthOf(root.right, target);
        if (rightDepth != -1) return 1 + rightDepth;

        return -1; // target not found in this subtree
    }

    // ---------------------------------------------------------
    // DEMO
    // ---------------------------------------------------------
    public static void main(String[] args) {
        /*
         * Build this tree:
         *           4
         *          / \
         *         2   6
         *        / \ / \
         *       1  3 5  7
         */
        TreeNode root = new TreeNode(4,
            new TreeNode(2, new TreeNode(1), new TreeNode(3)),
            new TreeNode(6, new TreeNode(5), new TreeNode(7)));

        System.out.println("Height (edges):    " + height(root));          // 2
        System.out.println("Max depth (nodes): " + maxDepthNodes(root));   // 3
        System.out.println("Height iterative:  " + heightIterative(root)); // 3
        System.out.println("Depth of node 5:   " + depthOf(root, 5));     // 2
        System.out.println("Depth of root 4:   " + depthOf(root, 4));     // 0
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIP                                               │
 * │                                                             │
 * │ When an interviewer asks about "height" or "depth," your    │
 * │ FIRST move should be: "Are we counting edges or nodes?"     │
 * │ This shows maturity and avoids off-by-one bugs.             │
 * │                                                             │
 * │ LeetCode 104 (Maximum Depth) counts nodes.                 │
 * │ Academic definitions typically count edges.                 │
 * │ Know both conventions cold.                                 │
 * └─────────────────────────────────────────────────────────────┘
 */

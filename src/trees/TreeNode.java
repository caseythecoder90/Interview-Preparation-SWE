package trees;

/**
 * ============================================================
 * SECTION 1.1: TreeNode — The Building Block of Every Tree Problem
 * ============================================================
 *
 * This is the standard binary tree node used in 95%+ of interview problems.
 * LeetCode uses this exact structure. Memorize it — you'll write it on a
 * whiteboard without thinking.
 */
public class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;

    // Default constructor (rarely used, but some problems need it)
    TreeNode() {}

    // Most common: create a node with just a value
    TreeNode(int val) {
        this.val = val;
    }

    // Full constructor: value + children (useful for building test trees quickly)
    TreeNode(int val, TreeNode left, TreeNode right) {
        this.val = val;
        this.left = left;
        this.right = right;
    }

    /**
     * Helper: build a quick test tree for local testing.
     *
     * Example usage:
     *     TreeNode root = new TreeNode(4,
     *         new TreeNode(2,
     *             new TreeNode(1),
     *             new TreeNode(3)),
     *         new TreeNode(6,
     *             new TreeNode(5),
     *             new TreeNode(7)));
     *
     * This builds:
     *           4
     *          / \
     *         2   6
     *        / \ / \
     *       1  3 5  7
     */

    @Override
    public String toString() {
        return "TreeNode{val=" + val + "}";
    }
}

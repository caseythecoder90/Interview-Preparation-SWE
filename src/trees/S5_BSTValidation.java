package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 5: BST VALIDATION & PROPERTIES
 * ============================================================
 */
public class S5_BSTValidation {

    // =============================================================
    // 5.1  VALIDATE BST (LC 98) ← VERY COMMON INTERVIEW QUESTION
    // =============================================================
    //
    // ⚠️  THE WRONG APPROACH (the mistake 50% of candidates make):
    //
    //   Checking only immediate children: left.val < node.val < right.val
    //
    //   This FAILS on trees like:
    //           5
    //          / \
    //         1   6
    //            / \
    //           3   7     ← 3 is in the RIGHT subtree of 5, but 3 < 5. INVALID!
    //
    //   Checking only immediate children would say this is valid. It's NOT.
    //
    // ✅ THE CORRECT APPROACH: Pass min/max BOUNDS down the recursion.
    //
    //   Every node must satisfy: min < node.val < max
    //   - When going LEFT:  the current node becomes the new UPPER bound
    //   - When going RIGHT: the current node becomes the new LOWER bound
    //   - Start with bounds of (-∞, +∞)

    /**
     * Validate BST using min/max bounds.
     *
     * Time:  O(n) — visit every node once
     * Space: O(h) — recursion stack
     */
    public static boolean isValidBST(TreeNode root) {
        return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private static boolean validate(TreeNode node, long min, long max) {
        if (node == null) return true; // empty tree is valid

        // Current node must be strictly within bounds
        if (node.val <= min || node.val >= max) return false;

        // Left subtree: all values must be < node.val (node becomes upper bound)
        // Right subtree: all values must be > node.val (node becomes lower bound)
        return validate(node.left, min, node.val)
            && validate(node.right, node.val, max);
    }

    /**
     * ALTERNATIVE: Validate BST using inorder traversal.
     *
     * If inorder traversal produces a strictly increasing sequence, it's a valid BST.
     * Track the previous value and ensure each new value is strictly greater.
     *
     * This is often easier to code under pressure and less error-prone than bounds.
     */
    public static boolean isValidBSTInorder(TreeNode root) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode curr = root;
        long prev = Long.MIN_VALUE; // use long to handle Integer.MIN_VALUE nodes

        while (curr != null || !stack.isEmpty()) {
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            curr = stack.pop();

            // In a valid BST, inorder values must be strictly increasing
            if (curr.val <= prev) return false;
            prev = curr.val;

            curr = curr.right;
        }
        return true;
    }

    // =============================================================
    // 5.2  KTH SMALLEST ELEMENT IN A BST (LC 230)
    // =============================================================
    //
    // Insight: inorder traversal of a BST gives sorted order.
    // So the kth smallest = the kth node visited during inorder.
    //
    // Stop as soon as we've visited k nodes — no need to traverse the whole tree.
    //
    // Time:  O(h + k) — go to the leftmost leaf (h steps), then visit k nodes
    // Space: O(h) for the stack

    /** ITERATIVE — stop early once we've found the kth element. */
    public static int kthSmallest(TreeNode root, int k) {
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode curr = root;

        while (curr != null || !stack.isEmpty()) {
            // Go as far left as possible
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }

            curr = stack.pop();
            k--;
            if (k == 0) return curr.val; // found the kth smallest!

            curr = curr.right;
        }
        return -1; // should never reach here if k is valid
    }

    /** RECURSIVE — uses an instance counter. Cleaner with a helper class or array trick. */
    private static int count;
    private static int result;

    public static int kthSmallestRecursive(TreeNode root, int k) {
        count = k;
        result = -1;
        kthHelper(root);
        return result;
    }

    private static void kthHelper(TreeNode node) {
        if (node == null || count <= 0) return; // prune: stop once found

        kthHelper(node.left);

        count--;
        if (count == 0) {
            result = node.val;
            return; // early exit
        }

        kthHelper(node.right);
    }

    // =============================================================
    // 5.3a  LOWEST COMMON ANCESTOR OF A BST (LC 235)
    // =============================================================
    //
    // In a BST, we can exploit the ordering property:
    //   - If both p and q are LESS than current → LCA is in the left subtree
    //   - If both p and q are GREATER than current → LCA is in the right subtree
    //   - If they SPLIT (one left, one right) or one EQUALS current → current IS the LCA
    //
    // This is O(h) time, O(1) space iterative. Beautiful.

    public static TreeNode lcaBST(TreeNode root, TreeNode p, TreeNode q) {
        while (root != null) {
            if (p.val < root.val && q.val < root.val) {
                root = root.left;   // both smaller → go left
            } else if (p.val > root.val && q.val > root.val) {
                root = root.right;  // both larger → go right
            } else {
                return root;        // split point = LCA
            }
        }
        return null; // shouldn't happen if p and q exist in tree
    }

    // =============================================================
    // 5.3b  LOWEST COMMON ANCESTOR OF A BINARY TREE (LC 236)
    // =============================================================
    //
    // For a GENERAL binary tree (no BST ordering), we can't use the
    // left/right comparison trick. Instead:
    //
    // RECURSIVE POSTORDER approach:
    //   1. If current node is null, p, or q → return it.
    //   2. Recurse on left and right subtrees.
    //   3. If BOTH return non-null → current node is the LCA (split point).
    //   4. If only one returns non-null → propagate that result up.
    //
    // The intuition: the LCA is the deepest node that has p in one subtree
    // and q in the other (or is p/q itself).
    //
    // Time:  O(n) — must visit all nodes in worst case
    // Space: O(h) — recursion stack

    public static TreeNode lcaBinaryTree(TreeNode root, TreeNode p, TreeNode q) {
        // Base cases
        if (root == null) return null;
        if (root == p || root == q) return root;

        // Search both subtrees
        TreeNode left = lcaBinaryTree(root.left, p, q);
        TreeNode right = lcaBinaryTree(root.right, p, q);

        // If both subtrees found something, this node is the LCA
        if (left != null && right != null) return root;

        // Otherwise, return whichever side found something (or null)
        return (left != null) ? left : right;
    }

    // =============================================================
    // 5.4  CHECK IF BINARY TREE IS BALANCED (LC 110)
    // =============================================================
    //
    // A height-balanced tree: for EVERY node, the heights of its left
    // and right subtrees differ by at most 1.
    //
    // NAIVE approach: compute height for every node → O(n²)
    //
    // OPTIMAL approach: compute height and check balance in ONE PASS.
    // Return -1 as a sentinel to indicate "not balanced."
    //
    // Time:  O(n) — visit each node once
    // Space: O(h) — recursion stack

    public static boolean isBalanced(TreeNode root) {
        return checkHeight(root) != -1;
    }

    /**
     * Returns the height of the subtree if it's balanced, or -1 if not.
     * Using -1 as a sentinel avoids a separate boolean and lets us short-circuit.
     */
    private static int checkHeight(TreeNode node) {
        if (node == null) return 0;

        int leftHeight = checkHeight(node.left);
        if (leftHeight == -1) return -1; // left subtree is unbalanced — propagate failure

        int rightHeight = checkHeight(node.right);
        if (rightHeight == -1) return -1; // right subtree is unbalanced

        // Check balance at this node
        if (Math.abs(leftHeight - rightHeight) > 1) return -1;

        // Return height of this subtree
        return 1 + Math.max(leftHeight, rightHeight);
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        /*
         *           8
         *          / \
         *         4   12
         *        / \  / \
         *       2  6 10 14
         */
        TreeNode root = new TreeNode(8,
            new TreeNode(4,
                new TreeNode(2),
                new TreeNode(6)),
            new TreeNode(12,
                new TreeNode(10),
                new TreeNode(14)));

        System.out.println("=== VALIDATE BST ===");
        System.out.println("Is valid BST (bounds):  " + isValidBST(root));        // true
        System.out.println("Is valid BST (inorder): " + isValidBSTInorder(root));  // true

        // Break the BST property to test
        TreeNode bad = new TreeNode(5,
            new TreeNode(1),
            new TreeNode(6,
                new TreeNode(3),  // 3 < 5 but in right subtree — INVALID
                new TreeNode(7)));
        System.out.println("Bad tree valid?  " + isValidBST(bad));  // false

        System.out.println("\n=== KTH SMALLEST ===");
        System.out.println("1st smallest: " + kthSmallest(root, 1));  // 2
        System.out.println("3rd smallest: " + kthSmallest(root, 3));  // 6
        System.out.println("5th smallest: " + kthSmallest(root, 5));  // 10

        System.out.println("\n=== LCA (BST) ===");
        TreeNode p = root.left.left;   // node 2
        TreeNode q = root.left.right;  // node 6
        System.out.println("LCA of 2 and 6: " + lcaBST(root, p, q).val);  // 4

        p = root.left.left;   // node 2
        q = root.right.right; // node 14
        System.out.println("LCA of 2 and 14: " + lcaBST(root, p, q).val); // 8

        System.out.println("\n=== LCA (General Binary Tree) ===");
        p = root.left.left;   // node 2
        q = root.left.right;  // node 6
        System.out.println("LCA of 2 and 6: " + lcaBinaryTree(root, p, q).val);  // 4

        System.out.println("\n=== BALANCED CHECK ===");
        System.out.println("Is balanced: " + isBalanced(root)); // true

        TreeNode unbalanced = new TreeNode(1,
            new TreeNode(2,
                new TreeNode(3), null),
            null);
        System.out.println("Unbalanced:  " + isBalanced(unbalanced)); // false
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — BST VALIDATION & PROPERTIES                │
 * │                                                             │
 * │ 1. For Validate BST: if you mention the bounds approach,    │
 * │    explain WHY checking immediate children fails. This is   │
 * │    exactly what the interviewer wants to hear.              │
 * │                                                             │
 * │ 2. Use LONG instead of INT for bounds to handle edge cases  │
 * │    where node values are Integer.MIN_VALUE or MAX_VALUE.    │
 * │    This is a common trip-up.                                │
 * │                                                             │
 * │ 3. For Kth Smallest: mention that if this operation is      │
 * │    called frequently, you could augment each node with a    │
 * │    "count of nodes in left subtree" for O(h) lookup. Shows  │
 * │    you think about system design.                           │
 * │                                                             │
 * │ 4. For LCA: the BST version is O(h) and the BT version is  │
 * │    O(n). Always clarify whether the tree is a BST — it      │
 * │    dramatically changes the optimal approach.               │
 * │                                                             │
 * │ 5. For balanced check: the -1 sentinel trick is elegant and │
 * │    converts an O(n²) naive solution into O(n). This "return │
 * │    multiple pieces of info from recursion" pattern appears  │
 * │    in diameter and max path sum too.                        │
 * └─────────────────────────────────────────────────────────────┘
 */

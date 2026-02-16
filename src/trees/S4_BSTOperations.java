package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 4: BST OPERATIONS
 * ============================================================
 *
 * Key complexity note for ALL BST operations:
 *   Time:  O(h) where h = height of the tree
 *          - Balanced BST:   h = O(log n) → operations are O(log n)
 *          - Skewed BST:     h = O(n) → operations degrade to O(n)
 *   Space: O(h) for recursive, O(1) for iterative
 *
 * This is why self-balancing BSTs (AVL, Red-Black) exist — they guarantee
 * O(log n) height. In interviews you won't implement balancing, but you
 * should know the complexity depends on the tree's shape.
 */
public class S4_BSTOperations {

    // =============================================================
    // 4.1  SEARCH IN A BST (LC 700)
    // =============================================================
    //
    // The BST property lets us eliminate half the tree at each step.
    // Go left if target < current, right if target > current.

    /** RECURSIVE search — clean and elegant. */
    public static TreeNode searchRecursive(TreeNode root, int target) {
        if (root == null) return null;                   // not found
        if (target == root.val) return root;             // found it
        if (target < root.val) return searchRecursive(root.left, target);  // go left
        return searchRecursive(root.right, target);      // go right
    }

    /**
     * ITERATIVE search — preferred in interviews because it's O(1) space.
     * Same logic, just use a while loop instead of recursion.
     */
    public static TreeNode searchIterative(TreeNode root, int target) {
        if (root == null) return null;

        while (root != null && root.val != target) {
            if (target <= root.val) {
                root = root.left;
            } else {
                root = root.right;
            }
        }

        return root;
    }

    // =============================================================
    // 4.2  INSERT INTO A BST (LC 701)
    // =============================================================
    //
    // Search for where the value SHOULD be (using BST property).
    // When you hit null, that's where you insert.
    // Note: there are multiple valid BSTs after insertion; any position
    // that maintains the BST property is correct.

    /** RECURSIVE — returns the (possibly new) root of the tree. */
    public static TreeNode insertRecursive(TreeNode root, int val) {
        if (root == null) return new TreeNode(val); // found the insertion point

        if (val < root.val) {
            root.left = insertRecursive(root.left, val);   // insert into left subtree
        } else {
            root.right = insertRecursive(root.right, val); // insert into right subtree
        }
        return root; // return unchanged root to parent
    }

    /** ITERATIVE — O(1) space, tracks parent to attach the new node. */
    public static TreeNode insertIterative(TreeNode root, int val) {
        TreeNode newNode = new TreeNode(val);
        if (root == null) return newNode;

        TreeNode curr = root, parent = null;
        while (curr != null) {
            parent = curr;
            curr = (val < curr.val) ? curr.left : curr.right;
        }
        // Now parent is the node where we attach the new node
        if (val < parent.val) {
            parent.left = newNode;
        } else {
            parent.right = newNode;
        }
        return root;
    }

    // =============================================================
    // 4.3  DELETE FROM A BST (LC 450) ← COMMON INTERVIEW QUESTION
    // =============================================================
    //
    // Three cases when deleting a node:
    //
    // CASE 1: Node is a LEAF (no children)
    //   → Simply remove it (return null to parent).
    //
    // CASE 2: Node has ONE child
    //   → Replace the node with its child (bypass it).
    //
    // CASE 3: Node has TWO children ← the hard case
    //   → Find the INORDER SUCCESSOR (smallest node in right subtree).
    //   → Copy the successor's value into the node being deleted.
    //   → Delete the successor from the right subtree (which reduces
    //     to Case 1 or Case 2, since the successor has no left child).
    //
    // Why inorder successor? Because it's the smallest value that's still
    // larger than the deleted node — it preserves the BST property.
    // You could also use the inorder predecessor (largest in left subtree).

    public static TreeNode deleteNode(TreeNode root, int key) {
        if (root == null) return null;

        // Step 1: Find the node to delete (standard BST search)
        if (key < root.val) {
            root.left = deleteNode(root.left, key);
        } else if (key > root.val) {
            root.right = deleteNode(root.right, key);
        } else {
            // Found the node to delete (root.val == key)

            // Case 1 & 2: zero or one child
            if (root.left == null) return root.right;  // replace with right child (or null)
            if (root.right == null) return root.left;   // replace with left child

            // Case 3: two children
            // Find inorder successor (leftmost node in right subtree)
            TreeNode successor = findMin(root.right);
            // Copy successor's value to current node
            root.val = successor.val;
            // Delete the successor from the right subtree
            root.right = deleteNode(root.right, successor.val);
        }
        return root;
    }

    // =============================================================
    // 4.4  FIND MIN AND MAX IN A BST
    // =============================================================
    //
    // MIN: keep going left until you can't. The leftmost node is the smallest.
    // MAX: keep going right until you can't. The rightmost node is the largest.
    //
    // Time:  O(h)
    // Space: O(1) iterative

    public static TreeNode findMin(TreeNode root) {
        // Guard against null input
        if (root == null) return null;
        while (root.left != null) {
            root = root.left;
        }
        return root;
    }

    public static TreeNode findMax(TreeNode root) {
        if (root == null) return null;
        while (root.right != null) {
            root = root.right;
        }
        return root;
    }

    // =============================================================
    // 4.5  INORDER SUCCESSOR IN BST
    // =============================================================
    //
    // The inorder successor of a node is the node with the SMALLEST value
    // that is GREATER than the given node's value.
    //
    // Two cases:
    //
    // CASE 1: Node has a RIGHT subtree
    //   → Successor is the LEFTMOST node in the right subtree (the min).
    //
    // CASE 2: Node has NO right subtree
    //   → Successor is the nearest ANCESTOR for which this node is in
    //     the LEFT subtree. (We went "up and to the right" to find it.)
    //   → Track this by searching from root: every time we go left,
    //     the current node could be the successor.

    /**
     * Find inorder successor given the root and a target node.
     *
     * We search from root to find the target, tracking the potential
     * successor along the way.
     *
     * Time:  O(h)
     * Space: O(1)
     */
    public static TreeNode inorderSuccessor(TreeNode root, TreeNode target) {
        // Case 1: right subtree exists
        if (target.right != null) {
            return findMin(target.right);
        }

        // Case 2: search from root, tracking the last "left turn"
        TreeNode successor = null;
        TreeNode curr = root;
        while (curr != null) {
            if (target.val < curr.val) {
                successor = curr;   // curr is a candidate — it's greater than target
                curr = curr.left;   // but maybe there's a smaller one
            } else if (target.val > curr.val) {
                curr = curr.right;  // target is to the right, curr can't be successor
            } else {
                break; // found the target node
            }
        }
        return successor;
    }

    // =============================================================
    // 4.6  INORDER PREDECESSOR IN BST
    // =============================================================
    //
    // Mirror of successor:
    //
    // CASE 1: Node has a LEFT subtree
    //   → Predecessor is the RIGHTMOST node in the left subtree (the max).
    //
    // CASE 2: Node has NO left subtree
    //   → Predecessor is the nearest ancestor for which this node is in
    //     the RIGHT subtree. Every time we go right, the current node
    //     could be the predecessor.

    public static TreeNode inorderPredecessor(TreeNode root, TreeNode target) {
        // Case 1: left subtree exists
        if (target.left != null) {
            return findMax(target.left);
        }

        // Case 2: search from root, tracking the last "right turn"
        TreeNode predecessor = null;
        TreeNode curr = root;
        while (curr != null) {
            if (target.val > curr.val) {
                predecessor = curr;  // curr is a candidate — it's less than target
                curr = curr.right;   // but maybe there's a larger one
            } else if (target.val < curr.val) {
                curr = curr.left;    // target is to the left, curr can't be predecessor
            } else {
                break; // found the target node
            }
        }
        return predecessor;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        /*
         * Build this BST:
         *           8
         *          / \
         *         4   12
         *        / \  / \
         *       2  6 10 14
         *      / \
         *     1   3
         */
        TreeNode root = new TreeNode(8,
            new TreeNode(4,
                new TreeNode(2, new TreeNode(1), new TreeNode(3)),
                new TreeNode(6)),
            new TreeNode(12,
                new TreeNode(10),
                new TreeNode(14)));

        // Search
        System.out.println("=== SEARCH ===");
        System.out.println("Search 6: " + (searchIterative(root, 6) != null));  // true
        System.out.println("Search 7: " + (searchIterative(root, 7) != null));  // false

        // Min / Max
        System.out.println("\n=== MIN / MAX ===");
        System.out.println("Min: " + findMin(root).val);  // 1
        System.out.println("Max: " + findMax(root).val);  // 14

        // Successor / Predecessor
        System.out.println("\n=== SUCCESSOR / PREDECESSOR ===");
        TreeNode node4 = root.left; // the node with value 4
        System.out.println("Successor of 4:    " + inorderSuccessor(root, node4).val);    // 6
        System.out.println("Predecessor of 4:  " + inorderPredecessor(root, node4).val);  // 3

        // Insert
        System.out.println("\n=== INSERT 5 ===");
        insertRecursive(root, 5);
        // 5 should go to the left of 6
        System.out.println("6's left child after insert: " + root.left.right.left.val); // 5

        // Delete
        System.out.println("\n=== DELETE 4 (two children) ===");
        root = deleteNode(root, 4);
        System.out.println("Root's left after deleting 4: " + root.left.val); // 5 (successor replaces 4)
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — BST OPERATIONS                             │
 * │                                                             │
 * │ 1. DELETE is the #1 most-asked BST operation. Practice the  │
 * │    two-children case until you can write it without thinking.│
 * │    The key insight: copy successor's VALUE, then delete the  │
 * │    successor. Don't try to rearrange pointers.              │
 * │                                                             │
 * │ 2. Always mention the O(h) complexity and explain that h is │
 * │    O(log n) for balanced trees but O(n) for skewed trees.   │
 * │    This shows you understand the real-world implications.   │
 * │                                                             │
 * │ 3. For successor/predecessor, draw the two cases on the     │
 * │    whiteboard. Interviewers love visual explanations here.  │
 * │                                                             │
 * │ 4. The recursive insert/delete pattern of returning the     │
 * │    (possibly new) root is elegant and avoids parent pointer │
 * │    bookkeeping. Use it: root.left = delete(root.left, key). │
 * │                                                             │
 * │ 5. Edge case candidates miss: deleting the root itself.     │
 * │    Make sure your code handles this — test it mentally.     │
 * └─────────────────────────────────────────────────────────────┘
 */

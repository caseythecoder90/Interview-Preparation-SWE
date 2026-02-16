package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 2: DEPTH-FIRST SEARCH (DFS) TRAVERSALS
 * ============================================================
 *
 * The three DFS traversals differ only in WHEN you process the current node
 * relative to its children:
 *
 *   INORDER:   Left  → Node → Right   (gives sorted order for BST)
 *   PREORDER:  Node  → Left → Right   (root always first — good for serialization)
 *   POSTORDER: Left  → Right → Node   (root always last — good for deletion/eval)
 *
 * Every tree problem is fundamentally one of these patterns with extra logic
 * bolted on. Master these and you can solve ~70% of tree interview questions.
 */
public class S2_DFSTraversals {

    // =============================================================
    // 2.1  INORDER TRAVERSAL  (Left → Node → Right)
    // =============================================================
    //
    // WHY IT MATTERS:
    //   Inorder on a BST produces values in SORTED (ascending) order.
    //   This is the single most important property to know for BST interviews.
    //   Many BST problems (kth smallest, validate BST, etc.) reduce to
    //   "do an inorder traversal."

    /** RECURSIVE — the natural, clean version. Write this first in interviews. */
    public static List<Integer> inorderRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderHelper(root, result);
        return result;
    }

    private static void inorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorderHelper(node.left, result);   // L
        result.add(node.val);               // N (process current node)
        inorderHelper(node.right, result);  // R
    }

    /**
     * ITERATIVE — using an explicit stack.
     *
     * The key insight: we need to go as far left as possible, then process,
     * then move to the right child and repeat.
     *
     * Pattern:
     *   1. Push all left children onto the stack (go deep left).
     *   2. Pop → that's the node to process.
     *   3. Move to its right child, then repeat step 1.
     *
     * This pattern is extremely common. You'll reuse it for kth smallest,
     * BST iterator, and many other problems.
     *
     * Time:  O(n) — every node is pushed and popped exactly once
     * Space: O(h) — stack holds at most h nodes (height of tree)
     */
    public static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode curr = root;

        while (curr != null || !stack.isEmpty()) {
            // Step 1: drill all the way left
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            // Step 2: pop and process
            curr = stack.pop();
            result.add(curr.val);
            // Step 3: move to right subtree
            curr = curr.right;
        }
        return result;
    }

    // =============================================================
    // 2.2  PREORDER TRAVERSAL  (Node → Left → Right)
    // =============================================================
    //
    // WHY IT MATTERS:
    //   - The root is always visited FIRST. This makes preorder ideal for:
    //     • Serialization (Section 7): save the root, then its children.
    //     • Copying/cloning a tree: create the node first, then build children.
    //   - Preorder + Inorder together can uniquely reconstruct a tree (Section 8.2).

    /** RECURSIVE */
    public static List<Integer> preorderRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderHelper(root, result);
        return result;
    }

    private static void preorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.val);                // N (process FIRST)
        preorderHelper(node.left, result);   // L
        preorderHelper(node.right, result);  // R
    }

    /**
     * ITERATIVE — the simplest iterative traversal.
     *
     * Push root. While stack not empty: pop, process, push RIGHT then LEFT.
     * (Push right first so left is processed first — LIFO.)
     *
     * Time:  O(n)
     * Space: O(h) — but can be O(n) in worst case for skewed trees
     */
    public static List<Integer> preorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.add(node.val);                         // N
            if (node.right != null) stack.push(node.right); // push R first
            if (node.left != null)  stack.push(node.left);  // so L is popped first
        }
        return result;
    }

    // =============================================================
    // 2.3  POSTORDER TRAVERSAL  (Left → Right → Node)
    // =============================================================
    //
    // WHY IT MATTERS:
    //   - The root is visited LAST. This is the natural order for:
    //     • Deleting a tree: delete children before the parent.
    //     • Evaluating expression trees: compute operands before operator.
    //     • Computing height/depth/size: you need children's info before deciding.
    //   - Many "return info from children, then decide at current node" problems
    //     are implicitly postorder (diameter, balanced check, max path sum).

    /** RECURSIVE */
    public static List<Integer> postorderRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        postorderHelper(root, result);
        return result;
    }

    private static void postorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        postorderHelper(node.left, result);   // L
        postorderHelper(node.right, result);  // R
        result.add(node.val);                 // N (process LAST)
    }

    /**
     * ITERATIVE — this is the tricky one. Here's why:
     *
     * In postorder we need to process a node AFTER both its children.
     * When we pop a node from the stack, we don't know if we've already
     * visited its right child or not. We need a way to track this.
     *
     * APPROACH: "Modified preorder with reverse"
     *   Preorder  = Node → Left → Right
     *   If we do   Node → Right → Left   (swap push order)
     *   Then reverse the result: Left → Right → Node = POSTORDER!
     *
     * This is the easiest iterative postorder to remember for interviews.
     *
     * Time:  O(n)
     * Space: O(n) — because we store the full result before reversing
     */
    public static List<Integer> postorderIterativeReverse(TreeNode root) {
        LinkedList<Integer> result = new LinkedList<>();
        if (root == null) return result;

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.addFirst(node.val);  // add to FRONT (effectively reversing)
            // Push left first so right is processed first (reverse of preorder)
            if (node.left != null)  stack.push(node.left);
            if (node.right != null) stack.push(node.right);
        }
        return result;
    }

    /**
     * ITERATIVE POSTORDER — "proper" version using a prev pointer.
     *
     * This version uses a single stack and tracks the previously processed node
     * to determine whether we're returning from the left or right subtree.
     *
     * When do we process a node? Only when:
     *   (a) it has no children (leaf), OR
     *   (b) its right child was the last node we processed (meaning we've done both subtrees), OR
     *   (c) its left child was the last node we processed AND it has no right child.
     *
     * This is harder to memorize but doesn't require reversal.
     *
     * Time:  O(n)
     * Space: O(h)
     */
    public static List<Integer> postorderIterativeProper(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);
        TreeNode prev = null; // tracks the last node we processed

        while (!stack.isEmpty()) {
            TreeNode curr = stack.peek();

            // Going DOWN the tree: push children
            if (prev == null || prev.left == curr || prev.right == curr) {
                // We arrived here from the parent — go deeper
                if (curr.left != null) {
                    stack.push(curr.left);
                } else if (curr.right != null) {
                    stack.push(curr.right);
                } else {
                    // Leaf node — process it
                    result.add(curr.val);
                    stack.pop();
                }
            }
            // Coming UP from left child
            else if (curr.left == prev) {
                if (curr.right != null) {
                    stack.push(curr.right); // still need to visit right subtree
                } else {
                    result.add(curr.val);
                    stack.pop();
                }
            }
            // Coming UP from right child — both subtrees done
            else {
                result.add(curr.val);
                stack.pop();
            }
            prev = curr;
        }
        return result;
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

        System.out.println("=== INORDER (sorted for BST) ===");
        System.out.println("Recursive:  " + inorderRecursive(root));   // [1,2,3,4,5,6,7]
        System.out.println("Iterative:  " + inorderIterative(root));   // [1,2,3,4,5,6,7]

        System.out.println("\n=== PREORDER (root first) ===");
        System.out.println("Recursive:  " + preorderRecursive(root));  // [4,2,1,3,6,5,7]
        System.out.println("Iterative:  " + preorderIterative(root));  // [4,2,1,3,6,5,7]

        System.out.println("\n=== POSTORDER (root last) ===");
        System.out.println("Recursive:  " + postorderRecursive(root));          // [1,3,2,5,7,6,4]
        System.out.println("Iter(rev):  " + postorderIterativeReverse(root));   // [1,3,2,5,7,6,4]
        System.out.println("Iter(prop): " + postorderIterativeProper(root));    // [1,3,2,5,7,6,4]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — DFS TRAVERSALS                             │
 * │                                                             │
 * │ 1. Always start by writing the RECURSIVE version. It's 3    │
 * │    lines and nearly impossible to get wrong. Then offer the  │
 * │    iterative version if asked.                              │
 * │                                                             │
 * │ 2. The iterative INORDER pattern (push-all-left, pop,      │
 * │    go-right) is the most important to memorize. It's the    │
 * │    backbone of BST Iterator, Kth Smallest, and Validate BST │
 * │    iterative solutions.                                     │
 * │                                                             │
 * │ 3. For iterative POSTORDER, the "reverse modified preorder" │
 * │    trick is much easier to code under pressure. Use it.     │
 * │                                                             │
 * │ 4. Common follow-up: "Can you do it in O(1) space?"        │
 * │    → Morris Traversal (uses threading). Know it exists,     │
 * │      but it's rarely expected in a 45-min round.            │
 * │                                                             │
 * │ 5. When you see ANY tree problem, ask yourself: "Is this    │
 * │    just a traversal with extra bookkeeping?" Usually yes.   │
 * └─────────────────────────────────────────────────────────────┘
 */

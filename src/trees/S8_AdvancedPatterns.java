package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 8: ADVANCED TREE PATTERNS
 * ============================================================
 */
public class S8_AdvancedPatterns {

    // =============================================================
    // 8.1  SORTED ARRAY TO BALANCED BST (LC 108)
    // =============================================================
    //
    // Given a sorted array, build a HEIGHT-BALANCED BST.
    //
    // Strategy: binary search / divide and conquer.
    //   - The middle element becomes the root (ensures balance).
    //   - The left half becomes the left subtree.
    //   - The right half becomes the right subtree.
    //   - Recurse.
    //
    // This is the same idea behind building a segment tree or merge sort.
    //
    // Time:  O(n) — each element is visited once
    // Space: O(log n) — recursion depth for a balanced result

    public static TreeNode sortedArrayToBST(int[] nums) {
        return buildBST(nums, 0, nums.length - 1);
    }

    private static TreeNode buildBST(int[] nums, int left, int right) {
        if (left > right) return null;

        int mid = left + (right - left) / 2; // avoid integer overflow
        TreeNode node = new TreeNode(nums[mid]);
        node.left = buildBST(nums, left, mid - 1);
        node.right = buildBST(nums, mid + 1, right);
        return node;
    }

    // =============================================================
    // 8.2  CONSTRUCT BT FROM PREORDER AND INORDER (LC 105)
    // =============================================================
    //
    // Given preorder and inorder traversals, reconstruct the unique binary tree.
    //
    // Key insight:
    //   - Preorder[0] is always the ROOT.
    //   - Find that root in inorder → everything LEFT of it is the left subtree,
    //     everything RIGHT of it is the right subtree.
    //   - Use a HashMap for O(1) lookup of the root's position in inorder.
    //   - Recurse: use the sizes of left/right subtrees to partition both arrays.
    //
    // Time:  O(n) — with HashMap for inorder index lookup
    // Space: O(n) — HashMap + recursion stack

    private static int preIndex; // tracks current position in preorder array

    public static TreeNode buildTree(int[] preorder, int[] inorder) {
        // Map each value to its index in inorder for O(1) lookup
        Map<Integer, Integer> inorderMap = new HashMap<>();
        for (int i = 0; i < inorder.length; i++) {
            inorderMap.put(inorder[i], i);
        }

        preIndex = 0;
        return buildTreeHelper(preorder, inorderMap, 0, inorder.length - 1);
    }

    private static TreeNode buildTreeHelper(int[] preorder,
                                             Map<Integer, Integer> inorderMap,
                                             int inLeft, int inRight) {
        if (inLeft > inRight) return null;

        // The current root is the next element in preorder
        int rootVal = preorder[preIndex++];
        TreeNode root = new TreeNode(rootVal);

        // Find root's position in inorder
        int inorderIdx = inorderMap.get(rootVal);

        // Build left subtree first (preorder is Node→Left→Right)
        // Left subtree uses inorder[inLeft .. inorderIdx-1]
        root.left = buildTreeHelper(preorder, inorderMap, inLeft, inorderIdx - 1);

        // Then right subtree
        // Right subtree uses inorder[inorderIdx+1 .. inRight]
        root.right = buildTreeHelper(preorder, inorderMap, inorderIdx + 1, inRight);

        return root;
    }

    // =============================================================
    // 8.3  FLATTEN BINARY TREE TO LINKED LIST (LC 114)
    // =============================================================
    //
    // Flatten the tree IN-PLACE into a "linked list" that uses the RIGHT
    // pointer (left is always null), in PREORDER.
    //
    //      1              1
    //     / \              \
    //    2   5     →        2
    //   / \   \              \
    //  3   4   6              3
    //                          \
    //                           4
    //                            \
    //                             5
    //                              \
    //                               6
    //
    // Strategy: reverse postorder (right → left → node).
    // We maintain a "prev" pointer to the previously processed node.
    // For each node, set its right to prev, left to null.
    //
    // Time:  O(n)
    // Space: O(h) recursion stack

    private static TreeNode prev;

    public static void flatten(TreeNode root) {
        prev = null;
        flattenHelper(root);
    }

    private static void flattenHelper(TreeNode node) {
        if (node == null) return;

        // Process right first, then left, then current (reverse preorder)
        flattenHelper(node.right);
        flattenHelper(node.left);

        // Now "prev" is the head of the already-flattened rest of the list
        node.right = prev;
        node.left = null;
        prev = node;
    }

    /**
     * ITERATIVE flatten — O(1) space (no recursion stack).
     * Uses the "Morris-like" approach: for each node with a left child,
     * find the rightmost node in the left subtree, attach the right subtree
     * there, then move the left subtree to the right.
     */
    public static void flattenIterative(TreeNode root) {
        TreeNode curr = root;
        while (curr != null) {
            if (curr.left != null) {
                // Find the rightmost node in the left subtree
                TreeNode rightmost = curr.left;
                while (rightmost.right != null) {
                    rightmost = rightmost.right;
                }
                // Attach the right subtree to the rightmost node of the left subtree
                rightmost.right = curr.right;
                // Move the left subtree to the right
                curr.right = curr.left;
                curr.left = null;
            }
            curr = curr.right;
        }
    }

    // =============================================================
    // 8.4  TREE VIEWS (Top, Bottom, Left, Right)
    // =============================================================
    //
    // These use BFS with column tracking (horizontal distance from root).
    // Root is at column 0. Left child → column-1. Right child → column+1.
    //
    // TOP VIEW:    first node encountered at each column (BFS order)
    // BOTTOM VIEW: last node encountered at each column (BFS order)
    // LEFT VIEW:   first node at each LEVEL (same as Section 3.3, i == 0)
    // RIGHT VIEW:  last node at each LEVEL (same as Section 3.3, i == levelSize-1)

    /** TOP VIEW: For each column, return the first node seen (via BFS). */
    public static List<Integer> topView(TreeNode root) {
        if (root == null) return new ArrayList<>();

        // TreeMap keeps columns sorted for ordered output
        TreeMap<Integer, Integer> columnMap = new TreeMap<>();
        // Queue stores [node, column] pairs
        Queue<int[]> queue = new LinkedList<>();
        // We need node references too; use parallel approach
        Queue<TreeNode> nodeQueue = new LinkedList<>();
        Queue<Integer> colQueue = new LinkedList<>();

        nodeQueue.offer(root);
        colQueue.offer(0);

        while (!nodeQueue.isEmpty()) {
            TreeNode node = nodeQueue.poll();
            int col = colQueue.poll();

            // TOP view: only record the FIRST node at each column
            columnMap.putIfAbsent(col, node.val);

            if (node.left != null) {
                nodeQueue.offer(node.left);
                colQueue.offer(col - 1);
            }
            if (node.right != null) {
                nodeQueue.offer(node.right);
                colQueue.offer(col + 1);
            }
        }
        return new ArrayList<>(columnMap.values());
    }

    /** BOTTOM VIEW: For each column, return the last node seen (via BFS). */
    public static List<Integer> bottomView(TreeNode root) {
        if (root == null) return new ArrayList<>();

        TreeMap<Integer, Integer> columnMap = new TreeMap<>();
        Queue<TreeNode> nodeQueue = new LinkedList<>();
        Queue<Integer> colQueue = new LinkedList<>();

        nodeQueue.offer(root);
        colQueue.offer(0);

        while (!nodeQueue.isEmpty()) {
            TreeNode node = nodeQueue.poll();
            int col = colQueue.poll();

            // BOTTOM view: always overwrite — last node at each column wins
            columnMap.put(col, node.val);

            if (node.left != null) {
                nodeQueue.offer(node.left);
                colQueue.offer(col - 1);
            }
            if (node.right != null) {
                nodeQueue.offer(node.right);
                colQueue.offer(col + 1);
            }
        }
        return new ArrayList<>(columnMap.values());
    }

    // =============================================================
    // 8.5  COUNT NODES IN A COMPLETE BINARY TREE (LC 222)
    // =============================================================
    //
    // Naive: O(n) — just count all nodes.
    // Optimal: O(log²n) — exploit the "complete" property.
    //
    // Key insight: in a complete binary tree, at least one of the two subtrees
    // is a PERFECT binary tree (all levels full).
    //
    //   - Compute height going all-left and all-right.
    //   - If they're equal → the tree is PERFECT → node count = 2^h - 1.
    //   - If not → recurse on both subtrees. One of them will be perfect
    //     (giving O(1) for that side), so we only recurse on one "real" side.
    //
    // Total: O(log n) recursive calls, each computing height in O(log n) → O(log²n).

    public static int countNodes(TreeNode root) {
        if (root == null) return 0;

        int leftHeight = leftHeight(root);
        int rightHeight = rightHeight(root);

        if (leftHeight == rightHeight) {
            // Perfect binary tree: 2^h - 1 nodes
            // (1 << leftHeight) computes 2^leftHeight
            return (1 << leftHeight) - 1;
        }

        // Not perfect: recurse on both subtrees + 1 for current node
        return 1 + countNodes(root.left) + countNodes(root.right);
    }

    /** Height measured by always going LEFT. */
    private static int leftHeight(TreeNode node) {
        int h = 0;
        while (node != null) {
            h++;
            node = node.left;
        }
        return h;
    }

    /** Height measured by always going RIGHT. */
    private static int rightHeight(TreeNode node) {
        int h = 0;
        while (node != null) {
            h++;
            node = node.right;
        }
        return h;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {
        System.out.println("=== SORTED ARRAY TO BST ===");
        int[] sorted = {1, 2, 3, 4, 5, 6, 7};
        TreeNode bst = sortedArrayToBST(sorted);
        System.out.println("Root: " + bst.val);         // 4 (middle element)
        System.out.println("Left: " + bst.left.val);    // 2
        System.out.println("Right: " + bst.right.val);  // 6

        System.out.println("\n=== CONSTRUCT FROM PREORDER + INORDER ===");
        int[] preorder = {4, 2, 1, 3, 6, 5, 7};
        int[] inorder =  {1, 2, 3, 4, 5, 6, 7};
        TreeNode built = buildTree(preorder, inorder);
        System.out.println("Root: " + built.val);                    // 4
        System.out.println("Root.left: " + built.left.val);         // 2
        System.out.println("Root.right: " + built.right.val);       // 6

        System.out.println("\n=== FLATTEN TO LINKED LIST ===");
        /*
         *      1
         *     / \
         *    2   5
         *   / \   \
         *  3   4   6
         */
        TreeNode flatRoot = new TreeNode(1,
            new TreeNode(2, new TreeNode(3), new TreeNode(4)),
            new TreeNode(5, null, new TreeNode(6)));
        flatten(flatRoot);
        System.out.print("Flattened: ");
        TreeNode curr = flatRoot;
        while (curr != null) {
            System.out.print(curr.val + " → ");
            curr = curr.right;
        }
        System.out.println("null");
        // 1 → 2 → 3 → 4 → 5 → 6 → null

        System.out.println("\n=== TOP VIEW / BOTTOM VIEW ===");
        /*
         *           4
         *          / \
         *         2   6
         *        / \ / \
         *       1  3 5  7
         */
        TreeNode viewRoot = new TreeNode(4,
            new TreeNode(2, new TreeNode(1), new TreeNode(3)),
            new TreeNode(6, new TreeNode(5), new TreeNode(7)));

        System.out.println("Top view:    " + topView(viewRoot));    // [1, 2, 4, 6, 7]
        System.out.println("Bottom view: " + bottomView(viewRoot)); // [1, 2, 5, 6, 7]

        System.out.println("\n=== COUNT NODES IN COMPLETE BT ===");
        /*
         * Complete binary tree:
         *       1
         *      / \
         *     2   3
         *    / \  /
         *   4  5 6
         */
        TreeNode complete = new TreeNode(1,
            new TreeNode(2, new TreeNode(4), new TreeNode(5)),
            new TreeNode(3, new TreeNode(6), null));
        System.out.println("Node count: " + countNodes(complete)); // 6
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — ADVANCED PATTERNS                          │
 * │                                                             │
 * │ 1. SORTED ARRAY → BST: Always pick the MIDDLE element as   │
 * │    root. This guarantees balance. If array has even length,  │
 * │    either middle works (left or right center). Don't        │
 * │    overthink it.                                            │
 * │                                                             │
 * │ 2. CONSTRUCT FROM TRAVERSALS: The HashMap for inorder index │
 * │    is what makes this O(n) instead of O(n²). If you forget  │
 * │    it and linear-scan inorder each time, you'll get TLE.    │
 * │    Also: preorder+inorder works, postorder+inorder works,   │
 * │    but preorder+postorder ONLY works if the tree is full.   │
 * │                                                             │
 * │ 3. FLATTEN: The reverse-postorder (right→left→node) trick   │
 * │    is elegant but hard to come up with on the spot. If you  │
 * │    blank, the iterative "attach right subtree to rightmost  │
 * │    of left" approach is more intuitive.                     │
 * │                                                             │
 * │ 4. TREE VIEWS: These are BFS + column/level tracking.       │
 * │    TreeMap<Integer, Integer> keyed by column is the standard │
 * │    approach. Know it — it comes up at Amazon, Google, Meta.  │
 * │                                                             │
 * │ 5. COUNT COMPLETE BT NODES: O(log²n) is the answer they     │
 * │    want. The key insight is that at least one subtree is     │
 * │    perfect. If you just say O(n) they'll push for better.   │
 * │    Bit shift (1 << h) for 2^h is a nice touch.             │
 * └─────────────────────────────────────────────────────────────┘
 */

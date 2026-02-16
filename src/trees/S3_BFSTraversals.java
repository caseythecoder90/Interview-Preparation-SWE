package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 3: BREADTH-FIRST SEARCH (BFS) TRAVERSALS
 * ============================================================
 *
 * BFS visits nodes level by level, left to right. It uses a QUEUE (FIFO).
 *
 * The core BFS pattern for trees:
 *   1. Add root to queue.
 *   2. While queue is not empty:
 *      a. Record queue.size() — this is the number of nodes at the current level.
 *      b. Process exactly that many nodes (poll, handle, enqueue children).
 *   3. The "process exactly size nodes" step is what lets you group by level.
 *
 * Almost every BFS tree problem is a variation of this template.
 */
public class S3_BFSTraversals {

    // =============================================================
    // 3.1  LEVEL-ORDER TRAVERSAL (LC 102)
    // =============================================================
    //
    // Returns nodes grouped by level: [[4], [2,6], [1,3,5,7]]
    //
    // Time:  O(n) — every node visited once
    // Space: O(w) — where w is the maximum width of the tree
    //         In the worst case (complete tree), the last level has ~n/2 nodes,
    //         so space is O(n).

    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size(); // ← KEY: snapshot the size BEFORE processing
            List<Integer> currentLevel = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                currentLevel.add(node.val);

                // Enqueue children for the NEXT level
                if (node.left != null)  queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(currentLevel);
        }
        return result;
    }

    // =============================================================
    // 3.2  ZIGZAG LEVEL-ORDER TRAVERSAL (LC 103)
    // =============================================================
    //
    // Same as level-order but alternating direction:
    //   Level 0: left → right   [4]
    //   Level 1: right → left   [6, 2]
    //   Level 2: left → right   [1, 3, 5, 7]
    //
    // Strategy: same BFS template, but on odd levels, add to the FRONT
    // of the current level list instead of the back.
    // (Alternatively, reverse odd levels after building them.)
    //
    // Time:  O(n)
    // Space: O(n)

    public static List<List<Integer>> zigzagLevelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        boolean leftToRight = true;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            LinkedList<Integer> currentLevel = new LinkedList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                // The direction toggle: addLast vs addFirst
                if (leftToRight) {
                    currentLevel.addLast(node.val);
                } else {
                    currentLevel.addFirst(node.val);
                }

                if (node.left != null)  queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(currentLevel);
            leftToRight = !leftToRight; // flip direction
        }
        return result;
    }

    // =============================================================
    // 3.3  RIGHT SIDE VIEW (LC 199)
    // =============================================================
    //
    // Imagine standing to the right of the tree. You see the LAST node
    // at each level. Return those nodes.
    //
    //           4           → you see 4
    //          / \
    //         2   6         → you see 6
    //        / \ / \
    //       1  3 5  7       → you see 7
    //
    // Result: [4, 6, 7]
    //
    // Strategy: BFS level-by-level. The last node processed in each level
    // is the rightmost, so just take the last element of each level.
    //
    // Alternative: DFS going right-first, tracking depth. But BFS is cleaner.
    //
    // Time:  O(n)
    // Space: O(w)

    public static List<Integer> rightSideView(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                // Last node in this level = visible from the right side
                if (i == levelSize - 1) {
                    result.add(node.val);
                }

                if (node.left != null)  queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
        }
        return result;
    }

    /**
     * RIGHT SIDE VIEW — DFS alternative.
     *
     * Visit right subtree first. The first node we encounter at each
     * depth is the rightmost node at that level.
     *
     * This is more space-efficient for tall, narrow trees (O(h) vs O(w)).
     */
    public static List<Integer> rightSideViewDFS(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        dfsRightView(root, 0, result);
        return result;
    }

    private static void dfsRightView(TreeNode node, int depth, List<Integer> result) {
        if (node == null) return;

        // If this is the first node we've seen at this depth, it's the rightmost
        if (depth == result.size()) {
            result.add(node.val);
        }

        dfsRightView(node.right, depth + 1, result); // visit right FIRST
        dfsRightView(node.left, depth + 1, result);
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

        System.out.println("=== LEVEL ORDER ===");
        System.out.println(levelOrder(root));
        // [[4], [2, 6], [1, 3, 5, 7]]

        System.out.println("\n=== ZIGZAG LEVEL ORDER ===");
        System.out.println(zigzagLevelOrder(root));
        // [[4], [6, 2], [1, 3, 5, 7]]

        System.out.println("\n=== RIGHT SIDE VIEW ===");
        System.out.println("BFS: " + rightSideView(root));       // [4, 6, 7]
        System.out.println("DFS: " + rightSideViewDFS(root));    // [4, 6, 7]

        // Edge case: tree with left-only path
        //    1
        //   /
        //  2
        // /
        // 3
        TreeNode leftOnly = new TreeNode(1,
            new TreeNode(2, new TreeNode(3), null), null);

        System.out.println("\nRight view of left-skewed tree: " + rightSideView(leftOnly));
        // [1, 2, 3] — even though they're all left children, they're the only nodes at each level
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — BFS TRAVERSALS                             │
 * │                                                             │
 * │ 1. The "snapshot levelSize = queue.size()" pattern is the   │
 * │    entire trick. Without it, you can't tell where one level │
 * │    ends and the next begins. ALWAYS use it.                 │
 * │                                                             │
 * │ 2. BFS on a tree NEVER needs a "visited" set (unlike BFS   │
 * │    on a graph). Trees are acyclic and directed downward,    │
 * │    so you can't revisit a node.                             │
 * │                                                             │
 * │ 3. "Left side view" is the same as right side view but take │
 * │    the FIRST element (i == 0) instead of the last.          │
 * │                                                             │
 * │ 4. Many problems that SEEM to need BFS can be solved with   │
 * │    DFS + depth tracking (often cleaner code). But BFS is    │
 * │    usually more intuitive to explain to an interviewer.      │
 * │                                                             │
 * │ 5. Common mistake: using `while (queue.size() > 0)` but     │
 * │    forgetting to snapshot the size. If you enqueue children  │
 * │    inside the loop without a size snapshot, you'll process   │
 * │    nodes from the next level in the same iteration.          │
 * └─────────────────────────────────────────────────────────────┘
 */

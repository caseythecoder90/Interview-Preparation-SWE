package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 3: STACK vs RECURSION
 * ============================================================
 *
 * CORE INSIGHT:
 *   Every recursive algorithm can be converted to an iterative one
 *   using an explicit stack. The JVM's call stack IS a stack —
 *   when you "go iterative," you're just managing that stack yourself.
 *
 *
 * -------------------------------------------------------
 * 3.1  WHY THE CALL STACK IS A STACK
 * -------------------------------------------------------
 *
 * When you call a recursive function:
 *   1. The JVM PUSHES a new stack frame (local variables, return address)
 *   2. The function executes
 *   3. On return, the JVM POPS the stack frame
 *
 * This is why infinite recursion throws StackOverflowError —
 * the call stack runs out of space (default ~512KB to 1MB in Java).
 *
 *
 * -------------------------------------------------------
 * 3.2  WHEN TO USE RECURSION vs EXPLICIT STACK
 * -------------------------------------------------------
 *
 * USE RECURSION when:
 *   - The code is cleaner and easier to understand
 *   - Tree depth is bounded (balanced trees: max ~30 levels for 10^9 nodes)
 *   - Interview setting where clarity matters most
 *   - The recursive solution is straightforward (DFS, divide-and-conquer)
 *
 * USE EXPLICIT STACK when:
 *   - Recursion depth could be very large (e.g., linked list of 10^6 nodes)
 *   - Interviewer explicitly asks for an iterative solution
 *   - You need fine-grained control over the traversal order
 *   - You're processing a very deep or unbalanced tree
 *   - You need to pause/resume traversal (iterators)
 *
 * INTERVIEW GUIDELINE:
 *   Start with recursive. If asked "can you do this iteratively?",
 *   convert using an explicit stack. Know both versions for tree traversals.
 */
public class S3_StackVsRecursion {

    // Simple tree node (self-contained for this file)
    static class TreeNode {
        int val;
        TreeNode left, right;
        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    // =============================================================
    // 3.3  SIDE-BY-SIDE: RECURSIVE vs ITERATIVE INORDER TRAVERSAL
    // =============================================================

    /**
     * RECURSIVE INORDER — clean and intuitive.
     *
     * The call stack manages the "go left, process, go right" flow:
     *
     *   Call stack for tree [2, 1, 3]:
     *     inorder(2) pushes frame
     *       inorder(1) pushes frame
     *         inorder(null) returns
     *         process(1)
     *         inorder(null) returns
     *       frame for 1 pops
     *       process(2)
     *       inorder(3) pushes frame
     *         inorder(null) returns
     *         process(3)
     *         inorder(null) returns
     *       frame for 3 pops
     *     frame for 2 pops
     *
     * Time: O(n), Space: O(h) where h = tree height
     */
    public static List<Integer> inorderRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        inorderHelper(root, result);
        return result;
    }

    private static void inorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        inorderHelper(node.left, result);   // go left
        result.add(node.val);               // process
        inorderHelper(node.right, result);  // go right
    }

    /**
     * ITERATIVE INORDER — explicit stack replaces the call stack.
     *
     * The pattern:
     *   1. Push all left children onto the stack (simulates "go left" calls)
     *   2. Pop → process the node (simulates "process" after left subtree returns)
     *   3. Move to right child (simulates "go right" call)
     *   4. Repeat
     *
     * Stack state for tree:
     *         4
     *        / \
     *       2   5
     *      / \
     *     1   3
     *
     *   Push 4, push 2, push 1 (go left until null)
     *   Pop 1, process 1, right = null → continue
     *   Pop 2, process 2, right = 3, push 3
     *   Pop 3, process 3, right = null → continue
     *   Pop 4, process 4, right = 5, push 5
     *   Pop 5, process 5, right = null → done
     *
     *   Result: [1, 2, 3, 4, 5] ← same as recursive
     *
     * Time: O(n), Space: O(h)
     */
    public static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Deque<TreeNode> stack = new ArrayDeque<>();
        TreeNode curr = root;

        while (curr != null || !stack.isEmpty()) {
            // Push all left children
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            // Process
            curr = stack.pop();
            result.add(curr.val);
            // Go right
            curr = curr.right;
        }
        return result;
    }

    // =============================================================
    // 3.4  SIDE-BY-SIDE: RECURSIVE vs ITERATIVE PREORDER
    // =============================================================

    /** RECURSIVE PREORDER: process → left → right */
    public static List<Integer> preorderRecursive(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        preorderHelper(root, result);
        return result;
    }

    private static void preorderHelper(TreeNode node, List<Integer> result) {
        if (node == null) return;
        result.add(node.val);                // process FIRST
        preorderHelper(node.left, result);
        preorderHelper(node.right, result);
    }

    /**
     * ITERATIVE PREORDER: push right first, then left (so left is processed first).
     *
     * This is the simplest iterative traversal — no "go left" loop needed.
     * Process immediately on pop, push children in reverse order.
     */
    public static List<Integer> preorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;

        Deque<TreeNode> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.add(node.val);           // process immediately

            // Push right FIRST so left is popped (processed) first
            if (node.right != null) stack.push(node.right);
            if (node.left != null) stack.push(node.left);
        }
        return result;
    }

    // =============================================================
    // 3.5  SIDE-BY-SIDE: RECURSIVE vs ITERATIVE DFS (GRAPH)
    // =============================================================

    /** Recursive DFS on an adjacency list graph. */
    public static List<Integer> dfsRecursive(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        dfsHelper(graph, start, visited, result);
        return result;
    }

    private static void dfsHelper(Map<Integer, List<Integer>> graph, int node,
                                   Set<Integer> visited, List<Integer> result) {
        visited.add(node);
        result.add(node);
        for (int neighbor : graph.getOrDefault(node, List.of())) {
            if (!visited.contains(neighbor)) {
                dfsHelper(graph, neighbor, visited, result);
            }
        }
    }

    /**
     * Iterative DFS — explicit stack replaces recursive calls.
     *
     * Note: iterative DFS may visit nodes in a slightly different order
     * than recursive DFS (neighbors are pushed in reverse onto the stack),
     * but both are valid DFS orderings.
     */
    public static List<Integer> dfsIterative(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();

        stack.push(start);

        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (visited.contains(node)) continue;

            visited.add(node);
            result.add(node);

            // Push neighbors (reversed so leftmost is popped first)
            List<Integer> neighbors = graph.getOrDefault(node, List.of());
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                if (!visited.contains(neighbors.get(i))) {
                    stack.push(neighbors.get(i));
                }
            }
        }
        return result;
    }

    // =============================================================
    // 3.6  WHEN RECURSION BLOWS UP: STACK OVERFLOW EXAMPLE
    // =============================================================
    //
    // A linked list of 100,000 nodes processed recursively:
    //   getLength(node) = 1 + getLength(node.next)
    //   → 100,000 recursive calls → StackOverflowError
    //
    // An explicit stack (or better, an iterative loop) handles this fine:
    //   int len = 0;
    //   while (node != null) { len++; node = node.next; }
    //   → O(1) stack space
    //
    // RULE OF THUMB:
    //   - Tree depth up to ~10,000: recursion is usually fine
    //   - Linked list or linear structure: always iterate
    //   - If unsure: mention to the interviewer "I could convert this
    //     to iterative if stack depth is a concern"

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        // Build test tree:
        //         4
        //        / \
        //       2   5
        //      / \
        //     1   3
        TreeNode tree = new TreeNode(4,
            new TreeNode(2, new TreeNode(1), new TreeNode(3)),
            new TreeNode(5));

        System.out.println("=== INORDER TRAVERSAL ===");
        System.out.println("Recursive: " + inorderRecursive(tree));   // [1, 2, 3, 4, 5]
        System.out.println("Iterative: " + inorderIterative(tree));   // [1, 2, 3, 4, 5]

        System.out.println("\n=== PREORDER TRAVERSAL ===");
        System.out.println("Recursive: " + preorderRecursive(tree));  // [4, 2, 1, 3, 5]
        System.out.println("Iterative: " + preorderIterative(tree));  // [4, 2, 1, 3, 5]

        // Build test graph:
        //   0 → [1, 2]
        //   1 → [3]
        //   2 → [3]
        //   3 → []
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put(0, List.of(1, 2));
        graph.put(1, List.of(3));
        graph.put(2, List.of(3));
        graph.put(3, List.of());

        System.out.println("\n=== GRAPH DFS ===");
        System.out.println("Recursive: " + dfsRecursive(graph, 0));   // [0, 1, 3, 2]
        System.out.println("Iterative: " + dfsIterative(graph, 0));   // [0, 1, 3, 2]
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — STACK vs RECURSION                          │
 * │                                                             │
 * │ 1. Default to RECURSIVE in interviews — it's cleaner and    │
 * │    faster to write. Only go iterative if asked, or if       │
 * │    recursion depth is a genuine concern.                    │
 * │                                                             │
 * │ 2. The conversion pattern is always the same:               │
 * │    - What gets pushed onto the call stack?                  │
 * │    → Push that onto your explicit stack instead.            │
 * │    - When does a recursive call return?                     │
 * │    → That's when you pop from your stack.                   │
 * │                                                             │
 * │ 3. Iterative INORDER is the hardest of the three traversals │
 * │    to convert (the "go left" loop + right child handoff).   │
 * │    Practice writing it from memory.                         │
 * │                                                             │
 * │ 4. If asked "what's the advantage of iterative?", say:      │
 * │    "It avoids StackOverflowError on deep inputs and gives   │
 * │    me fine-grained control over traversal order."           │
 * └─────────────────────────────────────────────────────────────┘
 */

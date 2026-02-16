package trees;

import java.util.*;

/**
 * ============================================================
 * SECTION 7: SERIALIZE AND DESERIALIZE A BINARY TREE (LC 297)
 * ============================================================
 *
 * ⭐ CONFIRMED FULLSTORY-RELEVANT PROBLEM ⭐
 *
 * Goal: convert a binary tree into a string representation, and reconstruct
 * the exact same tree from that string.
 *
 * DESIGN CHOICES:
 *
 * 1. TRAVERSAL ORDER: We use PREORDER (Node → Left → Right).
 *    Why? The root is serialized FIRST, which makes it natural to reconstruct
 *    the tree top-down. When deserializing, the first value is the root,
 *    then we recursively build left and right subtrees.
 *
 * 2. NULL REPRESENTATION: We use a sentinel like "null" or "#" to mark null children.
 *    Without null markers, we can't distinguish tree shapes.
 *    Example: trees [1, 2, null] and [1, null, 2] have the same values but different structures.
 *
 * 3. DELIMITER: We use "," to separate values.
 *    The serialized format: "4,2,1,null,null,3,null,null,6,5,null,null,7,null,null"
 *
 * ALTERNATIVES:
 *   - Level-order (BFS) serialization also works. LeetCode's input format uses this.
 *   - Preorder + Inorder together can reconstruct a tree WITHOUT null markers,
 *     but that's less practical for this problem.
 *
 * Time:  O(n) for both serialize and deserialize
 * Space: O(n) for the string / recursion
 */
public class S7_Serialization {

    // =============================================================
    // SERIALIZE: Binary Tree → String
    // =============================================================

    /**
     * Serialize a binary tree to a comma-separated string using preorder traversal.
     * Null nodes are represented as "null".
     */
    public static String serialize(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }

    private static void serializeHelper(TreeNode node, StringBuilder sb) {
        if (node == null) {
            sb.append("null,");
            return;
        }

        // PREORDER: process node first, then children
        sb.append(node.val).append(",");
        serializeHelper(node.left, sb);
        serializeHelper(node.right, sb);
    }

    // =============================================================
    // DESERIALIZE: String → Binary Tree
    // =============================================================

    /**
     * Deserialize a string back into a binary tree.
     *
     * Strategy: split the string into tokens, put them in a queue,
     * and consume tokens one by one in preorder.
     *
     * Why a Queue? Because we process tokens left-to-right in the order
     * they were serialized. A queue naturally gives us FIFO access.
     *
     * Alternative: use an int[] index pointer (index[0]) instead of a queue.
     * Both work; the queue approach is slightly clearer.
     */
    public static TreeNode deserialize(String data) {
        if (data == null || data.isEmpty()) return null;

        Queue<String> tokens = new LinkedList<>(Arrays.asList(data.split(",")));
        return deserializeHelper(tokens);
    }

    private static TreeNode deserializeHelper(Queue<String> tokens) {
        String val = tokens.poll();

        // "null" marker → this position is empty
        if (val == null || val.equals("null")) return null;

        // Create the node (preorder: node first)
        TreeNode node = new TreeNode(Integer.parseInt(val));

        // Recursively build left subtree, then right
        // The queue ensures we consume tokens in the correct order
        node.left = deserializeHelper(tokens);
        node.right = deserializeHelper(tokens);

        return node;
    }

    // =============================================================
    // ALTERNATIVE: Using an index pointer instead of a Queue
    // =============================================================
    //
    // Some interviewers prefer this approach because it avoids
    // the overhead of creating a Queue from the split array.

    public static String serialize2(TreeNode root) {
        StringBuilder sb = new StringBuilder();
        serializeHelper(root, sb);
        return sb.toString();
    }

    public static TreeNode deserialize2(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] tokens = data.split(",");
        int[] index = {0}; // use array to pass by reference
        return deserializeHelper2(tokens, index);
    }

    private static TreeNode deserializeHelper2(String[] tokens, int[] index) {
        if (index[0] >= tokens.length) return null;

        String val = tokens[index[0]];
        index[0]++; // consume this token

        if (val.equals("null")) return null;

        TreeNode node = new TreeNode(Integer.parseInt(val));
        node.left = deserializeHelper2(tokens, index);
        node.right = deserializeHelper2(tokens, index);

        return node;
    }

    // =============================================================
    // ALTERNATIVE: Level-order (BFS) Serialization
    // =============================================================
    //
    // This matches LeetCode's tree input format: [1,2,3,null,null,4,5]
    // Some interviewers specifically ask for this.

    public static String serializeBFS(TreeNode root) {
        if (root == null) return "";

        StringBuilder sb = new StringBuilder();
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode node = queue.poll();
            if (node == null) {
                sb.append("null,");
            } else {
                sb.append(node.val).append(",");
                queue.offer(node.left);   // enqueue even if null
                queue.offer(node.right);  // enqueue even if null
            }
        }
        return sb.toString();
    }

    public static TreeNode deserializeBFS(String data) {
        if (data == null || data.isEmpty()) return null;

        String[] tokens = data.split(",");
        if (tokens[0].equals("null")) return null;

        TreeNode root = new TreeNode(Integer.parseInt(tokens[0]));
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        int i = 1;

        while (!queue.isEmpty() && i < tokens.length) {
            TreeNode parent = queue.poll();

            // Left child
            if (i < tokens.length && !tokens[i].equals("null")) {
                parent.left = new TreeNode(Integer.parseInt(tokens[i]));
                queue.offer(parent.left);
            }
            i++;

            // Right child
            if (i < tokens.length && !tokens[i].equals("null")) {
                parent.right = new TreeNode(Integer.parseInt(tokens[i]));
                queue.offer(parent.right);
            }
            i++;
        }
        return root;
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

        // Preorder serialization
        System.out.println("=== PREORDER SERIALIZATION ===");
        String serialized = serialize(root);
        System.out.println("Serialized:   " + serialized);
        // "4,2,1,null,null,3,null,null,6,5,null,null,7,null,null,"

        TreeNode deserialized = deserialize(serialized);
        String reserialized = serialize(deserialized);
        System.out.println("Reserialized: " + reserialized);
        System.out.println("Match: " + serialized.equals(reserialized)); // true

        // Index-pointer approach
        System.out.println("\n=== INDEX-POINTER APPROACH ===");
        TreeNode deserialized2 = deserialize2(serialized);
        System.out.println("Reserialized: " + serialize(deserialized2));

        // BFS serialization
        System.out.println("\n=== BFS SERIALIZATION ===");
        String bfsSerialized = serializeBFS(root);
        System.out.println("BFS Serialized: " + bfsSerialized);

        TreeNode bfsDeserialized = deserializeBFS(bfsSerialized);
        System.out.println("BFS Reserialized: " + serializeBFS(bfsDeserialized));

        // Edge cases
        System.out.println("\n=== EDGE CASES ===");
        System.out.println("Null tree: " + serialize(null));           // "null,"
        System.out.println("Single node: " + serialize(new TreeNode(42))); // "42,null,null,"

        // Asymmetric tree
        TreeNode asym = new TreeNode(1, new TreeNode(2), null);
        String asymStr = serialize(asym);
        System.out.println("Asymmetric: " + asymStr);
        TreeNode asymBack = deserialize(asymStr);
        System.out.println("Roundtrip:  " + serialize(asymBack));
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — SERIALIZATION / DESERIALIZATION            │
 * │                                                             │
 * │ 1. CLARIFY: "What format should I use?" Some interviewers   │
 * │    want you to design the format. Some give you one. Ask.   │
 * │                                                             │
 * │ 2. The preorder + null markers approach is the most common  │
 * │    and easiest to implement. Default to this unless told     │
 * │    otherwise.                                               │
 * │                                                             │
 * │ 3. Common mistake: forgetting to handle negative numbers.   │
 * │    If you use a single character as delimiter, make sure     │
 * │    "-10" isn't mistakenly split.                            │
 * │                                                             │
 * │ 4. The int[] index trick (passing index by reference via an │
 * │    array) is a pattern you'll use in many recursive parsing │
 * │    problems. Learn it.                                      │
 * │                                                             │
 * │ 5. Fullstory relevance: this tests your ability to design   │
 * │    a data format and correctly implement serialization —    │
 * │    a skill used daily in real engineering (JSON, protobuf,  │
 * │    etc.). Think about it from that angle.                   │
 * │                                                             │
 * │ 6. If asked "how would you test this?": roundtrip test —   │
 * │    serialize, deserialize, re-serialize, compare strings.   │
 * │    Plus edge cases: null, single node, left-only, right-   │
 * │    only, negative values, Integer.MIN_VALUE.                │
 * └─────────────────────────────────────────────────────────────┘
 */

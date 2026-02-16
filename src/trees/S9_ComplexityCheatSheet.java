package trees;

/**
 * ============================================================
 * SECTION 9: COMPLEXITY CHEAT SHEET
 * ============================================================
 *
 * ┌──────────────────────────────────────────┬───────────────┬───────────────┬────────────────────────────────────────────┐
 * │ Operation / Algorithm                    │ Time          │ Space         │ Notes                                      │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 1: FUNDAMENTALS                  │               │               │                                            │
 * │ Tree height (recursive)                  │ O(n)          │ O(h)          │ h = height; O(log n) balanced, O(n) skewed │
 * │ Tree height (iterative BFS)              │ O(n)          │ O(w)          │ w = max width, up to O(n/2)                │
 * │ Depth of a specific node                 │ O(n)          │ O(h)          │ Worst case: must search entire tree        │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 2: DFS TRAVERSALS                │               │               │                                            │
 * │ Inorder (recursive)                      │ O(n)          │ O(h)          │ Sorted output for BST                     │
 * │ Inorder (iterative stack)                │ O(n)          │ O(h)          │ Foundation for kth smallest, BST iterator  │
 * │ Preorder (recursive)                     │ O(n)          │ O(h)          │ Root first — serialization                │
 * │ Preorder (iterative stack)               │ O(n)          │ O(h)          │ Push right then left                      │
 * │ Postorder (recursive)                    │ O(n)          │ O(h)          │ Root last — deletion, expression eval     │
 * │ Postorder (iterative, reverse trick)     │ O(n)          │ O(n)          │ Stores full result before reversing        │
 * │ Postorder (iterative, prev pointer)      │ O(n)          │ O(h)          │ True O(h) space but harder to code         │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 3: BFS TRAVERSALS                │               │               │                                            │
 * │ Level-order traversal                    │ O(n)          │ O(w)          │ w can be O(n) for complete tree            │
 * │ Zigzag level-order                       │ O(n)          │ O(w)          │ Same BFS + direction toggle                │
 * │ Right side view (BFS)                    │ O(n)          │ O(w)          │ Last node per level                       │
 * │ Right side view (DFS)                    │ O(n)          │ O(h)          │ More space-efficient for tall narrow trees │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 4: BST OPERATIONS                │               │               │                                            │
 * │ Search (recursive)                       │ O(h)          │ O(h)          │ O(log n) balanced, O(n) skewed             │
 * │ Search (iterative)                       │ O(h)          │ O(1)          │ Preferred — no stack overhead              │
 * │ Insert (recursive)                       │ O(h)          │ O(h)          │ Returns new root                          │
 * │ Insert (iterative)                       │ O(h)          │ O(1)          │ Tracks parent pointer                     │
 * │ Delete                                   │ O(h)          │ O(h)          │ Three cases; two-child uses successor      │
 * │ Find min / max                           │ O(h)          │ O(1)          │ Follow left/right to the end               │
 * │ Inorder successor                        │ O(h)          │ O(1)          │ Two cases: right subtree vs ancestor       │
 * │ Inorder predecessor                      │ O(h)          │ O(1)          │ Mirror of successor                       │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 5: BST VALIDATION & PROPERTIES   │               │               │                                            │
 * │ Validate BST (bounds)                    │ O(n)          │ O(h)          │ Use long for min/max to handle edge cases  │
 * │ Validate BST (inorder)                   │ O(n)          │ O(h)          │ Check strictly increasing sequence         │
 * │ Kth smallest (iterative inorder)         │ O(h + k)      │ O(h)          │ Stop early after k nodes                  │
 * │ LCA of BST                               │ O(h)          │ O(1)          │ Exploit BST ordering — no full traversal  │
 * │ LCA of Binary Tree                       │ O(n)          │ O(h)          │ Must potentially visit all nodes           │
 * │ Check balanced (optimal)                 │ O(n)          │ O(h)          │ -1 sentinel for early termination          │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 6: COMMON ALGORITHMS             │               │               │                                            │
 * │ Maximum depth                            │ O(n)          │ O(h)          │ 1 + max(left, right)                       │
 * │ Invert binary tree                       │ O(n)          │ O(h)          │ Swap children at every node                │
 * │ Same tree                                │ O(min(n1,n2)) │ O(min(h1,h2)) │ Short-circuits on first difference         │
 * │ Subtree of another tree                  │ O(n * m)      │ O(n)          │ n = main tree, m = subtree                 │
 * │ Diameter of binary tree                  │ O(n)          │ O(h)          │ Track global max during height computation │
 * │ Path sum (root-to-leaf)                  │ O(n)          │ O(h)          │ Subtract as you go; check at leaf          │
 * │ All root-to-leaf paths                   │ O(n)          │ O(n·h)        │ Backtracking; copy path at each leaf       │
 * │ Maximum path sum (any-to-any)            │ O(n)          │ O(h)          │ Return single direction, track fork global │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 7: SERIALIZATION                 │               │               │                                            │
 * │ Serialize (preorder)                     │ O(n)          │ O(n)          │ String output is O(n) characters           │
 * │ Deserialize (preorder)                   │ O(n)          │ O(n)          │ Queue-based or index-pointer               │
 * │ Serialize (BFS)                          │ O(n)          │ O(n)          │ Matches LeetCode input format              │
 * │ Deserialize (BFS)                        │ O(n)          │ O(n)          │ Queue of parent nodes                      │
 * ├──────────────────────────────────────────┼───────────────┼───────────────┼────────────────────────────────────────────┤
 * │ SECTION 8: ADVANCED PATTERNS             │               │               │                                            │
 * │ Sorted array → balanced BST              │ O(n)          │ O(log n)      │ Recursion depth = log n (balanced result)  │
 * │ Construct from preorder + inorder        │ O(n)          │ O(n)          │ HashMap for inorder index = key speedup    │
 * │ Flatten to linked list (recursive)       │ O(n)          │ O(h)          │ Reverse postorder trick                    │
 * │ Flatten to linked list (iterative)       │ O(n)          │ O(1)          │ Morris-like pointer manipulation           │
 * │ Top view / bottom view                   │ O(n)          │ O(n)          │ BFS + column tracking with TreeMap         │
 * │ Count nodes in complete BT               │ O(log²n)      │ O(log n)      │ At least one subtree is perfect            │
 * └──────────────────────────────────────────┴───────────────┴───────────────┴────────────────────────────────────────────┘
 *
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ KEY COMPLEXITY THEMES TO REMEMBER                                   │
 * │                                                                     │
 * │ 1. BALANCED vs UNBALANCED BST:                                      │
 * │    Balanced → h = O(log n) → BST ops are O(log n)                   │
 * │    Unbalanced → h = O(n) → BST ops degrade to O(n)                  │
 * │    ALWAYS mention this distinction in interviews.                   │
 * │                                                                     │
 * │ 2. SPACE for DFS = O(h), SPACE for BFS = O(w):                     │
 * │    DFS space depends on tree HEIGHT (recursion/stack depth).        │
 * │    BFS space depends on tree WIDTH (max nodes at any level).        │
 * │    For a balanced tree: h = O(log n), w = O(n/2) → BFS uses more.  │
 * │    For a skewed tree:   h = O(n), w = O(1) → DFS uses more.        │
 * │                                                                     │
 * │ 3. Most tree algorithms are O(n) time because you visit each node   │
 * │    once. The only exceptions are:                                   │
 * │    - BST operations that don't visit all nodes: O(h)                │
 * │    - Subtree check: O(n*m) because of repeated comparisons          │
 * │    - Count nodes in complete BT: O(log²n) by skipping subtrees      │
 * │                                                                     │
 * │ 4. The "return info up the recursion" pattern (used in diameter,    │
 * │    balanced check, max path sum) always gives O(n) one-pass.        │
 * │    Without it, you'd compute height at every node → O(n²).          │
 * │    This is the most valuable tree optimization to know.             │
 * └─────────────────────────────────────────────────────────────────────┘
 */
public class S9_ComplexityCheatSheet {
    // This file is a reference document — no executable code needed.
    // Print the table above or keep it open during practice.
}

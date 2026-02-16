package corepatterns;

import java.util.*;

/**
 * ============================================================
 * SECTION 9: BFS vs DFS DECISION GUIDE
 * ============================================================
 *
 *
 * -------------------------------------------------------
 * WHEN TO USE BFS
 * -------------------------------------------------------
 *
 * - SHORTEST PATH in unweighted graphs ("minimum number of steps/moves")
 * - LEVEL-ORDER traversal (process nodes level by level)
 * - Multi-source problems (Rotting Oranges, 01-Matrix)
 * - When you need to find the nearest/closest match
 *
 * BFS explores all neighbors at distance d before distance d+1.
 * This guarantees the first time you reach a node is via the shortest path.
 *
 *
 * -------------------------------------------------------
 * WHEN TO USE DFS
 * -------------------------------------------------------
 *
 * - EXPLORE ALL paths / all solutions (permutations, combinations)
 * - Tree traversals (inorder, preorder, postorder)
 * - Connected components
 * - BACKTRACKING problems (N-Queens, Sudoku)
 * - Cycle detection in directed graphs
 * - When you need to go deep before going wide
 *
 * DFS explores one branch fully before trying the next.
 * This is natural for tree recursion and exhaustive search.
 *
 *
 * -------------------------------------------------------
 * DECISION TABLE
 * -------------------------------------------------------
 *
 *   ┌───────────────────────────────────┬──────────────┐
 *   │ Problem pattern                   │ Use           │
 *   ├───────────────────────────────────┼──────────────┤
 *   │ "Shortest path" / "minimum steps" │ BFS           │
 *   │ "Level by level"                  │ BFS           │
 *   │ "Nearest X"                       │ BFS           │
 *   │ "All paths" / "all solutions"     │ DFS           │
 *   │ "Does a path exist?"             │ Either (DFS)  │
 *   │ "Connected components"           │ Either (DFS)  │
 *   │ "Topological sort"               │ Either (DFS)  │
 *   │ "Tree traversal"                 │ DFS           │
 *   │ "Backtracking"                   │ DFS           │
 *   └───────────────────────────────────┴──────────────┘
 */
public class S9_BFSDFSGuide {

    // =============================================================
    // 9.1  BFS TEMPLATE — using Queue
    // =============================================================
    //
    // Queue → add start → while not empty → poll → process → add neighbors
    //
    // GRID BFS — complete reusable pattern:

    /** Direction array for 4-directional grid movement. */
    static final int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    /**
     * BFS on a grid — find shortest path from start to target.
     * Returns the number of steps, or -1 if unreachable.
     *
     * This template works for ANY grid BFS problem. Change:
     *   - The "is target?" condition
     *   - The "can visit?" condition (walls, water, etc.)
     */
    public static int bfsGrid(int[][] grid, int[] start, int[] target) {
        int rows = grid.length, cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        Queue<int[]> queue = new LinkedList<>();

        queue.offer(start);
        visited[start[0]][start[1]] = true;
        int steps = 0;

        while (!queue.isEmpty()) {
            int size = queue.size(); // level snapshot

            for (int i = 0; i < size; i++) {
                int[] cell = queue.poll();

                // Check if we reached the target
                if (cell[0] == target[0] && cell[1] == target[1]) {
                    return steps;
                }

                // Explore 4 neighbors
                for (int[] d : DIRS) {
                    int nr = cell[0] + d[0];
                    int nc = cell[1] + d[1];

                    // Bounds check + not visited + not a wall (0 = path, 1 = wall)
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                            && !visited[nr][nc] && grid[nr][nc] == 0) {
                        visited[nr][nc] = true; // mark WHEN ENQUEUING
                        queue.offer(new int[]{nr, nc});
                    }
                }
            }
            steps++;
        }
        return -1; // target unreachable
    }

    /**
     * BFS on a graph (adjacency list) — level-order from a source.
     */
    public static List<List<Integer>> bfsGraph(Map<Integer, List<Integer>> graph, int start) {
        List<List<Integer>> levels = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                int node = queue.poll();
                level.add(node);

                for (int neighbor : graph.getOrDefault(node, List.of())) {
                    if (visited.add(neighbor)) { // add returns false if already present
                        queue.offer(neighbor);
                    }
                }
            }
            levels.add(level);
        }
        return levels;
    }

    // =============================================================
    // 9.2  DFS TEMPLATE — Recursive and Iterative
    // =============================================================

    /**
     * DFS on a grid — recursive. Counts the size of a connected component.
     * Modifies the grid in-place (marks visited cells).
     */
    public static int dfsGridRecursive(int[][] grid, int row, int col) {
        int rows = grid.length, cols = grid[0].length;

        // Base case: out of bounds or not part of component
        if (row < 0 || row >= rows || col < 0 || col >= cols || grid[row][col] != 1) {
            return 0;
        }

        grid[row][col] = 0; // mark visited
        int size = 1;

        // Recurse in all 4 directions
        for (int[] d : DIRS) {
            size += dfsGridRecursive(grid, row + d[0], col + d[1]);
        }
        return size;
    }

    /**
     * DFS on a graph — iterative with explicit stack.
     * Returns all reachable nodes from start.
     */
    public static List<Integer> dfsGraphIterative(Map<Integer, List<Integer>> graph, int start) {
        List<Integer> result = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();

        stack.push(start);

        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (!visited.add(node)) continue; // already visited

            result.add(node);

            // Push neighbors (reversed for consistent ordering)
            List<Integer> neighbors = graph.getOrDefault(node, List.of());
            for (int i = neighbors.size() - 1; i >= 0; i--) {
                if (!visited.contains(neighbors.get(i))) {
                    stack.push(neighbors.get(i));
                }
            }
        }
        return result;
    }

    /**
     * DFS on a graph — recursive. More natural for tree-like problems.
     */
    public static void dfsGraphRecursive(Map<Integer, List<Integer>> graph, int node,
                                          Set<Integer> visited, List<Integer> result) {
        visited.add(node);
        result.add(node);

        for (int neighbor : graph.getOrDefault(node, List.of())) {
            if (!visited.contains(neighbor)) {
                dfsGraphRecursive(graph, neighbor, visited, result);
            }
        }
    }

    // =============================================================
    // 9.3  PRACTICAL EXAMPLE — Max Area of Island (LC 695)
    // =============================================================
    //
    // Find the area of the largest island (connected component of 1s).
    // Uses DFS to explore each island and count its size.

    public static int maxAreaOfIsland(int[][] grid) {
        int maxArea = 0;
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[0].length; c++) {
                if (grid[r][c] == 1) {
                    maxArea = Math.max(maxArea, dfsGridRecursive(grid, r, c));
                }
            }
        }
        return maxArea;
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== BFS GRID: SHORTEST PATH ===");
        int[][] maze = {
            {0, 0, 0, 0},
            {1, 1, 0, 1},
            {0, 0, 0, 0},
            {0, 1, 1, 0}
        };
        System.out.println("Shortest path (0,0) to (3,3): " +
            bfsGrid(maze, new int[]{0, 0}, new int[]{3, 3})); // 6

        System.out.println("\n=== BFS GRAPH: LEVEL-ORDER ===");
        // Graph: 0-[1,2], 1-[3], 2-[3,4], 3-[], 4-[]
        Map<Integer, List<Integer>> graph = new HashMap<>();
        graph.put(0, List.of(1, 2));
        graph.put(1, List.of(3));
        graph.put(2, List.of(3, 4));
        graph.put(3, List.of());
        graph.put(4, List.of());

        System.out.println("BFS levels from 0: " + bfsGraph(graph, 0));
        // [[0], [1, 2], [3, 4]]

        System.out.println("\n=== DFS GRAPH ===");
        List<Integer> dfsResult = new ArrayList<>();
        dfsGraphRecursive(graph, 0, new HashSet<>(), dfsResult);
        System.out.println("DFS recursive: " + dfsResult);   // [0, 1, 3, 2, 4]
        System.out.println("DFS iterative: " + dfsGraphIterative(graph, 0)); // [0, 1, 3, 2, 4]

        System.out.println("\n=== MAX AREA OF ISLAND ===");
        int[][] islandGrid = {
            {0, 0, 1, 0, 0},
            {0, 1, 1, 1, 0},
            {0, 0, 1, 0, 0},
            {1, 1, 0, 0, 0},
            {1, 1, 0, 0, 0}
        };
        System.out.println("Max island area: " + maxAreaOfIsland(islandGrid)); // 5
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — BFS vs DFS                                   │
 * │                                                             │
 * │ 1. "Shortest path" = BFS. Always. Don't use DFS for        │
 * │    shortest path in an unweighted graph — it doesn't work.  │
 * │                                                             │
 * │ 2. MARK VISITED WHEN ENQUEUING (BFS), not when polling.     │
 * │    This prevents the same cell from being enqueued multiple │
 * │    times. It's the #1 BFS bug.                              │
 * │                                                             │
 * │ 3. GRID TRAVERSAL CHECKLIST:                                 │
 * │    ✓ Direction array: int[][] dirs = {{0,1},{0,-1},...}     │
 * │    ✓ Bounds check: row >= 0 && row < rows && col >= 0 ...  │
 * │    ✓ Visited tracking: boolean[][] or modify grid in-place  │
 * │    ✓ Know this pattern cold — it appears in many problems.  │
 * │                                                             │
 * │ 4. DFS recursion is cleaner for trees and backtracking.     │
 * │    BFS with a queue is cleaner for level-order and shortest │
 * │    path. In an interview, pick the one that matches the     │
 * │    problem and code it confidently.                         │
 * │                                                             │
 * │ 5. Time complexity for grid traversal (BFS or DFS):         │
 * │    O(rows * cols) — each cell visited at most once.         │
 * │    Space: O(rows * cols) for visited array or queue.        │
 * └─────────────────────────────────────────────────────────────┘
 */

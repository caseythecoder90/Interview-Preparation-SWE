package stacksqueues;

import java.util.*;

/**
 * ============================================================
 * SECTION 5: QUEUE PATTERNS IN INTERVIEWS
 * ============================================================
 *
 * Five patterns:
 *   1. BFS tree traversal (level-order)
 *   2. Grid BFS — Number of Islands (LC 200)
 *   3. Multi-source BFS — Rotting Oranges (LC 994)
 *   4. Sliding Window / Rate Limiter (Fullstory)
 *   5. Message Queue / Task Processing (Fullstory)
 */
public class S5_QueuePatterns {

    // Simple tree node for BFS demos
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
    // 5.1  BFS TREE TRAVERSAL — Level-Order (LC 102)
    // =============================================================
    //
    // THE BFS TEMPLATE:
    //   1. Enqueue the starting node
    //   2. While queue is not empty:
    //      a. Capture level size: int size = queue.size()
    //      b. Process all nodes at this level (loop `size` times)
    //      c. For each node: poll, process, enqueue children
    //
    // The SIZE SNAPSHOT is the key trick:
    //   At the start of each level, queue.size() tells you exactly
    //   how many nodes are at the current level. Process only that
    //   many, and any new nodes enqueued are for the NEXT level.
    //
    //   Tree:       3
    //              / \
    //             9   20
    //                /  \
    //               15   7
    //
    //   Level 0: queue = [3],      size = 1 → process 3,  enqueue 9, 20
    //   Level 1: queue = [9, 20],  size = 2 → process 9 and 20, enqueue 15, 7
    //   Level 2: queue = [15, 7],  size = 2 → process 15 and 7
    //
    //   Result: [[3], [9, 20], [15, 7]]

    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size(); // SNAPSHOT: nodes at this level
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);

                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            result.add(level);
        }
        return result;
    }

    // =============================================================
    // 5.2  GRID BFS — Number of Islands (LC 200)
    // =============================================================
    //
    // Given a 2D grid of '1' (land) and '0' (water), count the number
    // of islands. An island is a group of connected '1's (horizontally
    // or vertically).
    //
    // APPROACH: Scan the grid. When you find a '1':
    //   1. Increment island count
    //   2. BFS from that cell, marking all connected land as visited ('0')
    //   3. Continue scanning
    //
    // WHY BFS (not DFS)?
    //   Both work. BFS avoids deep recursion on large grids (e.g., 300x300
    //   grid that is all land = 90,000 recursive calls → possible stack overflow).
    //   In interviews, either approach is fine. BFS is safer for large inputs.
    //
    // Walkthrough on a small grid:
    //
    //   1 1 0 0       Start BFS at (0,0): mark connected '1's as '0'
    //   1 1 0 0       → Island #1 found (all four 1's in top-left)
    //   0 0 1 0
    //   0 0 0 1       Continue scanning... BFS at (2,2): Island #2
    //                  Continue scanning... BFS at (3,3): Island #3
    //   Answer: 3
    //
    // Time: O(rows * cols) — each cell visited once
    // Space: O(min(rows, cols)) — max queue size in BFS

    public static int numIslands(char[][] grid) {
        if (grid == null || grid.length == 0) return 0;

        int rows = grid.length, cols = grid[0].length;
        int count = 0;
        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}}; // right, left, down, up

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == '1') {
                    count++;
                    // BFS to mark all connected land
                    Queue<int[]> queue = new LinkedList<>();
                    queue.offer(new int[]{r, c});
                    grid[r][c] = '0'; // mark visited WHEN ENQUEUING (not when polling!)

                    while (!queue.isEmpty()) {
                        int[] cell = queue.poll();
                        for (int[] d : dirs) {
                            int nr = cell[0] + d[0];
                            int nc = cell[1] + d[1];
                            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                                    && grid[nr][nc] == '1') {
                                grid[nr][nc] = '0'; // mark visited
                                queue.offer(new int[]{nr, nc});
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    // =============================================================
    // 5.3  MULTI-SOURCE BFS — Rotting Oranges (LC 994)
    // =============================================================
    //
    // Grid cells: 0 = empty, 1 = fresh orange, 2 = rotten orange.
    // Each minute, rotten oranges rot adjacent fresh oranges.
    // Return the minimum minutes until no fresh oranges remain, or -1.
    //
    // KEY INSIGHT: MULTI-SOURCE BFS
    //   Start with ALL rotten oranges in the queue simultaneously.
    //   Each "level" of BFS = one minute of rotting.
    //   This is BFS from multiple starting points at once.
    //
    // WHY BFS, NOT DFS?
    //   We need to process all nodes at the same "time step" before moving
    //   to the next. DFS goes deep along one path — it doesn't process
    //   level-by-level. BFS naturally handles "simultaneous expansion."
    //
    // Walkthrough:
    //   2 1 1       Minute 0: rotten = [(0,0)]
    //   1 1 0       Minute 1: (0,0) rots (0,1) and (1,0) → rotten at minute 1
    //   0 1 1       Minute 2: (0,1) rots (0,2), (1,0) rots (1,1)
    //               Minute 3: (1,1) rots (2,1)
    //               Minute 4: (2,1) rots (2,2)
    //               Answer: 4
    //
    // Time: O(rows * cols), Space: O(rows * cols)

    public static int orangesRotting(int[][] grid) {
        int rows = grid.length, cols = grid[0].length;
        Queue<int[]> queue = new LinkedList<>();
        int freshCount = 0;

        // Step 1: enqueue ALL rotten oranges and count fresh ones
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 2) queue.offer(new int[]{r, c});
                else if (grid[r][c] == 1) freshCount++;
            }
        }

        if (freshCount == 0) return 0; // no fresh oranges

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        int minutes = 0;

        // Step 2: BFS level by level (each level = 1 minute)
        while (!queue.isEmpty()) {
            int size = queue.size();
            boolean rottedAny = false;

            for (int i = 0; i < size; i++) {
                int[] cell = queue.poll();
                for (int[] d : dirs) {
                    int nr = cell[0] + d[0];
                    int nc = cell[1] + d[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                            && grid[nr][nc] == 1) {
                        grid[nr][nc] = 2; // rot it
                        freshCount--;
                        queue.offer(new int[]{nr, nc});
                        rottedAny = true;
                    }
                }
            }
            if (rottedAny) minutes++;
        }

        return freshCount == 0 ? minutes : -1; // -1 if unreachable fresh oranges
    }

    // =============================================================
    // 5.4  SLIDING WINDOW / RATE LIMITER — Fullstory Pattern
    // =============================================================
    //
    // PROBLEM: Implement a rate limiter that allows at most `maxRequests`
    // requests per `windowMillis` milliseconds per user.
    //
    // WHY A QUEUE?
    //   Events expire in FIFO order. The oldest event expires first.
    //   A queue naturally maintains chronological order:
    //     - New events are added to the back (offer)
    //     - Expired events are removed from the front (poll)
    //     - The queue always holds only events within the current window
    //
    // This maps directly to the Fullstory Rate Limiter coding problem.

    static class RateLimiter {
        private final int maxRequests;
        private final long windowMillis;
        // Map each user to their queue of request timestamps
        private final Map<String, Queue<Long>> userRequests = new HashMap<>();

        public RateLimiter(int maxRequests, long windowMillis) {
            this.maxRequests = maxRequests;
            this.windowMillis = windowMillis;
        }

        /**
         * Check if a request is allowed for the given user at the given time.
         *
         * Algorithm:
         *   1. Get (or create) the user's request queue
         *   2. Remove all expired timestamps from the front (older than window)
         *   3. If queue size < limit: allow, add timestamp
         *   4. Otherwise: deny
         *
         * Time: O(expired) per call, amortized O(1) since each timestamp
         *       is added once and removed once.
         */
        public boolean isAllowed(String userId, long timestamp) {
            Queue<Long> requests = userRequests.computeIfAbsent(userId, k -> new LinkedList<>());

            // Remove expired timestamps
            while (!requests.isEmpty() && requests.peek() <= timestamp - windowMillis) {
                requests.poll();
            }

            if (requests.size() < maxRequests) {
                requests.offer(timestamp);
                return true;
            }
            return false;
        }
    }

    // =============================================================
    // 5.5  MESSAGE QUEUE / TASK PROCESSING — Fullstory Pattern
    // =============================================================
    //
    // A simple in-memory message queue with FIFO semantics.
    // Messages are enqueued by producers and dequeued by consumers.
    //
    // In a real system (Kafka, RabbitMQ), this is distributed.
    // Here we implement the core abstraction.

    static class MessageQueue<T> {
        private final Queue<T> queue = new LinkedList<>();

        /** Producer: enqueue a message. */
        public void enqueue(T message) {
            queue.offer(message);
        }

        /** Consumer: dequeue the next message. Returns null if empty. */
        public T dequeue() {
            return queue.poll();
        }

        /** Check how many messages are pending. */
        public int pendingCount() {
            return queue.size();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    /**
     * A priority message queue — highest priority messages are processed first.
     * Uses a PriorityQueue instead of a regular Queue.
     */
    static class PriorityMessageQueue<T> {
        private final Queue<PriorityMessage<T>> queue;

        PriorityMessageQueue() {
            // Lower priority number = higher priority (processed first)
            this.queue = new PriorityQueue<>(Comparator.comparingInt(m -> m.priority));
        }

        public void enqueue(T message, int priority) {
            queue.offer(new PriorityMessage<>(message, priority));
        }

        public T dequeue() {
            PriorityMessage<T> msg = queue.poll();
            return msg != null ? msg.message : null;
        }

        public int pendingCount() { return queue.size(); }

        static class PriorityMessage<T> {
            T message;
            int priority;
            PriorityMessage(T message, int priority) {
                this.message = message;
                this.priority = priority;
            }
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) {

        System.out.println("=== LEVEL-ORDER TRAVERSAL ===");
        //       3
        //      / \
        //     9   20
        //        /  \
        //       15   7
        TreeNode tree = new TreeNode(3,
            new TreeNode(9),
            new TreeNode(20, new TreeNode(15), new TreeNode(7)));
        System.out.println(levelOrder(tree)); // [[3], [9, 20], [15, 7]]

        System.out.println("\n=== NUMBER OF ISLANDS ===");
        char[][] grid1 = {
            {'1', '1', '0', '0', '0'},
            {'1', '1', '0', '0', '0'},
            {'0', '0', '1', '0', '0'},
            {'0', '0', '0', '1', '1'}
        };
        System.out.println("Islands: " + numIslands(grid1)); // 3

        System.out.println("\n=== ROTTING ORANGES ===");
        int[][] orangeGrid = {
            {2, 1, 1},
            {1, 1, 0},
            {0, 1, 1}
        };
        System.out.println("Minutes: " + orangesRotting(orangeGrid)); // 4

        System.out.println("\n=== RATE LIMITER ===");
        RateLimiter limiter = new RateLimiter(3, 1000); // 3 requests per second
        System.out.println("t=100:  " + limiter.isAllowed("user1", 100));   // true
        System.out.println("t=200:  " + limiter.isAllowed("user1", 200));   // true
        System.out.println("t=300:  " + limiter.isAllowed("user1", 300));   // true
        System.out.println("t=400:  " + limiter.isAllowed("user1", 400));   // false (3 in window)
        System.out.println("t=1100: " + limiter.isAllowed("user1", 1100));  // true (t=100 expired)
        System.out.println("other:  " + limiter.isAllowed("user2", 400));   // true (different user)

        System.out.println("\n=== MESSAGE QUEUE ===");
        MessageQueue<String> mq = new MessageQueue<>();
        mq.enqueue("order-created");
        mq.enqueue("payment-processed");
        mq.enqueue("email-sent");
        System.out.println("Pending: " + mq.pendingCount()); // 3
        while (!mq.isEmpty()) {
            System.out.println("Processing: " + mq.dequeue());
        }

        System.out.println("\n=== PRIORITY MESSAGE QUEUE ===");
        PriorityMessageQueue<String> pmq = new PriorityMessageQueue<>();
        pmq.enqueue("low-priority-task", 3);
        pmq.enqueue("critical-alert", 1);
        pmq.enqueue("medium-report", 2);
        System.out.println("1st: " + pmq.dequeue()); // critical-alert (priority 1)
        System.out.println("2nd: " + pmq.dequeue()); // medium-report (priority 2)
        System.out.println("3rd: " + pmq.dequeue()); // low-priority-task (priority 3)
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW TIPS — QUEUE PATTERNS                              │
 * │                                                             │
 * │ 1. BFS LEVEL TRICK: the "int size = queue.size()" snapshot  │
 * │    at the start of each level is the SINGLE MOST IMPORTANT  │
 * │    BFS technique. Without it, you can't process levels.     │
 * │                                                             │
 * │ 2. GRID BFS: mark cells as visited WHEN YOU ENQUEUE, not    │
 * │    when you poll. Otherwise, the same cell gets enqueued     │
 * │    multiple times → TLE and wrong results.                  │
 * │                                                             │
 * │ 3. MULTI-SOURCE BFS: "start all sources in the queue at     │
 * │    once" is a pattern that appears in many problems:         │
 * │    - Rotting Oranges                                        │
 * │    - Walls and Gates (LC 286)                               │
 * │    - 01-Matrix (LC 542)                                     │
 * │    The key insight: it's the same as BFS from a single      │
 * │    virtual "super-source" connected to all real sources.    │
 * │                                                             │
 * │ 4. RATE LIMITER: the queue-based sliding window approach    │
 * │    is the simplest correct solution. Mention that a real    │
 * │    production system might use Redis sorted sets or a       │
 * │    token bucket algorithm for better scaling.               │
 * └─────────────────────────────────────────────────────────────┘
 */

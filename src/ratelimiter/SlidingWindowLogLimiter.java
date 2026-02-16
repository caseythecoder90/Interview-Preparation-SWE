package ratelimiter;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * ============================================================
 * SLIDING WINDOW LOG RATE LIMITER
 * ============================================================
 *
 * THE primary interview answer for "Design a Rate Limiter."
 *
 * Approach: store every request timestamp in a per-user queue.
 * On each request, evict timestamps outside the window, then
 * check if the remaining count is under the limit.
 *
 * Data Structure: ConcurrentHashMap<String, Deque<Long>>
 *   - Key:   userId (or IP, API key, etc.)
 *   - Value: queue of timestamps (millis) for requests in the current window
 *
 * Why ConcurrentHashMap + ConcurrentLinkedDeque?
 *   - ConcurrentHashMap: thread-safe map with fine-grained locking (segment-level)
 *   - ConcurrentLinkedDeque: lock-free deque for concurrent reads/writes
 *   - Together: multiple users can be rate-limited concurrently without
 *     a single global lock bottleneck
 *
 * Why Deque (not List)?
 *   - O(1) addLast for recording new timestamps
 *   - O(1) peekFirst/pollFirst for evicting expired timestamps
 *   - Timestamps are naturally ordered (oldest at front, newest at back)
 *
 * Thread Safety:
 *   Per-user synchronization on the deque object ensures that
 *   evict + check + record is atomic for each user, while
 *   different users proceed concurrently without contention.
 */
public class SlidingWindowLogLimiter implements RateLimiter {

    private final int maxRequests;
    private final long windowSizeMillis;

    // userId → queue of timestamps within the current window
    // ConcurrentHashMap for safe concurrent access across users
    private final ConcurrentHashMap<String, Deque<Long>> requestLogs;

    /**
     * Creates a sliding window log rate limiter.
     *
     * @param maxRequests          maximum requests allowed per window per user
     * @param windowSizeInSeconds  the sliding window duration in seconds
     * @throws IllegalArgumentException if maxRequests < 0 or windowSize <= 0
     */
    public SlidingWindowLogLimiter(int maxRequests, int windowSizeInSeconds) {
        if (maxRequests < 0) {
            throw new IllegalArgumentException("maxRequests must be >= 0");
        }
        if (windowSizeInSeconds <= 0) {
            throw new IllegalArgumentException("windowSizeInSeconds must be > 0");
        }
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeInSeconds * 1000L;
        this.requestLogs = new ConcurrentHashMap<>();
    }

    /**
     * Determines whether a request from the given user should be allowed.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Get or create the user's timestamp queue</li>
     *   <li>Synchronize on the queue (per-user lock, not global)</li>
     *   <li>Evict all timestamps older than (now - windowSize)</li>
     *   <li>If queue size < maxRequests → allow, record timestamp</li>
     *   <li>Otherwise → deny</li>
     * </ol>
     *
     * <p>Time Complexity: O(E) where E = number of expired timestamps evicted.
     *    In the worst case (all timestamps expired), E = maxRequests → O(maxRequests).
     *    Amortized over many calls, each timestamp is evicted exactly once → O(1) amortized.
     *
     * <p>Space Complexity: O(maxRequests) per user — at most maxRequests timestamps stored.
     *
     * @param userId unique identifier for the requester
     * @return true if allowed, false if rate-limited
     */
    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        // computeIfAbsent is atomic in ConcurrentHashMap —
        // ensures exactly one deque per userId, no race conditions
        Deque<Long> timestamps = requestLogs.computeIfAbsent(
                userId, k -> new ConcurrentLinkedDeque<>()
        );

        // Synchronize on the per-user deque, NOT on the whole map.
        // This means user A and user B can be processed concurrently,
        // but two threads hitting user A are serialized (correctness).
        synchronized (timestamps) {
            // Evict expired timestamps from the front of the queue.
            // Since timestamps are added in order, the oldest are always at the front.
            // We only need to check the front — once we find a non-expired one, all
            // subsequent ones are also non-expired.
            long windowStart = now - windowSizeMillis;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            // Check if under the limit
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);  // record this request
                return true;              // allowed
            }

            return false;  // rate-limited — don't record the denied request
        }
    }

    @Override
    public String strategyName() {
        return "Sliding Window Log";
    }

    /**
     * Returns the number of active (non-expired) requests for a user.
     * Useful for testing and monitoring.
     *
     * <p>Time Complexity: O(E) for eviction + O(1) for size check
     * <p>Space Complexity: O(1) additional
     *
     * @param userId the user to check
     * @return number of requests in the current window
     */
    public int getCurrentCount(String userId) {
        Deque<Long> timestamps = requestLogs.get(userId);
        if (timestamps == null) return 0;

        synchronized (timestamps) {
            long windowStart = System.currentTimeMillis() - windowSizeMillis;
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }
            return timestamps.size();
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Sliding Window Log Rate Limiter Demo ===\n");

        // Allow 3 requests per 2-second window
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(3, 2);

        String user = "user-1";

        // Send 5 requests rapidly — first 3 should pass, last 2 denied
        System.out.println("--- Burst of 5 requests ---");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }
        System.out.println("  Active count: " + limiter.getCurrentCount(user));

        // Wait for window to expire
        System.out.println("\n--- Waiting 2.1 seconds for window to expire ---");
        Thread.sleep(2100);

        // Should be allowed again
        System.out.println("--- 3 more requests after window expires ---");
        for (int i = 1; i <= 3; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Multi-user isolation
        System.out.println("\n--- Multi-user isolation ---");
        SlidingWindowLogLimiter limiter2 = new SlidingWindowLogLimiter(2, 1);
        // Exhaust user-A's limit
        limiter2.allowRequest("user-A");
        limiter2.allowRequest("user-A");
        System.out.println("  user-A request 3: " +
                (limiter2.allowRequest("user-A") ? "ALLOWED" : "DENIED"));
        // user-B should still be allowed
        System.out.println("  user-B request 1: " +
                (limiter2.allowRequest("user-B") ? "ALLOWED" : "DENIED"));

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   Time:  O(E) per call, where E = expired timestamps       │
 * │          Amortized O(1) — each timestamp evicted once       │
 * │   Space: O(U × R) where U = users, R = maxRequests         │
 * │          Each user stores at most maxRequests timestamps    │
 * │                                                             │
 * │ TRADEOFFS vs OTHER APPROACHES                               │
 * │   ✓ Most accurate — no boundary issues like fixed window   │
 * │   ✓ Exact count within any window of size W                │
 * │   ✗ Memory: stores every timestamp (O(R) per user)         │
 * │   ✗ Not ideal for very high maxRequests (1M req/min)       │
 * │   → For high limits, use Sliding Window Counter instead    │
 * │                                                             │
 * │ COMMON FOLLOW-UP QUESTIONS                                  │
 * │                                                             │
 * │ Q: "How would you make this distributed?"                  │
 * │ A: Use Redis sorted sets (ZADD timestamp, ZREMRANGEBYSCORE │
 * │    to evict, ZCARD to count). Wrap in a Lua script for     │
 * │    atomicity. Or use a centralized rate-limit service       │
 * │    (e.g., Envoy, Kong) that all API gateways call.         │
 * │                                                             │
 * │ Q: "What happens under memory pressure?"                   │
 * │ A: Three mitigations:                                      │
 * │    1. Bounded queue — maxRequests caps per-user memory     │
 * │    2. Idle user cleanup — background thread evicts users   │
 * │       with no activity for N × windowSize                  │
 * │    3. LRU eviction of entire user entries when map exceeds │
 * │       a configured max size                                │
 * │                                                             │
 * │ Q: "Why synchronized block instead of ReentrantLock?"      │
 * │ A: For this use case, synchronized is simpler and the JVM │
 * │    optimizes it well (biased locking, lock coarsening).    │
 * │    ReentrantLock adds value when you need tryLock(),       │
 * │    lockInterruptibly(), or read/write lock separation.     │
 * │                                                             │
 * │ Q: "Why not synchronize on the entire map?"               │
 * │ A: That would serialize ALL users. Per-user sync means    │
 * │    user-A and user-B can be rate-limited concurrently.    │
 * │    Only same-user concurrent requests are serialized.      │
 * └─────────────────────────────────────────────────────────────┘
 */

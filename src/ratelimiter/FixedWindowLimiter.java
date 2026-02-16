package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * FIXED WINDOW RATE LIMITER
 * ============================================================
 *
 * The simplest rate limiting algorithm. Divides time into fixed
 * intervals (windows) and counts requests per window.
 *
 * How it works:
 *   - Divide the timeline into windows: [0s-60s], [60s-120s], ...
 *   - Each user gets a counter per window
 *   - On request: compute the current window key, check counter
 *   - If counter < max → increment and allow
 *   - If counter >= max → deny
 *
 * Data Structure: ConcurrentHashMap<String, WindowCounter>
 *   - Key:   "userId:windowKey" (composite key)
 *   - Value: AtomicInteger counter for that window
 *
 * Pros:
 *   - Extremely simple to implement
 *   - O(1) time and O(1) space per user per window
 *   - Easy to implement in distributed systems (Redis INCR + EXPIRE)
 *
 * Cons:
 *   - BOUNDARY PROBLEM: a burst at the end of window N and start of
 *     window N+1 can allow 2× the rate limit in a short period
 *   - Example: limit = 100/min. User sends 100 requests at 0:59,
 *     then 100 more at 1:00. That's 200 requests in 2 seconds!
 */
public class FixedWindowLimiter implements RateLimiter {

    // Inner record to hold the window ID and the atomic counter.
    // Using a record for concise, immutable (structurally) value semantics.
    private record WindowCounter(long windowId, AtomicInteger count) {
    }

    private final int maxRequests;
    private final long windowSizeMillis;

    // userId → current window counter
    // We store the windowId alongside the count so we can detect window transitions
    private final ConcurrentHashMap<String, WindowCounter> counters;

    /**
     * Creates a fixed window rate limiter.
     *
     * @param maxRequests          maximum requests per window per user
     * @param windowSizeInSeconds  window duration in seconds
     */
    public FixedWindowLimiter(int maxRequests, int windowSizeInSeconds) {
        if (maxRequests < 0) {
            throw new IllegalArgumentException("maxRequests must be >= 0");
        }
        if (windowSizeInSeconds <= 0) {
            throw new IllegalArgumentException("windowSizeInSeconds must be > 0");
        }
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeInSeconds * 1000L;
        this.counters = new ConcurrentHashMap<>();
    }

    /**
     * Determines whether a request should be allowed under the fixed window algorithm.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Compute the current window ID = currentTimeMillis / windowSizeMillis</li>
     *   <li>Get or create the user's WindowCounter</li>
     *   <li>If window ID changed → reset counter (new window)</li>
     *   <li>If count < max → increment and allow</li>
     *   <li>Otherwise → deny</li>
     * </ol>
     *
     * <p>Time Complexity: O(1) — map lookup + atomic increment
     * <p>Space Complexity: O(U) where U = number of active users
     *
     * @param userId unique identifier for the requester
     * @return true if allowed, false if rate-limited
     */
    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        long currentWindowId = now / windowSizeMillis;

        // Get or create a counter for this user
        WindowCounter counter = counters.compute(userId, (key, existing) -> {
            if (existing == null || existing.windowId() != currentWindowId) {
                // New user or new window → start fresh counter at 0
                return new WindowCounter(currentWindowId, new AtomicInteger(0));
            }
            return existing;  // same window — reuse existing counter
        });

        // Atomically increment and check.
        // incrementAndGet returns the NEW value after increment.
        // If it's <= max, the request is allowed.
        int newCount = counter.count().incrementAndGet();
        if (newCount <= maxRequests) {
            return true;
        }

        // Over limit — undo the increment to keep the counter accurate
        // (so getCurrentCount returns the correct value)
        counter.count().decrementAndGet();
        return false;
    }

    @Override
    public String strategyName() {
        return "Fixed Window";
    }

    /**
     * Returns the current request count for a user in the active window.
     *
     * @param userId the user to check
     * @return current count in the active window, 0 if no record
     */
    public int getCurrentCount(String userId) {
        WindowCounter counter = counters.get(userId);
        if (counter == null) return 0;

        long currentWindowId = System.currentTimeMillis() / windowSizeMillis;
        if (counter.windowId() != currentWindowId) return 0;  // stale window

        return counter.count().get();
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Fixed Window Rate Limiter Demo ===\n");

        // 3 requests per 2-second window
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 2);
        String user = "user-1";

        System.out.println("--- Burst of 5 requests ---");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Wait for the window to reset
        System.out.println("\n--- Waiting 2.1 seconds for new window ---");
        Thread.sleep(2100);

        System.out.println("--- 3 more requests in new window ---");
        for (int i = 1; i <= 3; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Demonstrate the boundary problem
        System.out.println("\n--- Boundary Problem Demo ---");
        System.out.println("  (Limit: 5 requests per 1-second window)");
        FixedWindowLimiter boundaryLimiter = new FixedWindowLimiter(5, 1);

        // Wait until we're near the end of a window
        long now = System.currentTimeMillis();
        long nextWindow = ((now / 1000) + 1) * 1000;
        long sleepMs = nextWindow - now - 50;  // 50ms before boundary
        if (sleepMs > 0) Thread.sleep(sleepMs);

        // Send 5 requests right before boundary
        int allowedBefore = 0;
        for (int i = 0; i < 5; i++) {
            if (boundaryLimiter.allowRequest("boundary-user")) allowedBefore++;
        }

        // Wait just past the boundary
        Thread.sleep(100);

        // Send 5 more right after boundary
        int allowedAfter = 0;
        for (int i = 0; i < 5; i++) {
            if (boundaryLimiter.allowRequest("boundary-user")) allowedAfter++;
        }

        System.out.printf("  Allowed before boundary: %d%n", allowedBefore);
        System.out.printf("  Allowed after boundary:  %d%n", allowedAfter);
        System.out.printf("  Total in ~150ms span:    %d  (limit was 5/sec!)%n",
                allowedBefore + allowedAfter);

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   Time:  O(1) per request — map lookup + atomic increment  │
 * │   Space: O(U) where U = number of active users             │
 * │          Only one counter per user (not per request!)       │
 * │                                                             │
 * │ THE BOUNDARY PROBLEM (Critical Interview Point!)           │
 * │                                                             │
 * │   Limit: 100 req/min. Timeline:                            │
 * │                                                             │
 * │   Window 1: [0:00 ——————————— 0:59]|[1:00 —— Window 2     │
 * │                          100 reqs ↗  ↖ 100 reqs            │
 * │                                                             │
 * │   200 requests in ~2 seconds! Fixed window can't see that  │
 * │   these straddle the boundary — each window counts OK.     │
 * │                                                             │
 * │ TRADEOFFS                                                   │
 * │   ✓ Simplest to implement and understand                   │
 * │   ✓ O(1) time and space — optimal for high throughput      │
 * │   ✓ Easy distributed impl (Redis INCR + EXPIRE)            │
 * │   ✗ Boundary problem allows 2× burst                       │
 * │   ✗ Not suitable when strict rate enforcement is needed    │
 * │                                                             │
 * │ DISTRIBUTED IMPLEMENTATION                                  │
 * │   Redis one-liner (per request):                           │
 * │     key = "rate:{userId}:{windowId}"                       │
 * │     count = INCR key                                       │
 * │     if count == 1: EXPIRE key {windowSize}                 │
 * │     if count > max: DENY                                   │
 * │   Lua script wraps INCR+EXPIRE for atomicity.             │
 * │                                                             │
 * │ WHEN TO USE                                                 │
 * │   → Acceptable when slight over-limit at boundaries is OK │
 * │   → High throughput systems where O(1) matters            │
 * │   → Distributed systems where simplicity is paramount     │
 * └─────────────────────────────────────────────────────────────┘
 */

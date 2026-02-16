package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * SLIDING WINDOW COUNTER RATE LIMITER (Hybrid Approach)
 * ============================================================
 *
 * Combines the accuracy of Sliding Window Log with the memory
 * efficiency of Fixed Window. The "best of both worlds" approach.
 *
 * How it works:
 *   - Maintain TWO fixed windows: the previous and the current
 *   - On each request, compute a WEIGHTED sum:
 *       estimatedCount = (prevCount × overlapRatio) + currentCount
 *   - overlapRatio = fraction of the previous window that overlaps
 *     with the current sliding window
 *
 * Visual:
 *   Timeline:
 *     |--- prev window ---|--- curr window ---|
 *                    |====== sliding window ======|
 *                    ^overlap^
 *
 *   overlapRatio = (windowSize - elapsedInCurrentWindow) / windowSize
 *
 *   Example: limit=10/min, 30 seconds into current window
 *     prevCount = 8, currCount = 3
 *     overlapRatio = (60 - 30) / 60 = 0.5
 *     estimated = 8 × 0.5 + 3 = 7 → under limit, allow
 *
 * Why this works:
 *   - Assumes requests in the previous window were evenly distributed
 *   - This is a reasonable approximation for most real workloads
 *   - Cloudflare uses this algorithm in production
 *
 * Data Structure: ConcurrentHashMap<String, UserWindow>
 *   - Only 2 counters per user (prev + current) → O(1) space
 */
public class SlidingWindowCounterLimiter implements RateLimiter {

    /**
     * Holds the two-window state for a single user.
     * Mutable — guarded by synchronized access.
     */
    private static class UserWindow {
        long prevWindowId;
        int prevCount;
        long currWindowId;
        int currCount;

        UserWindow(long windowId) {
            this.currWindowId = windowId;
            this.currCount = 0;
            this.prevWindowId = windowId - 1;
            this.prevCount = 0;
        }
    }

    private final int maxRequests;
    private final long windowSizeMillis;
    private final ConcurrentHashMap<String, UserWindow> windows;

    /**
     * Creates a sliding window counter rate limiter.
     *
     * @param maxRequests          maximum requests per sliding window per user
     * @param windowSizeInSeconds  the window duration in seconds
     */
    public SlidingWindowCounterLimiter(int maxRequests, int windowSizeInSeconds) {
        if (maxRequests < 0) {
            throw new IllegalArgumentException("maxRequests must be >= 0");
        }
        if (windowSizeInSeconds <= 0) {
            throw new IllegalArgumentException("windowSizeInSeconds must be > 0");
        }
        this.maxRequests = maxRequests;
        this.windowSizeMillis = windowSizeInSeconds * 1000L;
        this.windows = new ConcurrentHashMap<>();
    }

    /**
     * Determines whether a request should be allowed under the sliding window counter algorithm.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Compute current window ID and elapsed time in current window</li>
     *   <li>Get or create the user's UserWindow state</li>
     *   <li>Advance windows if needed (shift curr→prev, reset curr)</li>
     *   <li>Compute weighted estimate: prevCount × overlapRatio + currCount</li>
     *   <li>If estimate < maxRequests → increment currCount and allow</li>
     * </ol>
     *
     * <p>Time Complexity: O(1) — constant work per request
     * <p>Space Complexity: O(U) — two counters per user
     *
     * @param userId unique identifier for the requester
     * @return true if allowed, false if rate-limited
     */
    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();
        long currentWindowId = now / windowSizeMillis;

        // Time elapsed within the current window (0 to windowSizeMillis)
        long elapsedInWindow = now % windowSizeMillis;

        // The overlap ratio: how much of the previous window falls within
        // the sliding window. At the start of the current window, overlap
        // is ~100%. At the end, overlap is ~0%.
        double overlapRatio = 1.0 - ((double) elapsedInWindow / windowSizeMillis);

        UserWindow uw = windows.computeIfAbsent(userId, k -> new UserWindow(currentWindowId));

        synchronized (uw) {
            // Advance windows if time has moved forward
            if (currentWindowId != uw.currWindowId) {
                if (currentWindowId == uw.currWindowId + 1) {
                    // Moved to the next window — shift current → previous
                    uw.prevWindowId = uw.currWindowId;
                    uw.prevCount = uw.currCount;
                } else {
                    // Skipped one or more windows — previous data is stale
                    uw.prevWindowId = currentWindowId - 1;
                    uw.prevCount = 0;
                }
                uw.currWindowId = currentWindowId;
                uw.currCount = 0;
            }

            // Compute the weighted estimate
            // Only include previous count if it's from the immediately preceding window
            double prevWeight = (uw.prevWindowId == currentWindowId - 1)
                    ? uw.prevCount * overlapRatio
                    : 0.0;

            double estimatedCount = prevWeight + uw.currCount;

            if (estimatedCount < maxRequests) {
                uw.currCount++;
                return true;
            }

            return false;
        }
    }

    @Override
    public String strategyName() {
        return "Sliding Window Counter";
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Sliding Window Counter Rate Limiter Demo ===\n");

        // 5 requests per 2-second window
        SlidingWindowCounterLimiter limiter = new SlidingWindowCounterLimiter(5, 2);
        String user = "user-1";

        System.out.println("--- Burst of 7 requests ---");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Wait 1 second (half the window)
        System.out.println("\n--- Waiting 1 second (half window) ---");
        Thread.sleep(1000);

        // Some previous requests still counted due to overlap
        System.out.println("--- 5 more requests (prev window partially overlaps) ---");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Wait for full window to expire
        System.out.println("\n--- Waiting 2.1 seconds for full expiry ---");
        Thread.sleep(2100);

        System.out.println("--- 5 requests after full expiry ---");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        System.out.println("\n=== Demo Complete ===");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ BIG-O ANALYSIS                                              │
 * │   Time:  O(1) per request — constant work, no iteration    │
 * │   Space: O(U) — only 2 counters per user (prev + curr)    │
 * │          vs O(U × R) for Sliding Window Log                │
 * │                                                             │
 * │ WHY THIS IS THE "BEST" APPROACH FOR PRODUCTION              │
 * │                                                             │
 * │   Cloudflare's blog post confirms they use this algorithm  │
 * │   for their global rate limiting. The key insight:          │
 * │                                                             │
 * │   "The weighted estimate assumes uniform distribution of   │
 * │    requests in the previous window. For most real-world    │
 * │    traffic, this is accurate within ~1-2% of the true      │
 * │    sliding window count."                                   │
 * │                                                             │
 * │ TRADEOFFS                                                   │
 * │   ✓ O(1) time AND O(1) space per user — best of both      │
 * │   ✓ No boundary problem (unlike Fixed Window)              │
 * │   ✓ No per-request storage (unlike Sliding Window Log)     │
 * │   ✗ Approximate — not exact like Sliding Window Log        │
 * │   ✗ Slightly more complex to implement than Fixed Window   │
 * │                                                             │
 * │ COMPARISON TABLE                                            │
 * │                                                             │
 * │   Strategy          │ Time  │ Space  │ Accuracy │ Complex  │
 * │   ─────────────────-│───────│────────│──────────│──────────│
 * │   Fixed Window      │ O(1)  │ O(U)   │ Low      │ Simple   │
 * │   Sliding Log       │ O(R)* │ O(U×R) │ Exact    │ Medium   │
 * │   Sliding Counter   │ O(1)  │ O(U)   │ ~99%     │ Medium   │
 * │   Token Bucket      │ O(1)  │ O(U)   │ Exact**  │ Medium   │
 * │                                                             │
 * │   * amortized O(1)  ** exact for average rate              │
 * │                                                             │
 * │ COMMON FOLLOW-UP                                            │
 * │                                                             │
 * │ Q: "When would you pick Sliding Counter over Sliding Log?" │
 * │ A: "When the rate limit is high (e.g., 10K req/min).      │
 * │    Sliding Log stores 10K timestamps per user — expensive. │
 * │    Sliding Counter stores just 2 counters regardless of    │
 * │    the limit. For low limits (100/min), either works."     │
 * └─────────────────────────────────────────────────────────────┘
 */

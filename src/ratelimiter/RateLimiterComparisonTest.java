package ratelimiter;

/**
 * ============================================================
 * COMPARISON TEST: All Rate Limiting Strategies Side-by-Side
 * ============================================================
 *
 * This test demonstrates WHERE each strategy behaves differently:
 *   1. Basic rate limiting (all should behave the same)
 *   2. Boundary burst (Fixed Window allows 2× burst, others don't)
 *   3. Burst tolerance (Token Bucket allows initial burst)
 *   4. Gradual recovery (Sliding Log vs Counter accuracy)
 *
 * Run this to build intuition for interview discussions about
 * tradeoffs between strategies.
 */
public class RateLimiterComparisonTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("    PASS: " + testName);
            passed++;
        } else {
            System.out.println("    FAIL: " + testName);
            failed++;
        }
    }

    /**
     * Helper: count how many requests are allowed out of N attempts.
     */
    private static int countAllowed(RateLimiter limiter, String userId, int attempts) {
        int allowed = 0;
        for (int i = 0; i < attempts; i++) {
            if (limiter.allowRequest(userId)) allowed++;
        }
        return allowed;
    }

    // =============================================================
    // TEST 1: All strategies enforce the basic limit
    // =============================================================
    static void testBasicLimitEnforcement() {
        System.out.println("\n=== Test 1: Basic Limit Enforcement (all strategies) ===");
        int limit = 5;

        RateLimiter[] limiters = {
                new SlidingWindowLogLimiter(limit, 60),
                new FixedWindowLimiter(limit, 60),
                new SlidingWindowCounterLimiter(limit, 60),
                new TokenBucketLimiter(limit, limit)  // bucket=5, refill=5/sec
        };

        for (RateLimiter limiter : limiters) {
            String name = limiter.strategyName();
            int allowed = countAllowed(limiter, "user-1", 10);
            check(name + " — allowed exactly " + limit + " of 10 (got " + allowed + ")",
                    allowed == limit);
        }
    }

    // =============================================================
    // TEST 2: Behavior after full window expiry
    // =============================================================
    static void testFullWindowExpiry() throws InterruptedException {
        System.out.println("\n=== Test 2: Recovery After Full Window Expiry ===");
        int limit = 3;
        int windowSec = 1;

        SlidingWindowLogLimiter sliding = new SlidingWindowLogLimiter(limit, windowSec);
        FixedWindowLimiter fixed = new FixedWindowLimiter(limit, windowSec);
        SlidingWindowCounterLimiter counter = new SlidingWindowCounterLimiter(limit, windowSec);

        // Exhaust all limits
        for (int i = 0; i < limit; i++) {
            sliding.allowRequest("user");
            fixed.allowRequest("user");
            counter.allowRequest("user");
        }

        check("Sliding — at limit", !sliding.allowRequest("user"));
        check("Fixed — at limit", !fixed.allowRequest("user"));
        check("Counter — at limit", !counter.allowRequest("user"));

        // Wait for full window expiry
        Thread.sleep(1200);

        check("Sliding — recovered after expiry", sliding.allowRequest("user"));
        check("Fixed — recovered after expiry", fixed.allowRequest("user"));
        check("Counter — recovered after expiry", counter.allowRequest("user"));
    }

    // =============================================================
    // TEST 3: Token Bucket burst behavior
    // =============================================================
    static void testTokenBucketBurst() throws InterruptedException {
        System.out.println("\n=== Test 3: Token Bucket Burst Behavior ===");

        // Bucket: 10 max tokens, refills 2/sec
        // A fresh user can burst 10 requests instantly, then is limited to 2/sec
        TokenBucketLimiter bucket = new TokenBucketLimiter(10, 2.0);

        // For comparison, a sliding window with 2 req/sec equivalent
        SlidingWindowLogLimiter sliding = new SlidingWindowLogLimiter(2, 1);

        int bucketBurst = countAllowed(bucket, "user", 15);
        int slidingBurst = countAllowed(sliding, "user", 15);

        System.out.printf("    Token Bucket: %d of 15 allowed (burst!)%n", bucketBurst);
        System.out.printf("    Sliding Log:  %d of 15 allowed (strict)%n", slidingBurst);

        check("Token Bucket allows initial burst (10)", bucketBurst == 10);
        check("Sliding Log is stricter (only 2)", slidingBurst == 2);

        // After waiting, token bucket refills
        Thread.sleep(1100);
        int afterWait = countAllowed(bucket, "user", 5);
        System.out.printf("    Token Bucket after 1s wait: %d of 5 allowed (~2 refilled)%n", afterWait);
        check("Token Bucket refilled ~2 tokens after 1s", afterWait >= 1 && afterWait <= 3);
    }

    // =============================================================
    // TEST 4: Multi-user isolation across all strategies
    // =============================================================
    static void testMultiUserIsolationAll() {
        System.out.println("\n=== Test 4: Multi-User Isolation (all strategies) ===");
        int limit = 3;

        RateLimiter[] limiters = {
                new SlidingWindowLogLimiter(limit, 60),
                new FixedWindowLimiter(limit, 60),
                new SlidingWindowCounterLimiter(limit, 60),
                new TokenBucketLimiter(limit, limit)
        };

        for (RateLimiter limiter : limiters) {
            String name = limiter.strategyName();

            // Exhaust user-A
            countAllowed(limiter, "user-A", limit);

            // user-B should still have full limit
            int userBAllowed = countAllowed(limiter, "user-B", limit + 2);
            check(name + " — user-B gets full limit despite user-A exhausted (got " +
                    userBAllowed + ")", userBAllowed == limit);
        }
    }

    // =============================================================
    // TEST 5: Sliding window accuracy vs fixed window at boundary
    // =============================================================
    static void testSlidingVsFixedAccuracy() throws InterruptedException {
        System.out.println("\n=== Test 5: Sliding Window Accuracy vs Fixed Window ===");
        System.out.println("  (Demonstrates why sliding window is more accurate)");

        int limit = 5;
        int windowSec = 2;

        SlidingWindowLogLimiter sliding = new SlidingWindowLogLimiter(limit, windowSec);
        FixedWindowLimiter fixed = new FixedWindowLimiter(limit, windowSec);

        // Use all 5 requests
        for (int i = 0; i < limit; i++) {
            sliding.allowRequest("user");
            fixed.allowRequest("user");
        }

        // Wait 1 second (half window)
        Thread.sleep(1000);

        // Sliding window: original requests are still within the 2s window → denied
        boolean slidingAllows = sliding.allowRequest("user");
        System.out.println("    Sliding Log at half-window: " +
                (slidingAllows ? "ALLOWED" : "DENIED"));

        // Wait for full window expiry from start
        Thread.sleep(1100);

        // Now sliding should allow
        boolean slidingAfterExpiry = sliding.allowRequest("user");
        check("Sliding denies at half-window (accurate)", !slidingAllows);
        check("Sliding allows after full window expiry", slidingAfterExpiry);
    }

    // =============================================================
    // TEST 6: Strategy comparison summary table
    // =============================================================
    static void printComparisonTable() {
        System.out.println("\n=== Strategy Comparison Summary ===");
        System.out.println("  ┌──────────────────────┬───────┬────────┬──────────┬───────────┐");
        System.out.println("  │ Strategy             │ Time  │ Space  │ Accuracy │ Burst OK? │");
        System.out.println("  ├──────────────────────┼───────┼────────┼──────────┼───────────┤");
        System.out.println("  │ Fixed Window         │ O(1)  │ O(U)   │ Low      │ 2× at     │");
        System.out.println("  │                      │       │        │          │ boundary  │");
        System.out.println("  ├──────────────────────┼───────┼────────┼──────────┼───────────┤");
        System.out.println("  │ Sliding Window Log   │ O(R)* │ O(U×R) │ Exact    │ No        │");
        System.out.println("  ├──────────────────────┼───────┼────────┼──────────┼───────────┤");
        System.out.println("  │ Sliding Window Count │ O(1)  │ O(U)   │ ~99%     │ No        │");
        System.out.println("  ├──────────────────────┼───────┼────────┼──────────┼───────────┤");
        System.out.println("  │ Token Bucket         │ O(1)  │ O(U)   │ Exact**  │ Yes       │");
        System.out.println("  │                      │       │        │          │ (designed)│");
        System.out.println("  └──────────────────────┴───────┴────────┴──────────┴───────────┘");
        System.out.println("  * amortized O(1)   ** exact for average rate, allows burst");
        System.out.println();
        System.out.println("  WHEN TO USE EACH:");
        System.out.println("    Fixed Window         → Simple systems, acceptable 2× boundary burst");
        System.out.println("    Sliding Window Log   → Interview answer, strict limits, low maxReq");
        System.out.println("    Sliding Window Count → Production (Cloudflare), high maxReq, O(1) space");
        System.out.println("    Token Bucket         → API gateways (AWS, Stripe), bursty traffic OK");
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Rate Limiter Comparison — Test Suite       ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicLimitEnforcement();
        testFullWindowExpiry();
        testTokenBucketBurst();
        testMultiUserIsolationAll();
        testSlidingVsFixedAccuracy();

        printComparisonTable();

        System.out.println("══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("══════════════════════════════════════════════");
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — RATE LIMITER DESIGN DISCUSSION           │
 * │                                                             │
 * │ Q: "Which algorithm would you choose for a real system?"   │
 * │ A: "It depends on the requirements:                        │
 * │    - For API gateway (bursty): Token Bucket (AWS uses it)  │
 * │    - For strict compliance: Sliding Window Log             │
 * │    - For high scale + accuracy: Sliding Window Counter     │
 * │      (Cloudflare uses it)                                  │
 * │    - For simplicity: Fixed Window with Redis INCR"         │
 * │                                                             │
 * │ Q: "How would you handle rate limiting at API gateway?"    │
 * │ A: "Put the rate limiter BEFORE the application layer:     │
 * │    1. API Gateway (Kong, Envoy) — built-in rate limiting   │
 * │    2. Middleware/filter in the app framework                │
 * │    3. Dedicated rate-limit service (called via gRPC)       │
 * │    Key: rate limit as early as possible to save resources" │
 * │                                                             │
 * │ Q: "How to set the right limits?"                          │
 * │ A: "Start with capacity planning:                          │
 * │    1. Measure current p99 request rates per user           │
 * │    2. Set limits at 2-3× the p99 to allow headroom        │
 * │    3. Use tiered limits (free: 100/hr, paid: 10K/hr)      │
 * │    4. Return 429 Too Many Requests with Retry-After header │
 * │    5. Monitor and adjust based on 429 rate"                │
 * │                                                             │
 * │ Q: "What HTTP headers should a rate limiter set?"          │
 * │ A: "Three standard headers:                                │
 * │    X-RateLimit-Limit: 100        (max allowed)             │
 * │    X-RateLimit-Remaining: 42     (remaining in window)     │
 * │    X-RateLimit-Reset: 1672531200 (window reset epoch)      │
 * │    On 429: include Retry-After header (seconds to wait)"   │
 * └─────────────────────────────────────────────────────────────┘
 */

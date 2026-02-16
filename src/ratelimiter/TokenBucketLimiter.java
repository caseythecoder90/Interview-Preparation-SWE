package ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * TOKEN BUCKET RATE LIMITER
 * ============================================================
 *
 * Models rate limiting as a bucket that holds tokens. Each request
 * consumes one token. Tokens are refilled at a steady rate.
 *
 * How it works:
 *   - Bucket starts full with `maxTokens` tokens
 *   - Each request consumes 1 token (if available)
 *   - Tokens are added back at `refillRate` tokens per second
 *   - If no tokens available → request denied
 *
 * Key insight: we DON'T actually run a background refill thread.
 * Instead, we compute tokens LAZILY on each request:
 *   tokensToAdd = (now - lastRefillTime) × refillRate
 *   currentTokens = min(maxTokens, storedTokens + tokensToAdd)
 *
 * This "lazy evaluation" pattern is crucial — it means the algorithm
 * is O(1) with no background threads or timers.
 *
 * What makes Token Bucket special:
 *   - Allows BURSTS up to maxTokens, then enforces steady rate
 *   - Amazon API Gateway and Stripe use this algorithm
 *   - Smooth rate limiting without hard window boundaries
 *
 * Data Structure: ConcurrentHashMap<String, Bucket>
 *   - Each user has one Bucket with: tokens, lastRefillTimestamp
 */
public class TokenBucketLimiter implements RateLimiter {

    /**
     * Holds the token state for a single user.
     * Mutable — guarded by synchronized access.
     */
    private static class Bucket {
        double tokens;          // current token count (double for fractional refills)
        long lastRefillNanos;   // last refill timestamp in nanoseconds

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillNanos = now;
        }
    }

    private final int maxTokens;        // bucket capacity (max burst size)
    private final double refillRate;    // tokens per nanosecond
    private final ConcurrentHashMap<String, Bucket> buckets;

    /**
     * Creates a token bucket rate limiter.
     *
     * @param maxTokens           bucket capacity (also the max burst size)
     * @param refillPerSecond     number of tokens added per second
     * @throws IllegalArgumentException if maxTokens <= 0 or refillPerSecond <= 0
     */
    public TokenBucketLimiter(int maxTokens, double refillPerSecond) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be > 0");
        }
        if (refillPerSecond <= 0) {
            throw new IllegalArgumentException("refillPerSecond must be > 0");
        }
        this.maxTokens = maxTokens;
        // Convert to tokens per nanosecond for precision with System.nanoTime()
        this.refillRate = refillPerSecond / 1_000_000_000.0;
        this.buckets = new ConcurrentHashMap<>();
    }

    /**
     * Convenience constructor: maxTokens = maxRequests per second,
     * refill rate = maxTokens per second (one full refill per second).
     *
     * @param maxTokens bucket capacity and refill-per-second rate
     */
    public TokenBucketLimiter(int maxTokens) {
        this(maxTokens, maxTokens);
    }

    /**
     * Determines whether a request should be allowed under the token bucket algorithm.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Get or create the user's bucket (starts full)</li>
     *   <li>Compute elapsed time since last refill</li>
     *   <li>Add tokens: elapsed × refillRate (capped at maxTokens)</li>
     *   <li>If tokens >= 1 → consume one token, allow</li>
     *   <li>Otherwise → deny</li>
     * </ol>
     *
     * <p>Time Complexity: O(1) — constant arithmetic + map lookup
     * <p>Space Complexity: O(U) — one bucket per user (2 fields each)
     *
     * @param userId unique identifier for the requester
     * @return true if allowed, false if rate-limited
     */
    @Override
    public boolean allowRequest(String userId) {
        long now = System.nanoTime();

        // Bucket starts FULL — allows an initial burst up to maxTokens
        Bucket bucket = buckets.computeIfAbsent(
                userId, k -> new Bucket(maxTokens, now)
        );

        synchronized (bucket) {
            // LAZY REFILL: compute how many tokens to add since last check.
            // This avoids needing a background thread — tokens are computed
            // on-demand when the user actually makes a request.
            long elapsedNanos = now - bucket.lastRefillNanos;
            if (elapsedNanos > 0) {
                double tokensToAdd = elapsedNanos * refillRate;
                bucket.tokens = Math.min(maxTokens, bucket.tokens + tokensToAdd);
                bucket.lastRefillNanos = now;
            }

            // Try to consume one token
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }

            return false;  // no tokens available — rate limited
        }
    }

    @Override
    public String strategyName() {
        return "Token Bucket";
    }

    /**
     * Returns the approximate number of available tokens for a user.
     * Useful for testing and monitoring (e.g., X-RateLimit-Remaining header).
     *
     * @param userId the user to check
     * @return approximate available tokens
     */
    public int getAvailableTokens(String userId) {
        Bucket bucket = buckets.get(userId);
        if (bucket == null) return maxTokens;

        synchronized (bucket) {
            long now = System.nanoTime();
            long elapsedNanos = now - bucket.lastRefillNanos;
            double currentTokens = Math.min(maxTokens, bucket.tokens + elapsedNanos * refillRate);
            return (int) currentTokens;
        }
    }

    // =============================================================
    // DEMO
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Token Bucket Rate Limiter Demo ===\n");

        // Bucket: 5 max tokens, refills 2 tokens/second
        TokenBucketLimiter limiter = new TokenBucketLimiter(5, 2.0);
        String user = "user-1";

        // Burst: use all 5 tokens immediately
        System.out.println("--- Initial burst (5 max tokens) ---");
        for (int i = 1; i <= 7; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Wait 1 second — should refill 2 tokens
        System.out.println("\n--- Waiting 1 second (refill rate: 2/sec) ---");
        Thread.sleep(1000);

        System.out.println("--- Requests after partial refill ---");
        for (int i = 1; i <= 4; i++) {
            boolean allowed = limiter.allowRequest(user);
            System.out.printf("  Request %d: %s%n", i, allowed ? "ALLOWED" : "DENIED");
        }

        // Wait 3 seconds — should refill to max (5)
        System.out.println("\n--- Waiting 3 seconds (refill to max) ---");
        Thread.sleep(3000);

        System.out.println("--- Full burst again ---");
        for (int i = 1; i <= 6; i++) {
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
 * │   Time:  O(1) per request — arithmetic + map lookup        │
 * │   Space: O(U) — one bucket per user (tokens + timestamp)   │
 * │                                                             │
 * │ KEY INSIGHT: BURST vs SUSTAINED RATE                        │
 * │                                                             │
 * │   Token Bucket allows bursts up to maxTokens, then limits  │
 * │   to a sustained rate of refillPerSecond.                   │
 * │                                                             │
 * │   Example: maxTokens=10, refill=2/sec                      │
 * │   - Idle user can burst 10 requests instantly               │
 * │   - After that, limited to 2 requests/sec steady-state     │
 * │   - This is often DESIRABLE for API clients (bursty nature)│
 * │                                                             │
 * │ WHY LAZY REFILL?                                            │
 * │   Instead of a background thread adding tokens every N ms: │
 * │   - No thread overhead — refill computed on-demand          │
 * │   - O(1) per request — just arithmetic                     │
 * │   - Exact same result as a real-time refill                │
 * │   - Works perfectly in distributed systems (no timers)     │
 * │                                                             │
 * │ WHY nanoTime() INSTEAD OF currentTimeMillis()?             │
 * │   - currentTimeMillis can jump (NTP sync, system clock adj)│
 * │   - nanoTime is monotonic — always moves forward           │
 * │   - Better precision for sub-second rate limiting          │
 * │   - Trade-off: nanoTime is relative (can't compare across  │
 * │     JVMs/machines), but we only need elapsed time          │
 * │                                                             │
 * │ TRADEOFFS                                                   │
 * │   ✓ Allows controlled bursts (great for API clients)      │
 * │   ✓ O(1) time and space                                    │
 * │   ✓ Smooth rate limiting — no hard boundaries              │
 * │   ✓ Used by AWS, Stripe, Google Cloud in production        │
 * │   ✗ Can't enforce a strict "N requests per window" limit  │
 * │   ✗ Two params to tune (bucket size + refill rate)         │
 * │                                                             │
 * │ COMMON FOLLOW-UP QUESTIONS                                  │
 * │                                                             │
 * │ Q: "Token Bucket vs Leaky Bucket?"                         │
 * │ A: "Leaky Bucket processes requests at a FIXED rate (like  │
 * │    a queue draining). Token Bucket allows bursts. Leaky    │
 * │    is better for traffic shaping, Token for rate limiting. │
 * │    Implementation: Leaky = FIFO queue with fixed drain.    │
 * │    Token = counter with lazy refill."                      │
 * │                                                             │
 * │ Q: "How does Amazon API Gateway implement this?"           │
 * │ A: "Two settings: rate (tokens/sec) and burst (bucket      │
 * │    size). Default: 10K rate, 5K burst. Uses Token Bucket   │
 * │    per API key, stored in a distributed cache."            │
 * │                                                             │
 * │ Q: "How to handle variable-cost requests?"                 │
 * │ A: "Instead of consuming 1 token per request, consume     │
 * │    a variable number based on request cost (e.g., write=5  │
 * │    tokens, read=1 token). Change the parameter to          │
 * │    allowRequest(userId, cost)."                            │
 * └─────────────────────────────────────────────────────────────┘
 */

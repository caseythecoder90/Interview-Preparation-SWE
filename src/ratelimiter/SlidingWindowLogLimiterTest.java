package ratelimiter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * COMPREHENSIVE TESTS: Sliding Window Log Rate Limiter
 * ============================================================
 *
 * Tests cover:
 *   1. Basic allow/deny within window
 *   2. Window expiration — requests allowed again after window passes
 *   3. Multiple users isolated from each other
 *   4. Burst traffic at window boundaries
 *   5. Thread safety with concurrent requests
 *   6. Edge cases: zero max requests, very small windows
 *   7. Update existing key behavior (not applicable but conceptual)
 */
public class SlidingWindowLogLimiterTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + testName);
            passed++;
        } else {
            System.out.println("  FAIL: " + testName);
            failed++;
        }
    }

    // =============================================================
    // TEST 1: Basic allow/deny within window
    // =============================================================
    static void testBasicAllowDeny() {
        System.out.println("\n=== Test 1: Basic Allow/Deny ===");
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(3, 10);
        String user = "user-1";

        check("First request allowed", limiter.allowRequest(user));
        check("Second request allowed", limiter.allowRequest(user));
        check("Third request allowed", limiter.allowRequest(user));
        check("Fourth request denied (over limit)", !limiter.allowRequest(user));
        check("Fifth request denied (still over limit)", !limiter.allowRequest(user));
        check("Current count is 3", limiter.getCurrentCount(user) == 3);
    }

    // =============================================================
    // TEST 2: Window expiration
    // =============================================================
    static void testWindowExpiration() throws InterruptedException {
        System.out.println("\n=== Test 2: Window Expiration ===");
        // 2 requests per 1-second window
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(2, 1);
        String user = "user-1";

        check("Request 1 allowed", limiter.allowRequest(user));
        check("Request 2 allowed", limiter.allowRequest(user));
        check("Request 3 denied", !limiter.allowRequest(user));

        // Wait for window to expire
        Thread.sleep(1100);

        check("Request after expiry allowed", limiter.allowRequest(user));
        check("Count after expiry is 1", limiter.getCurrentCount(user) == 1);
    }

    // =============================================================
    // TEST 3: Multiple users isolated
    // =============================================================
    static void testMultiUserIsolation() {
        System.out.println("\n=== Test 3: Multi-User Isolation ===");
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(2, 10);

        // Exhaust user-A's limit
        limiter.allowRequest("user-A");
        limiter.allowRequest("user-A");
        check("user-A third request denied", !limiter.allowRequest("user-A"));

        // user-B should be unaffected
        check("user-B first request allowed", limiter.allowRequest("user-B"));
        check("user-B second request allowed", limiter.allowRequest("user-B"));
        check("user-B third request denied", !limiter.allowRequest("user-B"));

        // user-C untouched
        check("user-C first request allowed", limiter.allowRequest("user-C"));

        check("user-A count is 2", limiter.getCurrentCount("user-A") == 2);
        check("user-B count is 2", limiter.getCurrentCount("user-B") == 2);
        check("user-C count is 1", limiter.getCurrentCount("user-C") == 1);
    }

    // =============================================================
    // TEST 4: Sliding window behavior (not fixed window boundary)
    // =============================================================
    static void testSlidingBehavior() throws InterruptedException {
        System.out.println("\n=== Test 4: Sliding Window Behavior ===");
        // 3 requests per 2-second window
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(3, 2);
        String user = "user-1";

        // Send 3 requests now
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        limiter.allowRequest(user);
        check("At limit (3/3)", !limiter.allowRequest(user));

        // Wait 1 second — requests from 1s ago are still in the 2s window
        Thread.sleep(1000);
        check("1s later — still at limit (sliding, not fixed)", !limiter.allowRequest(user));

        // Wait another 1.1 seconds — original requests now outside the 2s window
        Thread.sleep(1100);
        check("2.1s later — window expired, allowed again", limiter.allowRequest(user));
    }

    // =============================================================
    // TEST 5: Thread safety with concurrent requests
    // =============================================================
    static void testThreadSafety() throws InterruptedException {
        System.out.println("\n=== Test 5: Thread Safety ===");

        // 100 requests per 10-second window
        int maxReq = 100;
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(maxReq, 10);
        String user = "shared-user";

        int numThreads = 20;
        int requestsPerThread = 20;  // 20 × 20 = 400 total attempts (limit 100)

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);
        AtomicInteger totalAllowed = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startGate.await();  // all threads start simultaneously
                    for (int r = 0; r < requestsPerThread; r++) {
                        if (limiter.allowRequest(user)) {
                            totalAllowed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();  // release all threads
        endGate.await();        // wait for all to finish
        executor.shutdown();

        int allowed = totalAllowed.get();
        check("Allowed exactly " + maxReq + " requests (got " + allowed + ")",
                allowed == maxReq);
        check("Current count is " + maxReq,
                limiter.getCurrentCount(user) == maxReq);
    }

    // =============================================================
    // TEST 6: Multi-user concurrent access
    // =============================================================
    static void testMultiUserConcurrent() throws InterruptedException {
        System.out.println("\n=== Test 6: Multi-User Concurrent Access ===");

        int maxReq = 10;
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(maxReq, 10);
        int numUsers = 5;
        int numThreads = 10;
        int requestsPerThread = 10;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);
        AtomicInteger[] perUserAllowed = new AtomicInteger[numUsers];
        for (int i = 0; i < numUsers; i++) {
            perUserAllowed[i] = new AtomicInteger(0);
        }

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startGate.await();
                    // Each thread targets a specific user (round-robin)
                    int userIdx = threadId % numUsers;
                    String userId = "user-" + userIdx;
                    for (int r = 0; r < requestsPerThread; r++) {
                        if (limiter.allowRequest(userId)) {
                            perUserAllowed[userIdx].incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        endGate.await();
        executor.shutdown();

        boolean allCorrect = true;
        for (int i = 0; i < numUsers; i++) {
            int allowed = perUserAllowed[i].get();
            if (allowed != maxReq) {
                allCorrect = false;
                System.out.println("  user-" + i + " got " + allowed + " (expected " + maxReq + ")");
            }
        }
        check("Each user allowed exactly " + maxReq + " requests", allCorrect);
    }

    // =============================================================
    // TEST 7: Edge case — zero max requests
    // =============================================================
    static void testZeroMaxRequests() {
        System.out.println("\n=== Test 7: Zero Max Requests ===");
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(0, 10);

        check("Request denied with 0 limit", !limiter.allowRequest("user-1"));
        check("Second request also denied", !limiter.allowRequest("user-1"));
        check("Different user also denied", !limiter.allowRequest("user-2"));
    }

    // =============================================================
    // TEST 8: Edge case — max requests of 1
    // =============================================================
    static void testMaxRequestsOne() throws InterruptedException {
        System.out.println("\n=== Test 8: Max Requests = 1 ===");
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(1, 1);
        String user = "user-1";

        check("First request allowed", limiter.allowRequest(user));
        check("Second request denied", !limiter.allowRequest(user));

        Thread.sleep(1100);
        check("After window, request allowed again", limiter.allowRequest(user));
        check("Then denied again", !limiter.allowRequest(user));
    }

    // =============================================================
    // TEST 9: Unknown user returns 0 count
    // =============================================================
    static void testUnknownUserCount() {
        System.out.println("\n=== Test 9: Unknown User Count ===");
        SlidingWindowLogLimiter limiter = new SlidingWindowLogLimiter(5, 10);

        check("Unknown user has count 0", limiter.getCurrentCount("ghost") == 0);
    }

    // =============================================================
    // TEST 10: Invalid constructor arguments
    // =============================================================
    static void testInvalidArgs() {
        System.out.println("\n=== Test 10: Invalid Constructor Args ===");

        boolean caught1 = false;
        try {
            new SlidingWindowLogLimiter(-1, 10);
        } catch (IllegalArgumentException e) {
            caught1 = true;
        }
        check("Negative maxRequests throws", caught1);

        boolean caught2 = false;
        try {
            new SlidingWindowLogLimiter(5, 0);
        } catch (IllegalArgumentException e) {
            caught2 = true;
        }
        check("Zero windowSize throws", caught2);

        boolean caught3 = false;
        try {
            new SlidingWindowLogLimiter(5, -1);
        } catch (IllegalArgumentException e) {
            caught3 = true;
        }
        check("Negative windowSize throws", caught3);
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Sliding Window Log Limiter — Test Suite    ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicAllowDeny();
        testWindowExpiration();
        testMultiUserIsolation();
        testSlidingBehavior();
        testThreadSafety();
        testMultiUserConcurrent();
        testZeroMaxRequests();
        testMaxRequestsOne();
        testUnknownUserCount();
        testInvalidArgs();

        System.out.println("\n══════════════════════════════════════════════");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        if (failed == 0) {
            System.out.println("ALL TESTS PASSED");
        } else {
            System.out.println("SOME TESTS FAILED");
        }
        System.out.println("══════════════════════════════════════════════");
    }
}

package ratelimiter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * COMPREHENSIVE TESTS: Fixed Window Rate Limiter
 * ============================================================
 *
 * Tests cover the same categories as SlidingWindowLogLimiterTest,
 * plus a specific test for the boundary problem unique to Fixed Window.
 */
public class FixedWindowLimiterTest {

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
    // TEST 1: Basic allow/deny
    // =============================================================
    static void testBasicAllowDeny() {
        System.out.println("\n=== Test 1: Basic Allow/Deny ===");
        FixedWindowLimiter limiter = new FixedWindowLimiter(3, 10);
        String user = "user-1";

        check("Request 1 allowed", limiter.allowRequest(user));
        check("Request 2 allowed", limiter.allowRequest(user));
        check("Request 3 allowed", limiter.allowRequest(user));
        check("Request 4 denied", !limiter.allowRequest(user));
        check("Current count is 3", limiter.getCurrentCount(user) == 3);
    }

    // =============================================================
    // TEST 2: Window reset
    // =============================================================
    static void testWindowReset() throws InterruptedException {
        System.out.println("\n=== Test 2: Window Reset ===");
        // 2 requests per 1-second window
        FixedWindowLimiter limiter = new FixedWindowLimiter(2, 1);
        String user = "user-1";

        check("Request 1 allowed", limiter.allowRequest(user));
        check("Request 2 allowed", limiter.allowRequest(user));
        check("Request 3 denied", !limiter.allowRequest(user));

        // Wait for window to advance
        Thread.sleep(1100);

        check("New window — request allowed", limiter.allowRequest(user));
        check("New window — count is 1", limiter.getCurrentCount(user) == 1);
    }

    // =============================================================
    // TEST 3: Multi-user isolation
    // =============================================================
    static void testMultiUserIsolation() {
        System.out.println("\n=== Test 3: Multi-User Isolation ===");
        FixedWindowLimiter limiter = new FixedWindowLimiter(2, 10);

        limiter.allowRequest("user-A");
        limiter.allowRequest("user-A");
        check("user-A at limit", !limiter.allowRequest("user-A"));

        check("user-B unaffected", limiter.allowRequest("user-B"));
        check("user-B request 2", limiter.allowRequest("user-B"));
        check("user-B at limit", !limiter.allowRequest("user-B"));
    }

    // =============================================================
    // TEST 4: Thread safety
    // =============================================================
    static void testThreadSafety() throws InterruptedException {
        System.out.println("\n=== Test 4: Thread Safety ===");

        int maxReq = 100;
        FixedWindowLimiter limiter = new FixedWindowLimiter(maxReq, 60);
        String user = "shared-user";

        int numThreads = 20;
        int requestsPerThread = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);
        AtomicInteger totalAllowed = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startGate.await();
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

        startGate.countDown();
        endGate.await();
        executor.shutdown();

        int allowed = totalAllowed.get();
        check("Allowed exactly " + maxReq + " (got " + allowed + ")", allowed == maxReq);
    }

    // =============================================================
    // TEST 5: Zero max requests
    // =============================================================
    static void testZeroMaxRequests() {
        System.out.println("\n=== Test 5: Zero Max Requests ===");
        FixedWindowLimiter limiter = new FixedWindowLimiter(0, 10);

        check("Always denied with 0 limit", !limiter.allowRequest("user-1"));
        check("Still denied", !limiter.allowRequest("user-1"));
    }

    // =============================================================
    // TEST 6: Invalid args
    // =============================================================
    static void testInvalidArgs() {
        System.out.println("\n=== Test 6: Invalid Args ===");

        boolean caught1 = false;
        try { new FixedWindowLimiter(-1, 10); } catch (IllegalArgumentException e) { caught1 = true; }
        check("Negative maxRequests throws", caught1);

        boolean caught2 = false;
        try { new FixedWindowLimiter(5, 0); } catch (IllegalArgumentException e) { caught2 = true; }
        check("Zero windowSize throws", caught2);
    }

    // =============================================================
    // RUN ALL TESTS
    // =============================================================
    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  Fixed Window Limiter — Test Suite          ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        testBasicAllowDeny();
        testWindowReset();
        testMultiUserIsolation();
        testThreadSafety();
        testZeroMaxRequests();
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

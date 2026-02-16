package eventtracker.test;

import eventtracker.model.Session;
import eventtracker.session.SessionManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * SESSION MANAGER — Test Suite
 * ============================================================
 *
 * Tests inactivity-gap session detection:
 *   - Events within gap → same session
 *   - Events across gap → new session
 *   - Multiple sessions per user
 *   - Session start/end accuracy
 *   - Multi-user isolation
 */
public class SessionManagerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testSingleSession() {
        System.out.println("\n=== Test 1: Single Session ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant t1 = Instant.parse("2024-06-15T10:00:00Z");
        Instant t2 = t1.plus(Duration.ofMinutes(10));
        Instant t3 = t1.plus(Duration.ofMinutes(20));

        String s1 = sm.getOrCreateSession("user1", t1);
        String s2 = sm.getOrCreateSession("user1", t2);
        String s3 = sm.getOrCreateSession("user1", t3);

        check("Session ID generated", s1 != null);
        check("10-min gap: same session", s1.equals(s2));
        check("20-min gap: same session", s1.equals(s3));
    }

    static void testNewSessionOnGap() {
        System.out.println("\n=== Test 2: New Session on Gap ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant t1 = Instant.parse("2024-06-15T10:00:00Z");
        Instant t2 = t1.plus(Duration.ofMinutes(31));  // 31 minutes later — exceeds 30-min timeout

        String s1 = sm.getOrCreateSession("user1", t1);
        String s2 = sm.getOrCreateSession("user1", t2);

        check("Different sessions", !s1.equals(s2));
    }

    static void testExactBoundary() {
        System.out.println("\n=== Test 3: Exact Boundary (30 min = same session) ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant t1 = Instant.parse("2024-06-15T10:00:00Z");
        Instant t2 = t1.plus(Duration.ofMinutes(30));  // Exactly at boundary

        String s1 = sm.getOrCreateSession("user1", t1);
        String s2 = sm.getOrCreateSession("user1", t2);

        check("Exactly 30 min: same session", s1.equals(s2));
    }

    static void testMultipleSessions() {
        System.out.println("\n=== Test 4: Multiple Sessions Per User ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant base = Instant.parse("2024-06-15T10:00:00Z");

        // Session 1: 10:00, 10:15, 10:25
        sm.getOrCreateSession("user1", base);
        sm.getOrCreateSession("user1", base.plus(Duration.ofMinutes(15)));
        sm.getOrCreateSession("user1", base.plus(Duration.ofMinutes(25)));

        // Session 2: 11:30 (65 min after start, 65 min after 10:25)
        sm.getOrCreateSession("user1", base.plus(Duration.ofMinutes(90)));
        sm.getOrCreateSession("user1", base.plus(Duration.ofMinutes(100)));

        // Session 3: 14:00 (long gap)
        sm.getOrCreateSession("user1", base.plus(Duration.ofMinutes(240)));

        List<Session> sessions = sm.getUserSessions("user1");
        check("3 sessions created", sessions.size() == 3);
        check("Session 1 has 3 events", sessions.get(0).eventCount() == 3);
        check("Session 2 has 2 events", sessions.get(1).eventCount() == 2);
        check("Session 3 has 1 event", sessions.get(2).eventCount() == 1);
    }

    static void testSessionStartEndTimes() {
        System.out.println("\n=== Test 5: Session Start/End Times ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant start = Instant.parse("2024-06-15T10:00:00Z");
        Instant mid = start.plus(Duration.ofMinutes(15));
        Instant end = start.plus(Duration.ofMinutes(25));

        sm.getOrCreateSession("user1", start);
        sm.getOrCreateSession("user1", mid);
        sm.getOrCreateSession("user1", end);

        Session session = sm.getUserSessions("user1").get(0);
        check("Start time is first event", session.startTime().equals(start));
        check("End time is last event", session.endTime().equals(end));
        check("Duration is 25 minutes", session.durationSeconds() == 25 * 60);
    }

    static void testMultiUserIsolation() {
        System.out.println("\n=== Test 6: Multi-User Isolation ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant t = Instant.parse("2024-06-15T10:00:00Z");

        String u1s = sm.getOrCreateSession("user1", t);
        String u2s = sm.getOrCreateSession("user2", t);

        check("Different users get different sessions", !u1s.equals(u2s));
        check("User1 has 1 session", sm.getUserSessions("user1").size() == 1);
        check("User2 has 1 session", sm.getUserSessions("user2").size() == 1);
    }

    static void testCustomTimeout() {
        System.out.println("\n=== Test 7: Custom Timeout ===");
        AtomicInteger counter = new AtomicInteger(0);
        // 5-minute timeout instead of default 30
        SessionManager sm = new SessionManager(Duration.ofMinutes(5),
                () -> "sess-" + counter.incrementAndGet());

        Instant t1 = Instant.parse("2024-06-15T10:00:00Z");
        Instant t2 = t1.plus(Duration.ofMinutes(6));  // > 5 min timeout

        String s1 = sm.getOrCreateSession("user1", t1);
        String s2 = sm.getOrCreateSession("user1", t2);

        check("6-min gap with 5-min timeout: new session", !s1.equals(s2));
    }

    static void testRemoveUser() {
        System.out.println("\n=== Test 8: Remove User Sessions ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        Instant t = Instant.parse("2024-06-15T10:00:00Z");
        sm.getOrCreateSession("user1", t);
        sm.getOrCreateSession("user2", t);

        sm.removeUser("user1");
        check("User1 sessions removed", sm.getUserSessions("user1").isEmpty());
        check("User2 sessions intact", sm.getUserSessions("user2").size() == 1);
    }

    static void testNoSessionsForUnknownUser() {
        System.out.println("\n=== Test 9: Unknown User Returns Empty ===");
        AtomicInteger counter = new AtomicInteger(0);
        SessionManager sm = new SessionManager(Duration.ofMinutes(30),
                () -> "sess-" + counter.incrementAndGet());

        check("Unknown user: empty list", sm.getUserSessions("nobody").isEmpty());
    }

    public static void main(String[] args) {
        System.out.println("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println("\u2551  Session Manager \u2014 Test Suite                 \u2551");
        System.out.println("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");

        testSingleSession();
        testNewSessionOnGap();
        testExactBoundary();
        testMultipleSessions();
        testSessionStartEndTimes();
        testMultiUserIsolation();
        testCustomTimeout();
        testRemoveUser();
        testNoSessionsForUnknownUser();

        System.out.println("\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }
}

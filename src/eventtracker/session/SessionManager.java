package eventtracker.session;

import eventtracker.model.Session;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * ============================================================
 * SESSION MANAGER — Inactivity-gap session detection
 * ============================================================
 *
 * Groups user events into sessions based on inactivity gaps.
 * A new session starts when the gap between consecutive events
 * exceeds the session timeout (default: 30 minutes).
 *
 * This is how real session tracking works:
 *   - Google Analytics: 30-minute inactivity timeout (configurable)
 *   - Amplitude: 30-minute default, 5-minute minimum
 *   - Mixpanel: 30-minute default
 *
 * Example:
 *   Event at 10:00 → session A starts
 *   Event at 10:15 → same session A (15 min gap < 30 min timeout)
 *   Event at 10:25 → same session A (10 min gap < 30 min timeout)
 *   Event at 11:30 → NEW session B (65 min gap > 30 min timeout)
 *
 * Thread Safety:
 *   Uses ConcurrentHashMap for user-level isolation and CopyOnWriteArrayList
 *   for session history. Active session updates are synchronized per-user.
 *
 * Time Complexity:
 *   - getOrCreateSession: O(1) amortized
 *   - getUserSessions: O(S) where S = number of sessions for user
 */
public class SessionManager {

    /** Default session timeout used by most analytics platforms */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);

    private final Duration sessionTimeout;
    private final Supplier<String> idGenerator;

    /**
     * Mutable session record — tracks an active session's evolving state.
     * Not exposed externally; converted to immutable Session record on read.
     */
    private static class ActiveSession {
        final String sessionId;
        final String userId;
        final Instant startTime;
        volatile Instant lastEventTime;
        final AtomicInteger eventCount = new AtomicInteger(1);

        ActiveSession(String sessionId, String userId, Instant startTime) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.startTime = startTime;
            this.lastEventTime = startTime;
        }

        /** Convert to immutable snapshot */
        Session toSession() {
            return new Session(sessionId, userId, startTime, lastEventTime, eventCount.get());
        }
    }

    /** userId → currently active session */
    private final ConcurrentHashMap<String, ActiveSession> activeSessions = new ConcurrentHashMap<>();

    /** userId → complete session history */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<ActiveSession>> allSessions = new ConcurrentHashMap<>();

    public SessionManager(Duration sessionTimeout, Supplier<String> idGenerator) {
        this.sessionTimeout = Objects.requireNonNull(sessionTimeout);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    public SessionManager(Supplier<String> idGenerator) {
        this(DEFAULT_TIMEOUT, idGenerator);
    }

    /**
     * Returns or creates a session for the given user at the given event time.
     *
     * <p>Logic:
     * 1. If user has no active session → create one
     * 2. If gap since last event > timeout → close old session, create new
     * 3. Otherwise → extend current session (update lastEventTime, increment count)
     *
     * <p>Time Complexity: O(1) amortized
     * <p>Space Complexity: O(1) per call (new session object only if gap exceeded)
     *
     * @param userId    the user
     * @param eventTime the timestamp of the event triggering this call
     * @return sessionId for this event
     */
    public String getOrCreateSession(String userId, Instant eventTime) {
        // Synchronized per-user to prevent race between gap-check and session creation
        synchronized (activeSessions) {
            ActiveSession current = activeSessions.get(userId);

            if (current != null) {
                Duration gap = Duration.between(current.lastEventTime, eventTime);
                if (!gap.isNegative() && gap.compareTo(sessionTimeout) <= 0) {
                    // Within timeout — extend current session
                    current.lastEventTime = eventTime;
                    current.eventCount.incrementAndGet();
                    return current.sessionId;
                }
                // Gap exceeded — fall through to create new session
            }

            // Create new session
            ActiveSession newSession = new ActiveSession(idGenerator.get(), userId, eventTime);
            activeSessions.put(userId, newSession);
            allSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(newSession);
            return newSession.sessionId;
        }
    }

    /**
     * Returns all sessions for a user as immutable snapshots.
     *
     * <p>Time Complexity: O(S) where S = number of sessions
     * <p>Space Complexity: O(S) for the snapshot list
     *
     * @param userId the user to get sessions for
     * @return list of sessions, chronologically ordered
     */
    public List<Session> getUserSessions(String userId) {
        CopyOnWriteArrayList<ActiveSession> sessions = allSessions.get(userId);
        if (sessions == null) return List.of();
        return sessions.stream().map(ActiveSession::toSession).toList();
    }

    /**
     * Returns the current active session for a user, if any.
     *
     * @param userId the user
     * @return the active session snapshot, or null if no active session
     */
    public Session getActiveSession(String userId) {
        ActiveSession active = activeSessions.get(userId);
        return active != null ? active.toSession() : null;
    }

    /**
     * Removes all session data for a user (supports GDPR eviction).
     *
     * @param userId the user to remove sessions for
     */
    public void removeUser(String userId) {
        activeSessions.remove(userId);
        allSessions.remove(userId);
    }

    /** @return the configured session timeout */
    public Duration getSessionTimeout() {
        return sessionTimeout;
    }
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES — SESSION MANAGEMENT                        │
 * │                                                             │
 * │ REAL-WORLD SESSION TRACKING:                                │
 * │   Google Analytics uses a 30-minute inactivity window,      │
 * │   resets at midnight, and starts new session on campaign    │
 * │   parameter change. Amplitude and Mixpanel similar but      │
 * │   with configurable timeouts.                               │
 * │                                                             │
 * │ WHY INACTIVITY GAP?                                         │
 * │   Alternative: fixed-duration sessions (every 30 min).      │
 * │   Problem: splits continuous activity into arbitrary chunks. │
 * │   Inactivity gap captures natural "engagement windows."     │
 * │                                                             │
 * │ AT SCALE:                                                    │
 * │   Session detection runs as a stream processor (Flink):     │
 * │   - Keyed by userId                                          │
 * │   - Session window with gap = 30 min                         │
 * │   - Output: session start/end events for downstream          │
 * │   Flink has built-in session windowing:                      │
 * │     .window(EventTimeSessionWindows.withGap(Time.minutes(30)))│
 * │                                                             │
 * │ EDGE CASES:                                                  │
 * │   - Out-of-order events: watermarking handles late arrivals │
 * │   - Clock skew: use server-side timestamps, not client      │
 * │   - Session spanning midnight: some systems force-close     │
 * │     sessions at midnight to simplify daily reporting        │
 * └─────────────────────────────────────────────────────────────┘
 */

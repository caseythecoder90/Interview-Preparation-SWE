package eventtracker.model;

import java.time.Instant;

/**
 * ============================================================
 * SESSION — Represents a user's activity session
 * ============================================================
 *
 * A session groups related events by detecting inactivity gaps.
 * For example, Google Analytics defines a session as ending after
 * 30 minutes of inactivity — same concept here.
 *
 * Why track sessions?
 *   - "How many sessions does a user have per week?" (engagement metric)
 *   - "What's the average session duration?" (stickiness metric)
 *   - "What events occur within a single session?" (funnel analysis)
 *   - Sessions are fundamental to web/app analytics (GA, Amplitude, Mixpanel)
 *
 * Immutable snapshot: once returned from SessionManager, this is a
 * point-in-time view. The active session may continue to accumulate events.
 */
public record Session(
        String sessionId,
        String userId,
        Instant startTime,
        Instant endTime,
        int eventCount
) {
    /**
     * @return session duration in seconds (endTime - startTime)
     */
    public long durationSeconds() {
        return endTime.getEpochSecond() - startTime.getEpochSecond();
    }
}

package eventtracker.retention;

import java.time.Duration;
import java.util.Objects;

/**
 * ============================================================
 * RETENTION POLICY — Per-event-type retention configuration
 * ============================================================
 *
 * Different event types have different business value and compliance needs:
 *   - "purchase" events: keep 2+ years (financial records, tax audits)
 *   - "page_view" events: keep 90 days (analytics, low individual value)
 *   - "login" events: keep 1 year (security audit trail)
 *
 * This is exactly how real systems work:
 *   - Mixpanel: data retention settings per project
 *   - Amplitude: configurable retention by event type
 *   - GDPR: "storage limitation" principle — keep data only as long as necessary
 *
 * Why per-type and not per-user?
 *   Users don't choose retention. The business defines it based on:
 *   legal requirements, storage costs, and analytical value.
 *   GDPR deletion (evictUser) is a separate concern — it overrides all policies.
 */
public record RetentionPolicy(
        String eventType,
        Duration retention
) {
    public RetentionPolicy {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(retention, "retention must not be null");
        if (retention.isNegative()) {
            throw new IllegalArgumentException("Retention duration must not be negative");
        }
    }
}

package eventtracker.test;

import eventtracker.model.Event;
import eventtracker.query.EventQuery;
import eventtracker.retention.EventRetentionManager;
import eventtracker.retention.RetentionPolicy;
import eventtracker.service.IndexedEventTracker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * EVENT RETENTION MANAGER — Test Suite
 * ============================================================
 *
 * Tests event lifecycle management:
 *   - Time-based eviction (evictBefore)
 *   - User-level deletion (GDPR)
 *   - Per-type retention policies
 *   - Secondary index cleanup after eviction
 */
public class EventRetentionManagerTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String testName, boolean condition) {
        if (condition) { System.out.println("  PASS: " + testName); passed++; }
        else           { System.out.println("  FAIL: " + testName); failed++; }
    }

    static void testEvictBefore() {
        System.out.println("\n=== Test 1: Evict Before Cutoff ===");

        Instant old = Instant.parse("2024-01-15T10:00:00Z");
        Instant recent = Instant.parse("2024-06-15T10:00:00Z");
        Instant cutoff = Instant.parse("2024-03-01T00:00:00Z");
        AtomicInteger counter = new AtomicInteger(0);

        // Track old events
        IndexedEventTracker t = new IndexedEventTracker(
                Clock.fixed(old, ZoneOffset.UTC),
                () -> "evt-" + counter.incrementAndGet());
        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());

        // Track recent events (using a second tracker sharing no state — simulate by removing old)
        // For proper testing, we need events at different timestamps in the same tracker
        // Let's use the Event record directly
        // Actually, since all events get the clock's instant, both events are "old"
        // We need a way to have different timestamps...

        // Workaround: track with old clock, then create retention manager with a cutoff
        check("2 events before eviction", t.size() == 2);

        EventRetentionManager rm = new EventRetentionManager(t, Duration.ofDays(365),
                Clock.fixed(recent, ZoneOffset.UTC));

        int evicted = rm.evictBefore(cutoff);
        check("Evicted 2 old events", evicted == 2);
        check("0 events remain", t.size() == 0);
    }

    static void testEvictPreservesRecent() {
        System.out.println("\n=== Test 2: Evict Preserves Recent Events ===");

        Instant now = Instant.parse("2024-06-15T10:00:00Z");
        Instant cutoff = now.minus(Duration.ofDays(90));
        AtomicInteger counter = new AtomicInteger(0);

        // All events are at 'now', so they should survive a 90-day cutoff
        IndexedEventTracker t = new IndexedEventTracker(
                Clock.fixed(now, ZoneOffset.UTC),
                () -> "evt-" + counter.incrementAndGet());

        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());

        EventRetentionManager rm = new EventRetentionManager(t, Duration.ofDays(365),
                Clock.fixed(now, ZoneOffset.UTC));

        int evicted = rm.evictBefore(cutoff);
        check("0 evicted (all recent)", evicted == 0);
        check("2 events remain", t.size() == 2);
    }

    static void testEvictUser() {
        System.out.println("\n=== Test 3: Evict User (GDPR) ===");
        AtomicInteger counter = new AtomicInteger(0);
        IndexedEventTracker t = new IndexedEventTracker(
                Clock.systemUTC(),
                () -> "evt-" + counter.incrementAndGet());

        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());
        t.track("user2", "page_view", Map.of());
        t.track("user3", "login", Map.of());

        EventRetentionManager rm = new EventRetentionManager(t, Duration.ofDays(365),
                Clock.systemUTC());

        int evicted = rm.evictUser("user1");
        check("Evicted 2 user1 events", evicted == 2);
        check("2 events remain", t.size() == 2);
        check("User1 fully gone", t.query(EventQuery.forUser("user1")).isEmpty());
        check("User2 intact", t.query(EventQuery.forUser("user2")).size() == 1);
        check("User3 intact", t.query(EventQuery.forUser("user3")).size() == 1);
    }

    static void testPerTypeRetentionPolicies() {
        System.out.println("\n=== Test 4: Per-Type Retention Policies ===");

        // Two time periods: "old" (180 days ago) and "now"
        Instant now = Instant.parse("2024-06-15T10:00:00Z");
        Instant old = now.minus(Duration.ofDays(180));
        AtomicInteger counter = new AtomicInteger(0);

        // Track old events
        IndexedEventTracker t = new IndexedEventTracker(
                Clock.fixed(old, ZoneOffset.UTC),
                () -> "evt-" + counter.incrementAndGet());

        t.track("user1", "page_view", Map.of());   // Old page_view — should be evicted (90 day policy)
        t.track("user1", "purchase", Map.of());     // Old purchase — should SURVIVE (730 day policy)
        t.track("user1", "login", Map.of());        // Old login — should be evicted (default 365 < 180 days? No...)

        // Actually: old events are 180 days old.
        // page_view retention = 90 days → 180 > 90 → EVICT
        // purchase retention = 730 days → 180 < 730 → KEEP
        // login: default retention = 365 days → 180 < 365 → KEEP

        List<RetentionPolicy> policies = List.of(
                new RetentionPolicy("page_view", Duration.ofDays(90)),
                new RetentionPolicy("purchase", Duration.ofDays(730))
        );

        EventRetentionManager rm = new EventRetentionManager(t, policies,
                Duration.ofDays(365), Clock.fixed(now, ZoneOffset.UTC));

        int evicted = rm.applyPolicies();
        check("Evicted 1 (page_view)", evicted == 1);
        check("2 events remain", t.size() == 2);

        // Verify purchase survived
        check("Purchase survived", t.getLastEventOfType("user1", "purchase").isPresent());
        check("Page_view evicted", t.getLastEventOfType("user1", "page_view").isEmpty());
        check("Login survived (default 365d)", t.getLastEventOfType("user1", "login").isPresent());
    }

    static void testSecondaryIndexesCleanedAfterEviction() {
        System.out.println("\n=== Test 5: Secondary Indexes Cleaned After Eviction ===");
        AtomicInteger counter = new AtomicInteger(0);
        IndexedEventTracker t = new IndexedEventTracker(
                Clock.systemUTC(),
                () -> "evt-" + counter.incrementAndGet());

        t.track("user1", "page_view", Map.of());
        t.track("user1", "purchase", Map.of());

        // Verify type index works before eviction
        check("Type query works before", t.query(
                EventQuery.forUser("user1").ofType("page_view")).size() == 1);

        // Evict page_views
        EventRetentionManager rm = new EventRetentionManager(t, Duration.ofDays(365),
                Clock.systemUTC());
        t.removeEventsIf(e -> "page_view".equals(e.eventType()));

        // Verify secondary index is clean (no dangling references)
        check("Type index cleaned", t.query(
                EventQuery.forUser("user1").ofType("page_view")).isEmpty());
        check("Count by type reflects eviction",
                !t.countByType("user1").containsKey("page_view"));
    }

    static void testStorageEstimate() {
        System.out.println("\n=== Test 6: Storage Estimate ===");
        AtomicInteger counter = new AtomicInteger(0);
        IndexedEventTracker t = new IndexedEventTracker(
                Clock.systemUTC(),
                () -> "evt-" + counter.incrementAndGet());

        EventRetentionManager rm = new EventRetentionManager(t, Duration.ofDays(365),
                Clock.systemUTC());

        long empty = rm.getStorageEstimate();
        check("Empty tracker: 0 bytes", empty == 0);

        for (int i = 0; i < 100; i++) {
            t.track("user1", "event", Map.of("key", "value"));
        }

        long populated = rm.getStorageEstimate();
        check("Populated tracker: > 0 bytes", populated > 0);
        check("Reasonable estimate (> 20KB for 100 events)", populated > 20_000);
    }

    public static void main(String[] args) {
        System.out.println("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        System.out.println("\u2551  Event Retention Manager \u2014 Test Suite         \u2551");
        System.out.println("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");

        testEvictBefore();
        testEvictPreservesRecent();
        testEvictUser();
        testPerTypeRetentionPolicies();
        testSecondaryIndexesCleanedAfterEviction();
        testStorageEstimate();

        System.out.println("\n\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : "SOME TESTS FAILED");
        System.out.println("\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");
    }
}

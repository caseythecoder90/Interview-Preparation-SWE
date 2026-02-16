package ratelimiter;

/**
 * ============================================================
 * RATE LIMITER — Common Interface
 * ============================================================
 *
 * All rate limiting strategies implement this interface.
 * The Strategy pattern allows swapping algorithms at runtime
 * without changing client code.
 *
 * Why an interface (not abstract class)?
 *   - Each strategy has completely different internal state
 *   - No shared implementation to inherit
 *   - Allows a class to implement multiple interfaces if needed
 *   - Cleaner separation of contract from implementation
 *
 * @param <T> The type of the identifier (usually String for userId/IP)
 */
public interface RateLimiter {

    /**
     * Determines whether a request from the given user should be allowed.
     *
     * <p>If allowed, the request is recorded internally (side effect).
     * If denied, no state change occurs — the caller should retry later.
     *
     * @param userId unique identifier for the requester (userId, IP, API key, etc.)
     * @return true if the request is allowed, false if rate-limited
     */
    boolean allowRequest(String userId);

    /**
     * Returns the strategy name for logging and comparison tests.
     *
     * @return human-readable name of the rate limiting algorithm
     */
    String strategyName();
}

/*
 * ┌─────────────────────────────────────────────────────────────┐
 * │ INTERVIEW NOTES                                             │
 * │                                                             │
 * │ WHY USE AN INTERFACE?                                       │
 * │                                                             │
 * │ Strategy Pattern: encapsulate a family of algorithms behind │
 * │ a common interface so they're interchangeable. The client   │
 * │ programs to the interface, not the implementation.          │
 * │                                                             │
 * │ Real-world example: an API gateway might switch from Fixed  │
 * │ Window to Sliding Window without changing any routing code. │
 * │                                                             │
 * │ COMMON FOLLOW-UP: "Why not an abstract class?"             │
 * │ → No shared state or default behavior across strategies.   │
 * │   Each has fundamentally different internal data structures.│
 * │   Interface is the right choice when you only share the    │
 * │   contract, not the implementation.                         │
 * └─────────────────────────────────────────────────────────────┘
 */

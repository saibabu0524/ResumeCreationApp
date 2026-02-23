package com.softsuave.resumecreationapp.core.analytics

/**
 * Interface for tracking analytics events.
 *
 * Feature modules depend ONLY on this interface — never on a Firebase SDK or
 * any other analytics provider directly. This keeps feature code clean and
 * makes swapping the backend a single DI binding change.
 *
 * Implementations:
 *  - [com.softsuave.resumecreationapp.core.analytics.impl.NoOpAnalyticsTracker] — for tests and local dev builds
 *  - [com.softsuave.resumecreationapp.core.analytics.impl.FirebaseAnalyticsTracker] — for production
 */
interface AnalyticsTracker {

    /**
     * Tracks an [AnalyticsEvent]. Implementations must never throw; all errors
     * should be silently swallowed or logged internally.
     */
    fun track(event: AnalyticsEvent)
}

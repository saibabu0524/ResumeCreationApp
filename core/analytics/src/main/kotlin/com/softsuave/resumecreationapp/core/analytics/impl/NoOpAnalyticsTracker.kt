package com.softsuave.resumecreationapp.core.analytics.impl

import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * No-op implementation of [AnalyticsTracker].
 *
 * All events are silently discarded. Use this in:
 *  - Unit tests — inject directly, no mocking needed
 *  - Local development builds — bound via DI in the `dev` product flavor
 *
 * This ensures local builds never pollute production analytics data.
 */
class NoOpAnalyticsTracker @Inject constructor() : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) {
        // Intentionally does nothing
    }
}

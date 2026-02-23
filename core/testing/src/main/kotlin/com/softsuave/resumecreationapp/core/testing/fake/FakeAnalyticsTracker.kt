package com.softsuave.resumecreationapp.core.testing.fake

import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker

/**
 * Fake implementation of [AnalyticsTracker] for testing.
 *
 * Records all tracked events so tests can assert that the correct
 * analytics events were fired with the expected parameters.
 *
 * Usage:
 * ```kotlin
 * val analytics = FakeAnalyticsTracker()
 * // ... exercise code ...
 * assertThat(analytics.events).containsExactly(
 *     AnalyticsEvent.ScreenView(screenName = "home"),
 * )
 * ```
 */
class FakeAnalyticsTracker : AnalyticsTracker {

    private val _events = mutableListOf<AnalyticsEvent>()

    /** All events tracked so far, in order. */
    val events: List<AnalyticsEvent> get() = _events.toList()

    /** Returns only events of type [T]. */
    inline fun <reified T : AnalyticsEvent> eventsOfType(): List<T> =
        events.filterIsInstance<T>()

    /** Clears all recorded events. Useful in @BeforeEach. */
    fun clear() {
        _events.clear()
    }

    override fun track(event: AnalyticsEvent) {
        _events.add(event)
    }
}

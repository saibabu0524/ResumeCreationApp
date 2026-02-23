package com.softsuave.resumecreationapp.core.analytics.impl

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.softsuave.resumecreationapp.core.analytics.AnalyticsEvent
import com.softsuave.resumecreationapp.core.analytics.AnalyticsTracker
import javax.inject.Inject

/**
 * Production [AnalyticsTracker] backed by Firebase Analytics.
 *
 * Maps each [AnalyticsEvent] subclass to a Firebase `logEvent` call with
 * the appropriate parameter bundle. All errors are silently swallowed to
 * prevent analytics from crashing the app.
 *
 * Swap in via DI in the `prod` / `staging` product flavor or via a Hilt module:
 * ```kotlin
 * @Binds
 * abstract fun bindAnalyticsTracker(impl: FirebaseAnalyticsTracker): AnalyticsTracker
 * ```
 */
class FirebaseAnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        runCatching {
            val bundle = event.toBundle()
            firebaseAnalytics.logEvent(event.name, bundle)
        }
        // Errors are intentionally swallowed — analytics must never crash the app
    }

    private fun AnalyticsEvent.toBundle(): Bundle = Bundle().apply {
        when (val e = this@toBundle) {
            is AnalyticsEvent.ScreenView -> {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, e.screenName)
            }
            is AnalyticsEvent.ButtonClick -> {
                putString("button_id", e.buttonId)
                putString(FirebaseAnalytics.Param.SCREEN_NAME, e.screenName)
            }
            is AnalyticsEvent.Login -> {
                putString(FirebaseAnalytics.Param.METHOD, e.method)
            }
            is AnalyticsEvent.SignUp -> {
                putString(FirebaseAnalytics.Param.METHOD, e.method)
            }
            is AnalyticsEvent.Logout -> Unit
            is AnalyticsEvent.Search -> {
                putString(FirebaseAnalytics.Param.SEARCH_TERM, e.query)
                putString(FirebaseAnalytics.Param.SCREEN_NAME, e.screenName)
            }
            is AnalyticsEvent.FeatureExposure -> {
                putString("feature_name", e.featureName)
                putString("variant", e.variant)
            }
            is AnalyticsEvent.Error -> {
                putString("error_type", e.errorType)
                putString(FirebaseAnalytics.Param.SCREEN_NAME, e.screenName)
                e.message?.let { putString("error_message", it) }
            }
        }
    }
}

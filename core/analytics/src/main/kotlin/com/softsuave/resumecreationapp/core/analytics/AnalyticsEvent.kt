package com.softsuave.resumecreationapp.core.analytics

/**
 * Sealed hierarchy of typed analytics events.
 *
 * Each subclass maps exactly to one analytics event name and carries only
 * the parameters that event requires. This ensures type-safety at the
 * call-site and keeps event schemas documented in code.
 *
 * To add a new event:
 *  1. Add a subclass here with the appropriate parameters.
 *  2. Map it in [com.softsuave.resumecreationapp.core.analytics.impl.FirebaseAnalyticsTracker].
 *  3. [com.softsuave.resumecreationapp.core.analytics.impl.NoOpAnalyticsTracker] needs no changes.
 */
sealed class AnalyticsEvent(val name: String) {

    /** Fired when a screen becomes visible to the user. */
    data class ScreenView(
        val screenName: String,
    ) : AnalyticsEvent("screen_view")

    /** Fired when the user taps a button or interactive element. */
    data class ButtonClick(
        val buttonId: String,
        val screenName: String,
    ) : AnalyticsEvent("button_click")

    /** Fired when the user successfully signs in. */
    data class Login(
        val method: String, // "email_password", "google", "biometric", etc.
    ) : AnalyticsEvent("login")

    /** Fired when the user successfully creates a new account. */
    data class SignUp(
        val method: String,
    ) : AnalyticsEvent("sign_up")

    /** Fired when the user explicitly signs out. */
    data object Logout : AnalyticsEvent("logout")

    /** Fired when a search query is submitted. */
    data class Search(
        val query: String,
        val screenName: String,
    ) : AnalyticsEvent("search")

    /** Fired when a feature-flag-gated feature is first encountered by the user. */
    data class FeatureExposure(
        val featureName: String,
        val variant: String,
    ) : AnalyticsEvent("feature_exposure")

    /** Generic error event for tracking non-fatal errors visible to the user. */
    data class Error(
        val errorType: String,
        val screenName: String,
        val message: String? = null,
    ) : AnalyticsEvent("app_error")
}

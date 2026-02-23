package com.softsuave.resumecreationapp.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import timber.log.Timber

/**
 * Wrapper for the Google Play In-App Review API.
 *
 * The trigger policy is configurable — decide **when** to show the review
 * dialog based on your business logic (e.g., after N sessions, after a
 * successful action, etc.).
 *
 * **Important**: Google controls whether the dialog is actually shown.
 * The API may silently do nothing if the user has already reviewed
 * or has been prompted too recently.
 *
 * Usage:
 * ```kotlin
 * val helper = InAppReviewHelper(activity)
 * helper.requestReview()
 * ```
 */
class InAppReviewHelper(private val activity: Activity) {

    private val reviewManager = ReviewManagerFactory.create(activity)

    /**
     * Requests the in-app review flow.
     *
     * This launches the Play-managed review dialog if conditions are met.
     * Google may suppress the dialog silently — do not assume the dialog
     * will always be shown.
     *
     * @param onComplete Called after the flow completes (whether shown or not).
     */
    fun requestReview(onComplete: () -> Unit = {}) {
        reviewManager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                Timber.d("InAppReview: review flow info obtained, launching")
                reviewManager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener {
                        Timber.d("InAppReview: review flow completed")
                        onComplete()
                    }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "InAppReview: failed to request review flow")
                onComplete()
            }
    }
}

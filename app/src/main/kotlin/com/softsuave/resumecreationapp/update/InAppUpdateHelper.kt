package com.softsuave.resumecreationapp.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import timber.log.Timber

/**
 * Wrapper for the Google Play In-App Update API.
 *
 * Supports both update modes:
 * - **Flexible**: Non-blocking update (background download, user installs when ready)
 * - **Immediate**: Full-screen blocking update (critical fixes)
 *
 * Usage:
 * ```kotlin
 * val helper = InAppUpdateHelper(activity)
 * helper.checkForUpdate(
 *     onUpdateAvailable = { info ->
 *         helper.startFlexibleUpdate(info) // or startImmediateUpdate(info)
 *     }
 * )
 * ```
 */
class InAppUpdateHelper(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(activity)

    /**
     * Checks whether an update is available.
     *
     * @param onUpdateAvailable Called with [AppUpdateInfo] when an update is available.
     * @param onNoUpdate Called when no update is available.
     */
    fun checkForUpdate(
        onUpdateAvailable: (AppUpdateInfo) -> Unit = {},
        onNoUpdate: () -> Unit = {},
    ) {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    Timber.d("InAppUpdate: update available — version code ${info.availableVersionCode()}")
                    onUpdateAvailable(info)
                } else {
                    Timber.d("InAppUpdate: no update available")
                    onNoUpdate()
                }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "InAppUpdate: failed to check for updates")
            }
    }

    /**
     * Starts a **flexible** (non-blocking) update.
     * User can continue using the app while the update downloads.
     */
    fun startFlexibleUpdate(
        appUpdateInfo: AppUpdateInfo,
        requestCode: Int = REQUEST_CODE_UPDATE,
    ) {
        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE),
                requestCode,
            )
        }
    }

    /**
     * Starts an **immediate** (blocking) update.
     * Full-screen experience — user must complete the update before continuing.
     */
    fun startImmediateUpdate(
        appUpdateInfo: AppUpdateInfo,
        requestCode: Int = REQUEST_CODE_UPDATE,
    ) {
        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                requestCode,
            )
        }
    }

    /**
     * Completes a flexible update (triggers app restart to install).
     * Call this after the download finishes.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    companion object {
        const val REQUEST_CODE_UPDATE = 1001
    }
}

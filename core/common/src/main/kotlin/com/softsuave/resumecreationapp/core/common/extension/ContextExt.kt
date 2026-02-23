package com.softsuave.resumecreationapp.core.common.extension

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Extension functions for Context operations
 */

/**
 * Shows a toast message
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Shows a toast message from string resource
 */
fun Context.showToast(@StringRes messageRes: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(messageRes), duration).show()
}

/**
 * Opens a URL in the browser
 */
fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}

/**
 * Checks if the device is running at least the specified API level
 */
fun Context.isAtLeastApiLevel(apiLevel: Int): Boolean =
    Build.VERSION.SDK_INT >= apiLevel

/**
 * Gets the app version name
 */
fun Context.getAppVersionName(): String =
    packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"

/**
 * Gets the app version code
 */
fun Context.getAppVersionCode(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageManager.getPackageInfo(packageName, 0).longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
    }

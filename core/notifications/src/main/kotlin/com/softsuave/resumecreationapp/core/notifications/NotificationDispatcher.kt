package com.softsuave.resumecreationapp.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central dispatcher for posting notifications.
 *
 * Handles:
 * - Notification ID management (auto-incrementing)
 * - POST_NOTIFICATIONS permission check (Android 13+)
 * - Centralized logging of posted notifications
 */
@Singleton
class NotificationDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationIdCounter = AtomicInteger(0)

    /**
     * Posts a notification if permission is granted.
     *
     * @param notification The [Notification] to display.
     * @param notificationId Optional explicit ID. Uses auto-incrementing ID if not provided.
     */
    fun show(notification: Notification, notificationId: Int? = null) {
        if (!hasNotificationPermission()) {
            Timber.w("NotificationDispatcher: POST_NOTIFICATIONS permission not granted")
            return
        }

        val id = notificationId ?: notificationIdCounter.getAndIncrement()
        notificationManager.notify(id, notification)
        Timber.d("NotificationDispatcher: posted notification id=$id")
    }

    /**
     * Cancels a specific notification by ID.
     */
    fun cancel(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancels all notifications posted by this app.
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    /**
     * Checks whether the app has notification posting permission.
     * Always returns true on Android < 13 (TIRAMISU).
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

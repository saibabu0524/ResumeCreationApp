package com.softsuave.resumecreationapp.core.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builder for constructing [Notification] objects with consistent styling.
 *
 * Responsibilities:
 * - Constructs notifications with correct channel, priority, and content
 * - Handles [PendingIntent] creation for deep link routing
 * - Uses the same route definitions as in-app navigation for deep links
 *
 * Usage:
 * ```kotlin
 * val notification = notificationBuilder.buildSimple(
 *     title = "Sync Complete",
 *     body = "Your data has been synced.",
 *     channelId = NotificationChannels.CHANNEL_SYNC
 * )
 * ```
 */
@Singleton
class NotificationBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Builds a simple notification with title and body.
     */
    fun buildSimple(
        title: String,
        body: String,
        channelId: String = NotificationChannels.CHANNEL_GENERAL,
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Builds a notification with a deep link intent.
     *
     * @param title Notification title.
     * @param body Notification body.
     * @param deepLinkIntent The [Intent] to launch when the notification is tapped.
     * @param channelId Target notification channel.
     * @param notificationId Unique ID for PendingIntent request code.
     */
    fun buildWithDeepLink(
        title: String,
        body: String,
        deepLinkIntent: Intent,
        channelId: String = NotificationChannels.CHANNEL_GENERAL,
        notificationId: Int = 0,
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Builds an ongoing progress notification (e.g., for sync or upload).
     */
    fun buildProgress(
        title: String,
        progress: Int,
        maxProgress: Int = PROGRESS_MAX,
        channelId: String = NotificationChannels.CHANNEL_SYNC,
    ): Notification {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle(title)
            .setProgress(maxProgress, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val PROGRESS_MAX = 100
    }
}

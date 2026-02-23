package com.softsuave.resumecreationapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all notification channel definitions.
 *
 * Channels are created at app startup via Jetpack App Startup's [Initializer]
 * or by calling [createAll] directly. This keeps startup time minimal and
 * channel definitions centralized.
 *
 * To add a new channel:
 * 1. Add a new [NotificationChannelInfo] to [channels]
 * 2. Call [createAll] (or rely on App Startup)
 */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Creates all notification channels. Safe to call multiple times —
     * existing channels are updated, not recreated.
     */
    fun createAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        channels.forEach { info ->
            val channel = NotificationChannel(
                info.id,
                info.name,
                info.importance,
            ).apply {
                description = info.description
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        /** General purpose notifications channel. */
        const val CHANNEL_GENERAL = "general"

        /** Sync progress / completion notifications. */
        const val CHANNEL_SYNC = "sync"

        /** Chat or direct message notifications. */
        const val CHANNEL_MESSAGES = "messages"

        private val channels = listOf(
            NotificationChannelInfo(
                id = CHANNEL_GENERAL,
                name = "General",
                description = "General app notifications",
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            ),
            NotificationChannelInfo(
                id = CHANNEL_SYNC,
                name = "Sync",
                description = "Background sync notifications",
                importance = NotificationManager.IMPORTANCE_LOW,
            ),
            NotificationChannelInfo(
                id = CHANNEL_MESSAGES,
                name = "Messages",
                description = "Direct message notifications",
                importance = NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }
}

/**
 * Internal data class for channel definitions.
 */
private data class NotificationChannelInfo(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int,
)

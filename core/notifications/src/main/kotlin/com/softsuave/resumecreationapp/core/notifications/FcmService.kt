package com.softsuave.resumecreationapp.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.softsuave.resumecreationapp.core.datastore.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service.
 *
 * No feature module imports Firebase Messaging directly — all FCM handling
 * is encapsulated here in `:core:notifications`.
 *
 * | Event | Handling |
 * |-------|---------|
 * | Token refresh | Stored locally, backend sync scheduled via WorkManager |
 * | Incoming message | Routed by type to [NotificationBuilder] |
 */
@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationBuilder: NotificationBuilder
    @Inject lateinit var notificationDispatcher: NotificationDispatcher

    // Coroutine scope tied to the service lifecycle
    private val serviceScope = CoroutineScope(SupervisorJob())

    /**
     * Called when the FCM registration token is refreshed.
     * Store locally and schedule backend sync.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: ${token.take(TOKEN_PREVIEW_LENGTH)}…")

        serviceScope.launch {
            // Note: Schedule token sync to backend via WorkManager when token registry endpoint is available
            // TokenSyncWorker.enqueue(workManager, token)
        }
    }

    /**
     * Called when an FCM message is received.
     * Routes to the appropriate notification builder based on message type.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received from: ${message.from}")

        // Handle data payload
        if (message.data.isNotEmpty()) {
            handleDataMessage(message.data)
        }

        // Handle notification payload (when app is in foreground)
        message.notification?.let { notification ->
            notificationDispatcher.show(
                notificationBuilder.buildSimple(
                    title = notification.title ?: return,
                    body = notification.body ?: return,
                    channelId = resolveChannel(message.data),
                ),
            )
        }
    }

    /**
     * Routes data-only messages to the correct handler based on type.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data[KEY_MESSAGE_TYPE] ?: return

        when (type) {
            TYPE_SYNC -> {
                Timber.d("FCM: Sync trigger received")
                // Note: Enqueue SyncWorker here when backend sync logic is finalized
            }
            TYPE_MESSAGE -> {
                val title = data[KEY_TITLE] ?: return
                val body = data[KEY_BODY] ?: return
                notificationDispatcher.show(
                    notificationBuilder.buildSimple(
                        title = title,
                        body = body,
                        channelId = NotificationChannels.CHANNEL_MESSAGES,
                    ),
                )
            }
            else -> Timber.w("FCM: Unknown message type: $type")
        }
    }

    /**
     * Resolves the appropriate notification channel based on message data.
     */
    private fun resolveChannel(data: Map<String, String>): String {
        return when (data[KEY_MESSAGE_TYPE]) {
            TYPE_SYNC -> NotificationChannels.CHANNEL_SYNC
            TYPE_MESSAGE -> NotificationChannels.CHANNEL_MESSAGES
            else -> NotificationChannels.CHANNEL_GENERAL
        }
    }

    companion object {
        private const val KEY_MESSAGE_TYPE = "type"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val TYPE_SYNC = "sync"
        private const val TYPE_MESSAGE = "message"
        private const val TOKEN_PREVIEW_LENGTH = 10
    }
}

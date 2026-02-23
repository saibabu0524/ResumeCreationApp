package com.softsuave.resumecreationapp.core.notifications.initializer

import android.content.Context
import androidx.startup.Initializer
import com.softsuave.resumecreationapp.core.notifications.NotificationChannels
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Jetpack App Startup [Initializer] for notification channels.
 *
 * Creates all notification channels at app startup instead of in
 * `Application.onCreate()` — keeps startup time minimal and startup
 * logic declarative.
 *
 * Registered in `AndroidManifest.xml` via the `<provider>` block
 * for `InitializationProvider`.
 */
class NotificationChannelInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationChannelInitializerEntryPoint::class.java,
        )
        entryPoint.notificationChannels().createAll()
    }

    /** No dependencies — channels can be created first. */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface NotificationChannelInitializerEntryPoint {
        fun notificationChannels(): NotificationChannels
    }
}

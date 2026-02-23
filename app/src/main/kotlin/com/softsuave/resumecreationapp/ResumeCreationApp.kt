package com.softsuave.resumecreationapp

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for the Template app.
 *
 * - Annotated with [@HiltAndroidApp] to trigger Hilt code generation.
 * - Implements [Configuration.Provider] for Hilt-managed WorkManager workers.
 * - StrictMode is enabled in debug builds only.
 * - Timber [DebugTree] is planted in debug builds only — **no logging in release**.
 */
@HiltAndroidApp
class ResumeCreationApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(
                if (BuildConfig.DEBUG) android.util.Log.DEBUG else android.util.Log.ERROR,
            )
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase first to ensure it's available for Hilt dependencies
        FirebaseApp.initializeApp(this)

        if (BuildConfig.DEBUG) {
            setupTimber()
            setupStrictMode()
        }
    }

    private fun setupTimber() {
        Timber.plant(Timber.DebugTree())
    }

    private fun setupStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build(),
        )
    }
}

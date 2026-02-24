package com.softsuave.resumecreationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.softsuave.resumecreationapp.core.ui.theme.AppTheme
import com.softsuave.resumecreationapp.navigation.AppNavHost
import com.softsuave.resumecreationapp.core.datastore.TokenStorage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single Activity architecture entry point.
 *
 * Uses edge-to-edge display and the Jetpack SplashScreen API.
 * All navigation happens through [AppNavHost] inside [setContent].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenStorage: TokenStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val startDestination: Any = if (tokenStorage.accessToken != null) {
            com.softsuave.resumecreationapp.feature.resume.navigation.ResumeGraph
        } else {
            com.softsuave.resumecreationapp.feature.auth.navigation.AuthGraphRoute
        }

        setContent {
            AppTheme {
                AppNavHost(startDestination = startDestination)
            }
        }
    }
}

package com.softsuave.resumecreationapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.softsuave.resumecreationapp.core.ui.theme.AppTheme
import com.softsuave.resumecreationapp.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity architecture entry point.
 *
 * Uses edge-to-edge display and the Jetpack SplashScreen API.
 * All navigation happens through [AppNavHost] inside [setContent].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                AppNavHost()
            }
        }
    }
}

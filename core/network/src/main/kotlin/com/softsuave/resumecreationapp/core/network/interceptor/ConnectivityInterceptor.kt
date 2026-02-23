package com.softsuave.resumecreationapp.core.network.interceptor

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.softsuave.resumecreationapp.core.domain.model.AppException
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Throws [AppException.NoInternet] immediately when the device is offline,
 * rather than waiting for the request to time out (which can take 30+ seconds).
 *
 * This improves perceived responsiveness: the UI can show an "offline" state
 * instantly instead of waiting for a timeout.
 */
class ConnectivityInterceptor(
    private val connectivityManager: ConnectivityManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isConnected()) {
            throw NoInternetException()
        }
        return chain.proceed(chain.request())
    }

    private fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Wrapped in [IOException] so OkHttp/Retrofit handle it correctly.
     * [com.softsuave.resumecreationapp.core.network.ExceptionMapper] maps this to [AppException.NoInternet].
     */
    class NoInternetException : IOException("No active internet connection.")
}

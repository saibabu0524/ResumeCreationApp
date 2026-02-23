package com.softsuave.resumecreationapp.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject

/**
 * Observes device connectivity state as a [Flow].
 *
 * Used by:
 *  - UI layer — show/hide an offline banner
 *  - SyncWorker (`core:work`) — delay background sync until connected
 *
 * Emits `true` when a validated internet connection is available, `false` otherwise.
 * The [isOnline] flow is cold — it only registers the callback when collected.
 */
class NetworkMonitor @Inject constructor(
    private val connectivityManager: ConnectivityManager,
) {
    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onLost(network: Network) {
                trySend(isCurrentlyConnected())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(isCurrentlyConnected())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        trySend(isCurrentlyConnected())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.conflate()

    /**
     * Returns the current connectivity state synchronously.
     * Prefer subscribing to [isOnline] in most cases.
     */
    fun isCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

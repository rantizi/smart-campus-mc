package com.unpad.padukampus.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkMonitor {
    val isOnline: Flow<Boolean>

    fun isOnlineNow(): Boolean
}

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {
    private val manager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override fun isOnlineNow(): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override val isOnline: Flow<Boolean> =
        callbackFlow {
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            trySend(isOnlineNow())
                        }

                        override fun onLost(network: Network) {
                            trySend(isOnlineNow())
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            trySend(isOnlineNow())
                        }
                    }
                manager.registerDefaultNetworkCallback(callback)
                trySend(isOnlineNow())
                awaitClose { manager.unregisterNetworkCallback(callback) }
            }
            .distinctUntilChanged()
            .conflate()
}

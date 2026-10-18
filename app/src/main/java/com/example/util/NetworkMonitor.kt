package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        if (connectivityManager == null) return false
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun registerCallback() {
        if (connectivityManager == null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = isCurrentlyOnline(context)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isOnline.value = hasInternet
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            // fallback
            _isOnline.value = checkInitialConnectivity()
        }
    }

    companion object {
        fun isCurrentlyOnline(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        suspend fun measureSpeedKbps(context: Context): Double = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val startRx = android.net.TrafficStats.getTotalRxBytes()
            val startTime = System.currentTimeMillis()

            kotlinx.coroutines.delay(250)

            val endRx = android.net.TrafficStats.getTotalRxBytes()
            val endTime = System.currentTimeMillis()

            val durationSec = (endTime - startTime) / 1000.0
            val rxDiff = if (startRx != android.net.TrafficStats.UNSUPPORTED.toLong() && endRx >= startRx) {
                endRx - startRx
            } else 0L

            var calculatedKbps = if (durationSec > 0) (rxDiff / 1024.0) / durationSec else 0.0

            if (calculatedKbps < 10.0 && isCurrentlyOnline(context)) {
                try {
                    val url = java.net.URL("https://www.google.com/generate_204")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 1500
                    conn.readTimeout = 1500
                    val probeStart = System.currentTimeMillis()
                    conn.connect()
                    val code = conn.responseCode
                    val probeEnd = System.currentTimeMillis()
                    val latencyMs = (probeEnd - probeStart).coerceAtLeast(10L)
                    conn.disconnect()
                    if (code in 200..304) {
                        calculatedKbps = (1500.0 / latencyMs) * 120.0
                    }
                } catch (_: Exception) {}
            }

            calculatedKbps.coerceAtLeast(0.0)
        }

        suspend fun waitForInternet(context: Context, minSpeedKbps: Double = 100.0) {
            while (true) {
                val online = isCurrentlyOnline(context)
                val speed = if (online) measureSpeedKbps(context) else 0.0
                if (online && speed >= minSpeedKbps) break
                kotlinx.coroutines.delay(2000)
            }
        }
    }
}

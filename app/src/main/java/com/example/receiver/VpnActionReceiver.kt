package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.notification.PipelineNotificationManager
import com.example.vpn.VpnGateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VpnActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ROTATE_VPN = "com.example.vpn.ACTION_ROTATE_VPN"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_ROTATE_VPN) {
            CoroutineScope(Dispatchers.Main).launch {
                val nextServer = VpnGateManager.switchToNextServer(context)
                if (nextServer != null) {
                    Toast.makeText(
                        context,
                        "🛡️ Glitch Auto-Shield: Switched to ${nextServer.countryFlag} ${nextServer.countryLong} (${nextServer.ip})",
                        Toast.LENGTH_LONG
                    ).show()

                    PipelineNotificationManager.showProgressNotification(
                        context = context,
                        title = "Glitch OS Shield Rotated",
                        statusMessage = "Connected to ${nextServer.countryFlag} ${nextServer.countryLong} (${nextServer.ip})",
                        progressPercent = 0
                    )
                } else {
                    Toast.makeText(
                        context,
                        "🛡️ Glitch Auto-Shield: Reconnecting to fastest secure gateway...",
                        Toast.LENGTH_SHORT
                    ).show()
                    VpnGateManager.autoConnectFastest(context)
                }
            }
        }
    }
}

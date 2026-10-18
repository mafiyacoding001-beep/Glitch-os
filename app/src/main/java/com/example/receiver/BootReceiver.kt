package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.SettingsManager
import com.example.worker.AutomationSchedulerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Device broadcast received: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settingsManager = SettingsManager(context.applicationContext)
                    val settings = settingsManager.settingsFlow.value
                    if (settings.autoPublishEnabled) {
                        Log.i(TAG, "Re-scheduling background automation on boot (interval: ${settings.scheduleIntervalHours}h, delay: ${settings.scheduleDelaySeconds}s)")
                        AutomationSchedulerManager.scheduleRecurringAutomation(
                            context = context.applicationContext,
                            intervalHours = settings.scheduleIntervalHours,
                            delaySeconds = settings.scheduleDelaySeconds,
                            category = settings.autoCategory,
                            language = settings.defaultLanguage,
                            autoUpload = true
                        )
                    } else {
                        Log.d(TAG, "Auto-publish is disabled; skipping boot schedule.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling automation on boot", e)
                }
            }
        }
    }
}

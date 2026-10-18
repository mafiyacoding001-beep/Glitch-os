package com.example.util

import android.content.Context
import android.os.Build
import android.provider.Settings

object DeviceDataCollector {
    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
        } catch (e: Exception) {
            "unknown_device"
        }
    }

    fun collectAllTelemetry(context: Context): Map<String, Any> {
        val deviceId = getDeviceId(context)
        val hardwareOs = mapOf(
            "model" to Build.MODEL,
            "manufacturer" to Build.MANUFACTURER,
            "brand" to Build.BRAND,
            "device" to Build.DEVICE,
            "product" to Build.PRODUCT,
            "sdk_int" to Build.VERSION.SDK_INT,
            "release" to Build.VERSION.RELEASE
        )
        val memory = mapOf("total_mb" to 4096, "free_mb" to 2048)
        val storage = mapOf("total_gb" to 64, "free_gb" to 32)
        val battery = mapOf("level_pct" to 100, "is_charging" to true)
        val network = mapOf("type" to "WIFI", "is_connected" to true)
        val app = mapOf("version_name" to "1.1", "version_code" to 2)
        val display = mapOf("width_px" to 1080, "height_px" to 1920, "density" to 2.0f)

        return mapOf(
            "device_id" to deviceId,
            "hardware_and_os" to hardwareOs,
            "memory" to memory,
            "storage" to storage,
            "battery" to battery,
            "network" to network,
            "app" to app,
            "display" to display
        )
    }
}

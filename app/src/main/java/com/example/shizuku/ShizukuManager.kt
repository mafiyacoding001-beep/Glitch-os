package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuManager {
    private const val TAG = "ShizukuManager"
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(SHIZUKU_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(SHIZUKU_PACKAGE, 0)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            // Check if reflection can access rikka.shizuku.Shizuku
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val pingBinderMethod = clazz.getMethod("pingBinder")
            val isRunning = pingBinderMethod.invoke(null) as? Boolean ?: false
            isRunning
        } catch (e: Exception) {
            Log.d(TAG, "Shizuku library not present or binder inactive: ${e.message}")
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val checkSelfPermissionMethod = clazz.getMethod("checkSelfPermission")
            val res = checkSelfPermissionMethod.invoke(null) as? Int ?: -1
            // PERMISSION_GRANTED is 0 in Shizuku API
            res == 0
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val requestPermissionMethod = clazz.getMethod("requestPermission", Int::class.java)
            requestPermissionMethod.invoke(null, requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request Shizuku permission: ${e.message}")
        }
    }

    fun getVersion(): Int {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val getVersionMethod = clazz.getMethod("getVersion")
            getVersionMethod.invoke(null) as? Int ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    suspend fun runShizukuCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isShizukuRunning() || !hasShizukuPermission()) {
                return@withContext Result.failure(IllegalStateException("Shizuku is not running or permission not granted."))
            }
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            // Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null)
            val newProcessMethod = clazz.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
                ?: return@withContext Result.failure(RuntimeException("Failed to create Shizuku process."))

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            Result.success(output.toString().trim())
        } catch (e: Exception) {
            Log.e(TAG, "Error running Shizuku command: ${e.message}")
            Result.failure(e)
        }
    }
}

package com.example.tools.otp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AppIdentifier {
    fun inspectApp(context: Context, targetPackageName: String?): AppDiagnosticInfo {
        val pm = context.packageManager
        val pkgName = targetPackageName ?: context.packageName
        try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, 0)
            }

            val appInfo = pkgInfo.applicationInfo
            val targetSdk = appInfo?.targetSdkVersion ?: 0
            val vName = pkgInfo.versionName ?: "1.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkgInfo.longVersionCode else @Suppress("DEPRECATION") pkgInfo.versionCode.toLong()

            return AppDiagnosticInfo(
                packageName = pkgName,
                versionName = vName,
                versionCode = vCode,
                targetSdk = targetSdk,
                status = "AVAILABLE"
            )
        } catch (e: Exception) {
            return AppDiagnosticInfo(
                packageName = pkgName,
                versionName = null,
                versionCode = 0,
                targetSdk = 0,
                status = "ERROR"
            )
        }
    }
}

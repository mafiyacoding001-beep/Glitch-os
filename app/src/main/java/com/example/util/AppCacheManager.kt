package com.example.util

import android.content.Context
import android.util.Log
import java.io.File

object AppCacheManager {

    private const val TAG = "AppCacheManager"
    const val DEFAULT_MAX_CACHE_MB = 100L

    /**
     * Calculates total cache directory size in bytes.
     */
    fun getCacheSizeBytes(context: Context): Long {
        var totalSize: Long = 0
        try {
            // 1. Internal cache directory
            context.cacheDir?.let { totalSize += getFolderSizeBytes(it) }

            // 2. External cache directory
            context.externalCacheDir?.let { totalSize += getFolderSizeBytes(it) }

            // 3. Code cache directory
            context.codeCacheDir?.let { totalSize += getFolderSizeBytes(it) }

            // 4. App specific internal temp files directory
            File(context.filesDir, "temp").let { if (it.exists()) totalSize += getFolderSizeBytes(it) }
            File(context.filesDir, "audio_cache").let { if (it.exists()) totalSize += getFolderSizeBytes(it) }
            File(context.filesDir, "image_cache").let { if (it.exists()) totalSize += getFolderSizeBytes(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
        }
        return totalSize
    }

    /**
     * Formats bytes into human-readable format (e.g. "45.2 MB").
     */
    fun formatSizeBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Formats current cache size directly into a string.
     */
    fun getFormattedCacheSize(context: Context): String {
        return formatSizeBytes(getCacheSizeBytes(context))
    }

    /**
     * Automatically clears cache if the size exceeds threshold in MB (default 100 MB).
     * Returns true if cache was cleared, false otherwise.
     */
    fun checkAndAutoClearCache(context: Context, thresholdMb: Long = DEFAULT_MAX_CACHE_MB): Pair<Boolean, String> {
        val totalBytes = getCacheSizeBytes(context)
        val thresholdBytes = thresholdMb * 1024L * 1024L

        if (totalBytes > thresholdBytes) {
            val freedSizeStr = formatSizeBytes(totalBytes)
            val cleared = clearAllCache(context)
            if (cleared) {
                Log.i(TAG, "Auto-cleared $freedSizeStr cache because size exceeded ${thresholdMb}MB threshold.")
                return Pair(true, "Auto-cleaned $freedSizeStr cache (Exceeded ${thresholdMb}MB limit)")
            }
        }
        return Pair(false, "")
    }

    /**
     * Clears all temporary cache directories safely (preserves exported videos).
     */
    fun clearAllCache(context: Context): Boolean {
        var success = true
        try {
            // 1. Clear internal cache
            context.cacheDir?.let { success = success && deleteDirContents(it) }

            // 2. Clear external cache
            context.externalCacheDir?.let { success = success && deleteDirContents(it) }

            // 3. Clear temporary app directories
            File(context.filesDir, "temp").let { if (it.exists()) deleteDirContents(it) }
            File(context.filesDir, "audio_cache").let { if (it.exists()) deleteDirContents(it) }
            File(context.filesDir, "image_cache").let { if (it.exists()) deleteDirContents(it) }

            // 4. Clear WebView cache if available
            try {
                android.webkit.WebStorage.getInstance().deleteAllData()
            } catch (e: Exception) {
                Log.w(TAG, "WebStorage clean notice: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            return false
        }
        return success
    }

    private fun getFolderSizeBytes(dir: File): Long {
        var size: Long = 0
        if (!dir.exists()) return 0
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) {
                getFolderSizeBytes(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun deleteDirContents(dir: File): Boolean {
        var deletedAll = true
        if (!dir.exists()) return true
        val files = dir.listFiles() ?: return true
        for (file in files) {
            if (file.isDirectory) {
                deletedAll = deleteDirContents(file) && deletedAll
                file.delete()
            } else {
                // Do not delete exported MP4 files if they reside in cache accidentally
                if (!file.name.endsWith(".mp4", ignoreCase = true) || file.name.startsWith("temp_")) {
                    deletedAll = file.delete() && deletedAll
                }
            }
        }
        return deletedAll
    }
}

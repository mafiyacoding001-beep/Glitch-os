package com.example.data.youtube

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

class YouTubeUploader(private val context: Context) {

    companion object {
        private const val TAG = "GLITCHYouTubeUploader"
        private const val CHUNK_SIZE = 1024 * 1024 // 1 MB chunks for robust resumable uploads
    }

    suspend fun uploadVideoResumable(
        videoFile: File,
        title: String,
        description: String,
        tags: List<String>,
        accessToken: String,
        privacyStatus: String = "public",
        categoryId: String = "28",
        onProgress: (Int, String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        require(videoFile.exists() && videoFile.length() > 0) {
            "Video file does not exist or is empty: ${videoFile.absolutePath}"
        }
        require(title.isNotBlank()) {
            "A YouTube video title is required before uploading."
        }
        val normalizedTitle = title.trim()
        require(
            !normalizedTitle.contains("Viral Clickable Title", ignoreCase = true) &&
                !normalizedTitle.contains("viral clickeable", ignoreCase = true) &&
                !normalizedTitle.equals("AI Video", ignoreCase = true) &&
                !normalizedTitle.equals("My Short Video", ignoreCase = true)
        ) {
            "Replace the placeholder video title with a real title before uploading."
        }

        onProgress(5, "[YOUTUBE_UPLOAD] Initializing YouTube Resumable Upload Session...")
        val uploadUrl = safeCall(onProgress) {
            initiateResumableUpload(
                videoFile = videoFile,
                title = title,
                description = description,
                tags = tags,
                accessToken = accessToken,
                privacyStatus = privacyStatus,
                categoryId = categoryId
            )
        }

        onProgress(15, "[YOUTUBE_UPLOAD] Upload session created. Uploading video stream...")
        val videoId = uploadFileChunksWithResume(videoFile, uploadUrl, onProgress)

        val fullYoutubeUrl = "https://youtu.be/$videoId"
        onProgress(100, "[YOUTUBE_UPLOAD_SUCCESS] Upload complete! Video URL: $fullYoutubeUrl")
        fullYoutubeUrl
    }

    private suspend fun <T> safeCall(
        onProgress: (Int, String) -> Unit,
        block: suspend () -> T
    ): T {
        while (true) {
            com.example.util.NetworkMonitor.waitForInternet(context)
            try {
                return block()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                if (isNetworkException(e)) {
                    Log.w(TAG, "Network exception during YouTube upload operation, waiting for internet...", e)
                    onProgress(-1, "[PAUSED_OFFLINE] ⚠️ Internet connection lost during upload. Video preserved. Waiting to reconnect...")
                    kotlinx.coroutines.delay(3000)
                } else {
                    throw e
                }
            }
        }
    }

    private fun isNetworkException(e: Exception): Boolean {
        if (e is java.io.IOException ||
            e is java.net.SocketException ||
            e is java.net.UnknownHostException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.ConnectException ||
            e is javax.net.ssl.SSLException) return true
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("timeout") || msg.contains("connection") || msg.contains("host") || msg.contains("socket") || msg.contains("network")
    }

    private fun initiateResumableUpload(
        videoFile: File,
        title: String,
        description: String,
        tags: List<String>,
        accessToken: String,
        privacyStatus: String,
        categoryId: String
    ): String {
        val url = URL("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.doInput = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000

        conn.setRequestProperty("Authorization", "Bearer $accessToken")
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.setRequestProperty("X-Upload-Content-Type", "video/mp4")
        conn.setRequestProperty("X-Upload-Content-Length", videoFile.length().toString())

        val snippet = JSONObject().apply {
            put("title", title.take(100))
            put("description", description.take(5000))
            if (tags.isNotEmpty()) {
                put("tags", JSONArray(tags))
            }
            put("categoryId", categoryId.ifBlank { "28" })
        }

        val status = JSONObject().apply {
            put("privacyStatus", privacyStatus)
            put("selfDeclaredMadeForKids", false)
        }

        val body = JSONObject().apply {
            put("snippet", snippet)
            put("status", status)
        }

        conn.outputStream.use { os ->
            os.write(body.toString().toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode == 200 || responseCode == 201) {
            val location = conn.getHeaderField("Location")
            if (!location.isNullOrBlank()) {
                return location
            }
            throw IllegalStateException("YouTube API did not return a resumable Location header")
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IllegalStateException("YouTube initiate upload failed (HTTP $responseCode): $err")
        }
    }

    private suspend fun uploadFileChunksWithResume(
        videoFile: File,
        uploadUrl: String,
        onProgress: (Int, String) -> Unit
    ): String {
        val totalBytes = videoFile.length()
        var bytesUploaded = 0L
        val buffer = ByteArray(CHUNK_SIZE)

        FileInputStream(videoFile).use { fis ->
            while (bytesUploaded < totalBytes) {
                val bytesToRead = Math.min(CHUNK_SIZE.toLong(), totalBytes - bytesUploaded).toInt()
                val read = fis.read(buffer, 0, bytesToRead)
                if (read <= 0) break

                val chunkStart = bytesUploaded
                val chunkEnd = bytesUploaded + read - 1

                val completedId = safeCall(onProgress) {
                    val url = URL(uploadUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "PUT"
                    conn.doOutput = true
                    conn.doInput = true
                    conn.connectTimeout = 60_000
                    conn.readTimeout = 60_000

                    conn.setRequestProperty("Content-Type", "video/mp4")
                    conn.setRequestProperty("Content-Length", read.toString())
                    conn.setRequestProperty("Content-Range", "bytes $chunkStart-$chunkEnd/$totalBytes")

                    conn.outputStream.use { os ->
                        os.write(buffer, 0, read)
                        os.flush()
                    }

                    val code = conn.responseCode
                    if (code == 308) {
                        bytesUploaded += read
                        val percent = ((bytesUploaded.toDouble() / totalBytes) * 80).toInt() + 15
                        onProgress(percent, "Uploaded ${(bytesUploaded / 1024 / 1024)}MB / ${(totalBytes / 1024 / 1024)}MB (${percent}%)")
                        null
                    } else if (code == 200 || code == 201) {
                        val resp = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(resp)
                        bytesUploaded += read
                        json.getString("id")
                    } else {
                        val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        throw IllegalStateException("YouTube chunk upload failed (HTTP $code): $err")
                    }
                }

                if (!completedId.isNullOrBlank()) {
                    return completedId
                }
            }
        }

        val lastCheckUrl = URL(uploadUrl)
        val conn = lastCheckUrl.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Content-Range", "bytes */$totalBytes")
        conn.connectTimeout = 30_000
        conn.readTimeout = 30_000
        val code = conn.responseCode
        if (code == 200 || code == 201) {
            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            return JSONObject(resp).getString("id")
        }

        throw IllegalStateException("Upload finished without a completed YouTube video ID response")
    }
}

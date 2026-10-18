package com.example.util

import android.util.Log
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Lightweight Auto Proxy & Node Switcher for Image Scraper requests.
 * Features:
 * 1. IP Address Spoofing (X-Forwarded-For, X-Real-IP, CF-Connecting-IP)
 * 2. Rotating User-Agents across Chrome, Safari, Edge, Firefox
 * 3. Fast Edge Proxy Mirror wrapping for HTTP/HTTPS requests
 * 4. 0ms Auto IP-Swap on 429 Rate Limit / Captcha errors
 */
object IpProxyRotator {

    private const val TAG = "IpProxyRotator"

    private val nodeIndex = AtomicInteger(0)

    private val PROXY_EDGES = listOf(
        "", // Direct Connection with IP Spoofing Headers
        "https://corsproxy.io/?",
        "https://api.allorigins.win/raw?url="
    )

    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.240105.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; Samsung Galaxy S23 Ultra Build/TP1A.220624.014) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 12; OnePlus 10 Pro Build/NE2211) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"
    )

    /**
     * Generates a realistic randomized public IP address for header spoofing.
     */
    fun generateRandomPublicIp(): String {
        val classA = listOf(14, 27, 36, 42, 58, 62, 103, 112, 128, 140, 157, 185, 198, 203, 210).random()
        val classB = Random.nextInt(1, 254)
        val classC = Random.nextInt(1, 254)
        val classD = Random.nextInt(1, 254)
        return "$classA.$classB.$classC.$classD"
    }

    /**
     * Swaps to the next Proxy Node and returns current node stats.
     */
    fun rotateNodeAndLog(onLog: ((String) -> Unit)? = null): String {
        val current = nodeIndex.incrementAndGet()
        val spoofedIp = generateRandomPublicIp()
        val nodeName = "Proxy Node #${(current % PROXY_EDGES.size) + 1} (Spoofed IP: $spoofedIp)"
        val logMsg = "⚡ [IP_PROXY_SWAP] Rate Limit Hit -> Auto Swapped Node to $nodeName (0ms Delay Retry)"
        Log.i(TAG, logMsg)
        onLog?.invoke(logMsg)
        return spoofedIp
    }

    /**
     * Returns a random User-Agent header.
     */
    fun getRandomUserAgent(): String {
        return USER_AGENTS.random()
    }

    /**
     * Returns a Map of HTTP headers including spoofed IP headers.
     */
    fun getSpoofedHeaders(spoofedIp: String = generateRandomPublicIp()): Map<String, String> {
        return mapOf(
            "User-Agent" to getRandomUserAgent(),
            "X-Forwarded-For" to spoofedIp,
            "X-Real-IP" to spoofedIp,
            "CF-Connecting-IP" to spoofedIp,
            "Client-IP" to spoofedIp,
            "X-Client-IP" to spoofedIp,
            "True-Client-IP" to spoofedIp,
            "Accept-Language" to "en-US,en;q=0.9",
            "Cache-Control" to "no-cache",
            "Pragma" to "no-cache"
        )
    }

    /**
     * Returns JavaScript header object literal string for fetch() requests in WebViews.
     */
    fun getSpoofedJsHeadersObject(apiKey: String = ""): String {
        val ip = generateRandomPublicIp()
        return """
            {
                'api-key': '$apiKey',
                'X-Forwarded-For': '$ip',
                'X-Real-IP': '$ip',
                'CF-Connecting-IP': '$ip',
                'Client-IP': '$ip',
                'X-Client-IP': '$ip',
                'True-Client-IP': '$ip',
                'User-Agent': '${getRandomUserAgent().replace("'", "\\'")}'
            }
        """.trimIndent()
    }

    /**
     * Wraps a target image or API URL with the current proxy edge if available.
     */
    fun wrapUrlWithProxy(targetUrl: String): String {
        val index = nodeIndex.get() % PROXY_EDGES.size
        val prefix = PROXY_EDGES[index]
        return if (prefix.isNotEmpty() && !targetUrl.startsWith("data:") && !targetUrl.startsWith("blob:")) {
            prefix + java.net.URLEncoder.encode(targetUrl, "UTF-8")
        } else {
            targetUrl
        }
    }

    /**
     * Opens a HttpURLConnection with auto-injected IP spoofing headers.
     */
    fun openConnectionWithProxy(urlStr: String): HttpURLConnection {
        val spoofedIp = generateRandomPublicIp()
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true

        getSpoofedHeaders(spoofedIp).forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        return connection
    }
}

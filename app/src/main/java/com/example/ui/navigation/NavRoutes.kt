package com.example.ui.navigation

sealed class NavRoute(val route: String) {
    object Login : NavRoute("login")
    object Home : NavRoute("home")
    object Create : NavRoute("create")
    object Progress : NavRoute("progress/{jobId}") {
        fun createRoute(jobId: String) = "progress/$jobId"
    }
    object Preview : NavRoute("preview/{videoUrl}/{title}") {
        fun createRoute(videoUrl: String, title: String): String {
            val encodedUrl = java.net.URLEncoder.encode(videoUrl, "UTF-8")
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            return "preview/$encodedUrl/$encodedTitle"
        }
    }
    object Library : NavRoute("library")
    object Details : NavRoute("details/{projectId}") {
        fun createRoute(projectId: String) = "details/$projectId"
    }
    object Settings : NavRoute("settings")
    object Logs : NavRoute("logs")
    object YouTubeManager : NavRoute("youtube_manager")
    object KeyboardPlayground : NavRoute("keyboard_playground")
    object VpnShield : NavRoute("vpn_shield")
    object TrendPilot : NavRoute("trend_pilot")
    object Chatbot : NavRoute("chatbot")
    object YtPlayer : NavRoute("yt_player/{initialVideoId}/{category}/{searchQuery}") {
        fun createRoute(initialVideoId: String, category: String, searchQuery: String): String {
            val encodedId = java.net.URLEncoder.encode(initialVideoId, "UTF-8")
            val encodedCategory = java.net.URLEncoder.encode(category, "UTF-8")
            val encodedQuery = if (searchQuery.isBlank()) "-" else java.net.URLEncoder.encode(searchQuery, "UTF-8")
            return "yt_player/$encodedId/$encodedCategory/$encodedQuery"
        }
    }
}

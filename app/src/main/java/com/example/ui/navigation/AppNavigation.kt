package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.components.AppUpdateDialog
import com.example.ui.viewmodels.MainViewModel
import com.example.util.GoogleSignInHelper

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val appUpdateData by viewModel.appUpdateData.collectAsState()

    val startDestination = NavRoute.Home.route

    if (appUpdateData != null && appUpdateData!!.shouldShowAlert) {
        AppUpdateDialog(appUpdate = appUpdateData!!)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoute.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCreate = { navController.navigate(NavRoute.Create.route) },
                onNavigateToProgress = { jobId -> navController.navigate(NavRoute.Progress.createRoute(jobId)) },
                onNavigateToPreview = { url, title -> navController.navigate(NavRoute.Preview.createRoute(url, title)) },
                onNavigateToLibrary = { navController.navigate(NavRoute.Library.route) },
                onNavigateToSettings = { navController.navigate(NavRoute.Settings.route) },
                onNavigateToYouTubeManager = { navController.navigate(NavRoute.YouTubeManager.route) },
                onNavigateToKeyboardPlayground = { navController.navigate(NavRoute.KeyboardPlayground.route) },
                onNavigateToVpnShield = { navController.navigate(NavRoute.VpnShield.route) },
                onNavigateToLogin = { navController.navigate(NavRoute.Login.route) }
            )
        }

        composable(NavRoute.Create.route) {
            CreateVideoScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProgress = { jobId ->
                    navController.navigate(NavRoute.Progress.createRoute(jobId)) {
                        popUpTo(NavRoute.Home.route)
                    }
                }
            )
        }

        composable(
            route = NavRoute.Progress.route,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: ""
            ProgressScreen(
                jobId = jobId,
                viewModel = viewModel,
                onNavigateToPreview = { url, title ->
                    navController.navigate(NavRoute.Preview.createRoute(url, title)) {
                        popUpTo(NavRoute.Home.route)
                    }
                },
                onNavigateHome = {
                    navController.navigate(NavRoute.Home.route) {
                        popUpTo(NavRoute.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoute.Preview.route,
            arguments = listOf(
                navArgument("videoUrl") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val videoUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: "Video Preview"
            PreviewScreen(
                videoUrl = videoUrl,
                title = title,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.Library.route) {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { url, title -> navController.navigate(NavRoute.Preview.createRoute(url, title)) },
                onNavigateToDetails = { id -> navController.navigate(NavRoute.Details.createRoute(id)) }
            )
        }

        composable(
            route = NavRoute.Details.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectDetailsScreen(
                projectId = projectId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { url, title -> navController.navigate(NavRoute.Preview.createRoute(url, title)) }
            )
        }

        composable(NavRoute.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogs = { navController.navigate(NavRoute.Logs.route) }
            )
        }

        composable(NavRoute.Logs.route) {
            LogsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.YouTubeManager.route) {
            YouTubeManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { url, title ->
                    navController.navigate(NavRoute.Preview.createRoute(url, title))
                }
            )
        }

        composable(NavRoute.KeyboardPlayground.route) {
            KeyboardPlaygroundScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.VpnShield.route) {
            VpnShieldScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoute.TrendPilot.route) {
            TrendPilotScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateWithTopic = { _ ->
                    navController.navigate(NavRoute.Create.route)
                },
                onNavigateToYtPlayer = { initialVideoId, category, searchQuery ->
                    navController.navigate(NavRoute.YtPlayer.createRoute(initialVideoId, category, searchQuery))
                }
            )
        }

        composable(NavRoute.Chatbot.route) {
            ChatbotScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { url, title -> navController.navigate(NavRoute.Preview.createRoute(url, title)) }
            )
        }

        composable(
            route = NavRoute.YtPlayer.route,
            arguments = listOf(
                navArgument("initialVideoId") { type = NavType.StringType },
                navArgument("category") { type = NavType.StringType },
                navArgument("searchQuery") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val initialVideoId = backStackEntry.arguments?.getString("initialVideoId") ?: ""
            val category = backStackEntry.arguments?.getString("category") ?: "ALL"
            val rawQuery = backStackEntry.arguments?.getString("searchQuery") ?: ""
            val searchQuery = if (rawQuery == "-") "" else rawQuery

            com.example.ytplayer.YtShortsPagerScreen(
                initialVideoId = initialVideoId,
                category = category,
                searchQuery = searchQuery,
                onNavigateBack = { navController.popBackStack() },
                onRepurposeVideo = { video ->
                    navController.navigate(NavRoute.Create.route)
                }
            )
        }
    }
}

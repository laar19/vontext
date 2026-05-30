package com.videocontextbot.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.videocontextbot.ui.screens.HomeScreen
import com.videocontextbot.ui.screens.ProcessingScreen
import com.videocontextbot.ui.screens.ResultsScreen

object Routes {
    const val HOME = "home"
    const val PROCESSING = "processing/{jobId}"
    const val RESULTS = "results/{jobId}"

    fun processing(jobId: String) = "processing/$jobId"
    fun results(jobId: String) = "results/$jobId"
}

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToProcessing = { jobId ->
                    navController.navigate(Routes.processing(jobId))
                }
            )
        }

        composable(
            route = Routes.PROCESSING,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            ProcessingScreen(
                jobId = jobId,
                onNavigateToResults = { id ->
                    navController.navigate(Routes.results(id)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }

        composable(
            route = Routes.RESULTS,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            ResultsScreen(
                jobId = jobId,
                onNavigateBack = { navController.popBackStack(Routes.HOME, false) }
            )
        }
    }
}

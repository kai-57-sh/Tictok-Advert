package com.example.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun TictokAdvertApp() {
    val navController = rememberNavController()
    // Sharing a single parent ViewModel across stack pages guarantees smooth, unified state synchronization!
    val viewModel: AdvertViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "feed"
    ) {
        composable("feed") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = { adId ->
                    navController.navigate("detail/$adId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToStats = {
                    navController.navigate("stats")
                }
            )
        }

        composable(
            route = "detail/{adId}",
            arguments = listOf(navArgument("adId") { type = NavType.StringType })
        ) { backStackEntry ->
            val adId = backStackEntry.arguments?.getString("adId") ?: ""
            DetailsScreen(
                adId = adId,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { adId ->
                    navController.navigate("detail/$adId")
                }
            )
        }

        composable("stats") {
            StatsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

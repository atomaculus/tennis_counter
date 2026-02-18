package com.example.tenniscounter.mobile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.ui.detail.MatchDetailScreen
import com.example.tenniscounter.mobile.ui.detail.MatchDetailViewModel
import com.example.tenniscounter.mobile.ui.history.HistoryScreen
import com.example.tenniscounter.mobile.ui.history.HistoryViewModel

private const val HISTORY_ROUTE = "history"
private const val DETAIL_ROUTE = "detail/{matchId}"
private const val DETAIL_ROUTE_PREFIX = "detail"

@Composable
fun MobileApp() {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val repository = remember(appContext) { MobileServiceLocator.matchRepository(appContext) }

    NavHost(
        navController = navController,
        startDestination = HISTORY_ROUTE
    ) {
        composable(HISTORY_ROUTE) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(repository)
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onMatchClick = { matchId ->
                    navController.navigate("$DETAIL_ROUTE_PREFIX/$matchId")
                },
                onNewMatch = { onCreated ->
                    historyViewModel.createDefaultMatch(onCreated)
                }
            )
        }

        composable(
            route = DETAIL_ROUTE,
            arguments = listOf(navArgument("matchId") { type = NavType.LongType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getLong("matchId") ?: return@composable
            val detailViewModel: MatchDetailViewModel = viewModel(
                key = "match_detail_$matchId",
                factory = MatchDetailViewModel.factory(matchId, repository)
            )
            MatchDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

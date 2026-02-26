package com.example.tenniscounter.mobile

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tenniscounter.mobile.billing.PremiumBillingManager
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.ui.counter.MobileCounterScreen
import com.example.tenniscounter.mobile.ui.counter.MobileCounterViewModel
import com.example.tenniscounter.mobile.ui.detail.MatchDetailScreen
import com.example.tenniscounter.mobile.ui.detail.MatchDetailViewModel
import com.example.tenniscounter.mobile.ui.history.HistoryScreen
import com.example.tenniscounter.mobile.ui.history.HistoryViewModel

private const val COUNTER_ROUTE = "counter"
private const val HISTORY_ROUTE = "history"
private const val DETAIL_ROUTE = "detail/{matchId}"
private const val DETAIL_ROUTE_PREFIX = "detail"

@Composable
fun MobileApp() {
    val navController = rememberNavController()
    val localContext = LocalContext.current
    val appContext = localContext.applicationContext
    val activity = localContext as? Activity
    val repository = remember(appContext) { MobileServiceLocator.matchRepository(appContext) }
    val premiumBillingManager = remember(appContext) { PremiumBillingManager(appContext) }
    val premiumUiState = premiumBillingManager.uiState.collectAsStateWithLifecycle().value

    DisposableEffect(premiumBillingManager) {
        premiumBillingManager.start()
        onDispose { premiumBillingManager.dispose() }
    }

    NavHost(
        navController = navController,
        startDestination = COUNTER_ROUTE
    ) {
        composable(COUNTER_ROUTE) {
            val counterViewModel: MobileCounterViewModel = viewModel()
            MobileCounterScreen(
                viewModel = counterViewModel,
                premiumUiState = premiumUiState,
                onOpenHistory = { navController.navigate(HISTORY_ROUTE) },
                onUnlockPremium = { activity?.let(premiumBillingManager::launchPurchase) }
            )
        }

        composable(HISTORY_ROUTE) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(repository)
            )
            HistoryScreen(
                viewModel = historyViewModel,
                premiumUiState = premiumUiState,
                onUnlockPremium = { activity?.let(premiumBillingManager::launchPurchase) },
                onRestorePurchases = premiumBillingManager::restorePurchases,
                onOpenCounter = {
                    navController.navigate(COUNTER_ROUTE)
                },
                onMatchClick = { matchId ->
                    navController.navigate("$DETAIL_ROUTE_PREFIX/$matchId")
                },
                onNewMatch = { onCreated ->
                    if (premiumUiState.isPremiumUnlocked) {
                        historyViewModel.createDefaultMatch(onCreated)
                    }
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
                onBack = { navController.popBackStack() },
                premiumUiState = premiumUiState,
                onUnlockPremium = { activity?.let(premiumBillingManager::launchPurchase) },
                onRestorePurchases = premiumBillingManager::restorePurchases
            )
        }
    }
}

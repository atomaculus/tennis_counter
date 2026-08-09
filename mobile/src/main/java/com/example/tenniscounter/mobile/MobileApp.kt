package com.example.tenniscounter.mobile

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tenniscounter.mobile.billing.PremiumBillingManager
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.sync.LiveScoreRepository
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.counter.LiveScoreScreen
import com.example.tenniscounter.mobile.ui.counter.MobileCounterScreen
import com.example.tenniscounter.mobile.ui.counter.MobileCounterViewModel
import com.example.tenniscounter.mobile.ui.counter.MobileVeintiunoScreen
import com.example.tenniscounter.mobile.ui.counter.ParedonStubScreen
import com.example.tenniscounter.mobile.ui.counter.PlayModeSelectScreen
import com.example.tenniscounter.mobile.ui.detail.MatchDetailScreen
import com.example.tenniscounter.mobile.ui.detail.MatchDetailViewModel
import com.example.tenniscounter.mobile.ui.history.HistoryScreen
import com.example.tenniscounter.mobile.ui.history.HistoryViewModel
import com.example.tenniscounter.mobile.export.MatchExporter
import com.example.tenniscounter.mobile.review.InAppReviewManager
import com.example.tenniscounter.mobile.sync.MatchConfigBroadcaster
import com.example.tenniscounter.mobile.ui.components.GarminConnectionBadge
import com.example.tenniscounter.mobile.ui.counter.FormatPreset
import com.example.tenniscounter.mobile.ui.stats.StatsScreen
import com.example.tenniscounter.mobile.ui.stats.StatsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope

private const val COUNTER_ROUTE = "counter"
private const val HISTORY_ROUTE = "history"
private const val STATS_ROUTE = "stats"
private const val DETAIL_ROUTE = "detail/{matchId}"
private const val DETAIL_ROUTE_PREFIX = "detail"

/** Modality picked from [PlayModeSelectScreen] within the "Play" tab, for this composition only (not persisted). */
private enum class PlayMode {
    Match, Paredon, Veintiuno
}

@Composable
fun MobileApp() {
    val navController = rememberNavController()
    val localContext = LocalContext.current
    val appContext = localContext.applicationContext
    val activity = localContext as? Activity
    val repository = remember(appContext) { MobileServiceLocator.matchRepository(appContext) }
    val configBroadcaster = remember(appContext) { MatchConfigBroadcaster(appContext) }
    val garminConfigSender = remember(appContext) { MobileServiceLocator.garminMatchConfigSender(appContext) }
    val premiumBillingManager = remember(appContext) { PremiumBillingManager(appContext) }
    val premiumUiState = premiumBillingManager.uiState.collectAsStateWithLifecycle().value

    DisposableEffect(premiumBillingManager) {
        premiumBillingManager.start()
        onDispose { premiumBillingManager.dispose() }
    }

    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == COUNTER_ROUTE || currentRoute == HISTORY_ROUTE || currentRoute == STATS_ROUTE

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = PlayceColors.Surface,
                    contentColor = PlayceColors.TextPrimary
                ) {
                    data class NavItem(val route: String, val label: String, val iconRes: Int)
                    listOf(
                        NavItem(COUNTER_ROUTE, stringResource(R.string.tab_play), R.drawable.ic_counter),
                        NavItem(HISTORY_ROUTE, stringResource(R.string.tab_history), R.drawable.ic_history),
                        NavItem(STATS_ROUTE, stringResource(R.string.tab_stats), R.drawable.ic_stats)
                    ).forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(COUNTER_ROUTE) {
                                        saveState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PlayceColors.Accent,
                                selectedTextColor = PlayceColors.Accent,
                                unselectedIconColor = PlayceColors.TextSecondary,
                                unselectedTextColor = PlayceColors.TextSecondary,
                                indicatorColor = PlayceColors.AccentMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = COUNTER_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(COUNTER_ROUTE) {
                val liveState = LiveScoreRepository.state.collectAsStateWithLifecycle().value
                val counterViewModel: MobileCounterViewModel = viewModel()
                val hasActiveLocalMatch = counterViewModel.hasActiveMatch.collectAsStateWithLifecycle().value

                // Detect when a live watch match ends (state goes non-null → null)
                var wasLive by remember { mutableStateOf(false) }
                LaunchedEffect(liveState) {
                    if (liveState != null) {
                        wasLive = true
                    } else if (wasLive) {
                        // Match just ended on the watch
                        wasLive = false
                        activity?.let { InAppReviewManager.onMatchCompleted(it) }
                    }
                }

                // A session already in progress (local match, or a live watch
                // match) always wins over the mode selector.
                var playMode by remember { mutableStateOf<PlayMode?>(null) }
                val skipModeSelect = liveState != null || hasActiveLocalMatch

                Box(modifier = Modifier.fillMaxSize()) {
                if (liveState != null) {
                    LiveScoreScreen(
                        liveState = liveState,
                        receivedAtElapsedRealtime = LiveScoreRepository.receivedAtElapsedRealtime
                    )
                } else if (!skipModeSelect && playMode == null) {
                    PlayModeSelectScreen(
                        onSelectMatch = { playMode = PlayMode.Match },
                        onSelectParedon = { playMode = PlayMode.Paredon },
                        onSelectVeintiuno = { playMode = PlayMode.Veintiuno }
                    )
                } else if (!skipModeSelect && playMode == PlayMode.Veintiuno) {
                    MobileVeintiunoScreen(onExit = { playMode = null })
                } else if (!skipModeSelect && playMode == PlayMode.Paredon) {
                    ParedonStubScreen(onBack = { playMode = null })
                } else {
                    MobileCounterScreen(
                        viewModel = counterViewModel,
                        premiumUiState = premiumUiState,
                        onUnlockPremium = { activity?.let(premiumBillingManager::launchPurchase) },
                        onMatchCompleted = {
                            activity?.let { InAppReviewManager.onMatchCompleted(it) }
                        },
                        onSendConfigToWatch = { nameA, nameB, preset, initialServerIsPlayerA ->
                            val resolvedA = nameA.ifBlank { "Player A" }
                            val resolvedB = nameB.ifBlank { "Player B" }
                            // Wear OS path (existing, untouched)
                            configBroadcaster.sendConfig(
                                playerAName = resolvedA,
                                playerBName = resolvedB,
                                setsToWin = preset.setsToWin,
                                tiebreakAtSixAll = preset.tiebreakAtSixAll,
                                superTiebreakInFinalSet = preset.superTiebreakInFinalSet,
                                noAdScoring = preset.noAdScoring,
                                initialServerIsPlayerA = initialServerIsPlayerA
                            )
                            // Garmin path (parallel, no-op if no Garmin device connected)
                            garminConfigSender.sendConfig(
                                playerAName = resolvedA,
                                playerBName = resolvedB,
                                setsToWin = preset.setsToWin,
                                tiebreakAtSixAll = preset.tiebreakAtSixAll,
                                superTiebreakInFinalSet = preset.superTiebreakInFinalSet,
                                noAdScoring = preset.noAdScoring,
                                initialServerIsPlayerA = initialServerIsPlayerA
                            )
                        },
                        onSaveMatch = { summary ->
                            val matchDao = MobileServiceLocator.matchDao(appContext)
                            val pointEventsJson = counterViewModel.pointEventsJson()
                            kotlinx.coroutines.MainScope().launch {
                                withContext(Dispatchers.IO) {
                                    val entity = com.example.tenniscounter.mobile.data.local.MatchEntity(
                                        createdAt = summary.createdAt,
                                        durationSeconds = summary.durationSeconds.toLong(),
                                        finalScoreText = summary.setsScore,
                                        setScoresText = summary.completedSetsText,
                                        idempotencyKey = "mobile_${summary.createdAt}",
                                        pointEventsJson = pointEventsJson
                                    )
                                    matchDao.insertOrIgnore(entity)
                                }
                            }
                        }
                    )
                }
                    GarminConnectionBadge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                    )
                }
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
                        navController.navigate(COUNTER_ROUTE) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(COUNTER_ROUTE) {
                                saveState = true
                            }
                        }
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

            composable(STATS_ROUTE) {
                val matchDao = remember(appContext) { MobileServiceLocator.matchDao(appContext) }
                val statsViewModel: StatsViewModel = viewModel(
                    factory = StatsViewModel.factory(matchDao)
                )
                val exportScope = rememberCoroutineScope()
                StatsScreen(
                    viewModel = statsViewModel,
                    premiumUiState = premiumUiState,
                    onUnlockPremium = { activity?.let(premiumBillingManager::launchPurchase) },
                    onRestorePurchases = premiumBillingManager::restorePurchases,
                    onOpenCounter = {
                        navController.navigate(COUNTER_ROUTE) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(COUNTER_ROUTE) {
                                saveState = true
                            }
                        }
                    },
                    onExport = {
                        exportScope.launch {
                            val matches = withContext(Dispatchers.IO) {
                                MobileServiceLocator.matchDao(appContext).getAllMatchesOnce()
                            }
                            val intent = MatchExporter.exportToCsv(appContext, matches)
                            if (intent != null) {
                                localContext.startActivity(Intent.createChooser(intent, "Export matches"))
                            }
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
                    onDeleted = { navController.popBackStack() },
                    premiumUiState = premiumUiState,
                    onUnlockPremium = { activity?.let(premiumBillingManager::launchPurchase) },
                    onRestorePurchases = premiumBillingManager::restorePurchases
                )
            }
        }
    }
}

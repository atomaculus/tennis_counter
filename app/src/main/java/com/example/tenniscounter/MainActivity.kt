package com.example.tenniscounter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.tenniscounter.sync.PendingMatchMessage
import com.example.tenniscounter.sync.PendingMatchStore
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.example.tenniscounter.sound.PointSoundManager
import com.example.tenniscounter.health.PlayceHealthServicesManager
import com.example.tenniscounter.ui.components.PlayceWearColors
import com.example.tenniscounter.ui.components.PlayceWearSpacing
import com.example.tenniscounter.ui.components.PlayceWearShapes
import com.example.tenniscounter.ui.components.PlayceButtonVariant
import com.example.tenniscounter.ui.components.PlayceCard
import com.example.tenniscounter.ui.components.PlayceChip
import com.example.tenniscounter.ui.components.PlayceButton
import com.example.tenniscounter.ui.components.TennisWearTheme
import com.example.tenniscounter.ui.components.SheetAction
import com.example.tenniscounter.sync.MatchConfigRepository
import com.example.tenniscounter.sync.LiveScoreBroadcaster
import com.example.tenniscounter.sync.LiveScoreObserver
import com.example.tenniscounter.sync.WearLiveMatchState
import com.example.tenniscounter.ui.FinishedMatchSummary
import com.example.tenniscounter.ui.MatchState
import com.example.tenniscounter.ui.SpectatorScreen
import com.example.tenniscounter.ui.TennisViewModel
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val WEAR_DATA_LAYER_TAG = "WearDataLayer"
private const val ACK_WAIT_RETRY_MS = 10_000L
private const val RETRY_TRIGGER_THROTTLE_MS = 1_500L
private val retryScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun healthPermissionsForDevice(): Array<String> {
    return buildList {
        add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT <= 35) {
            add(Manifest.permission.BODY_SENSORS)
        }
        add("android.permission.health.READ_HEART_RATE")
    }.toTypedArray()
}

/**
 * Versioned "What's New" flag. Bump [CURRENT_WHATSNEW_VERSION] whenever the
 * controls change and the overlay should be shown again after an update.
 */
private object WhatsNewPrefs {
    const val CURRENT_WHATSNEW_VERSION = 2
    private const val PREFS_NAME = "playce_whatsnew"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version"

    fun shouldShow(context: Context): Boolean {
        val lastSeen = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, 0)
        return lastSeen < CURRENT_WHATSNEW_VERSION
    }

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_SEEN_VERSION, CURRENT_WHATSNEW_VERSION)
            .apply()
    }
}

private enum class AppScreen {
    Counter,
    MatchFinished,
    Spectator
}

private enum class ActiveSheet {
    None,
    PlayerA,
    PlayerB,
    Admin,
    EndMatchConfirm,
    InitialServer,
    DecidingTiebreak
}

private enum class PhoneSendResult {
    Sent,
    NoConnectedPhone,
    Failed
}

private data class SpectatorUiState(
    val hasActiveLiveMatch: Boolean,
    val spectatorState: WearLiveMatchState?
)

private data class ServeIndicatorState(
    val serverLabel: String,
    val isPlayerAServing: Boolean,
    val serveOnLeftSide: Boolean
)

class MainActivity : ComponentActivity() {

    private val isAmbient = mutableStateOf(false)
    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient.value = true
        }

        override fun onExitAmbient() {
            isAmbient.value = false
        }
    }
    private val ambientObserver = AmbientLifecycleObserver(this, ambientCallback)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Declares ambient support: during a match the activity stays on screen
        // in ambient mode (simplified scoreboard) instead of falling back to the
        // watch face.
        lifecycle.addObserver(ambientObserver)
        setContent {
            TennisWearTheme {
                TennisCounterApp(isAmbient = isAmbient.value)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        triggerPendingRetry(applicationContext, "onStart")
    }

    override fun onResume() {
        super.onResume()
        triggerPendingRetry(applicationContext, "onResume")
    }
}

@Composable
private fun TennisCounterApp(
    isAmbient: Boolean = false,
    viewModel: TennisViewModel = viewModel()
) {
    val state by viewModel.matchState.collectAsState()
    val finishedSummary by viewModel.finishedMatch.collectAsState()
    val isSaved by viewModel.isFinishedMatchSaved.collectAsState()
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val pointSound = remember { PointSoundManager() }
    val liveBroadcaster = remember { LiveScoreBroadcaster(context) }
    val healthServicesManager = remember { PlayceHealthServicesManager(context.applicationContext) }
    var localNodeId by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        onDispose { pointSound.release() }
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val node = Tasks.await(Wearable.getNodeClient(context).localNode)
                localNodeId = node.id
            } catch (e: Exception) {
                Log.w(WEAR_DATA_LAYER_TAG, "Failed to get local node id", e)
            }
        }
    }

    fun hasHealthPermissions(): Boolean {
        return healthPermissionsForDevice().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun startWorkoutIfPermitted() {
        if (!hasHealthPermissions()) return
        uiScope.launch {
            healthServicesManager.startMatchWorkoutIfPossible()
        }
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        startWorkoutIfPermitted()
    }

    LaunchedEffect(Unit) {
        val missing = mutableListOf<String>()
        if (!hasHealthPermissions()) {
            missing += healthPermissionsForDevice()
        }
        // Needed so the foreground-service notification (and its watch-face chip)
        // can be shown while a match is running.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing += Manifest.permission.POST_NOTIFICATIONS
        }
        if (missing.isEmpty()) {
            healthServicesManager.startMatchWorkoutIfPossible()
        } else {
            healthPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    // Helper to broadcast current state after any score change
    fun broadcastCurrentState(lastScoredPlayer: String) {
        if (localNodeId.isNotEmpty()) {
            liveBroadcaster.broadcastState(
                state = viewModel.matchState.value,
                lastScoredPlayer = lastScoredPlayer,
                scorerNodeId = localNodeId
            )
        }
    }

    // Observe match config sent from the phone
    val pendingConfig by MatchConfigRepository.pendingConfig.collectAsState()
    LaunchedEffect(pendingConfig) {
        val config = pendingConfig ?: return@LaunchedEffect
        viewModel.setPlayerNames(config.playerAName, config.playerBName)
        viewModel.setMatchFormat(config.format)
        config.initialServerIsPlayerA?.let { viewModel.setInitialServer(it) }
        MatchConfigRepository.consume()
        Log.d(WEAR_DATA_LAYER_TAG, "Applied config from phone: ${config.playerAName} vs ${config.playerBName}")
    }

    val spectatorUiState = rememberSpectatorUiState(
        context = context,
        localNodeId = localNodeId
    )
    val spectatorState = spectatorUiState.spectatorState

    var appScreen by remember { mutableStateOf(AppScreen.Counter) }
    var activeSheet by remember { mutableStateOf(ActiveSheet.None) }
    var transientMessage by remember { mutableStateOf<String?>(null) }
    var showWhatsNew by remember { mutableStateOf(WhatsNewPrefs.shouldShow(context)) }

    // Ask who serves first before the first point (unless phone config already fixed it).
    val showInitialServerPrompt by viewModel.showInitialServerPrompt.collectAsState()
    LaunchedEffect(showInitialServerPrompt, appScreen, activeSheet) {
        if (showInitialServerPrompt && appScreen == AppScreen.Counter && activeSheet == ActiveSheet.None) {
            activeSheet = ActiveSheet.InitialServer
        } else if (!showInitialServerPrompt && activeSheet == ActiveSheet.InitialServer) {
            activeSheet = ActiveSheet.None
        }
    }

    // Offer a deciding tiebreak right after a set leaves the players tied on sets.
    val offerDecidingTiebreak by viewModel.offerDecidingTiebreak.collectAsState()
    LaunchedEffect(offerDecidingTiebreak, appScreen, activeSheet) {
        if (offerDecidingTiebreak && appScreen == AppScreen.Counter && activeSheet == ActiveSheet.None) {
            activeSheet = ActiveSheet.DecidingTiebreak
        } else if (!offerDecidingTiebreak && activeSheet == ActiveSheet.DecidingTiebreak) {
            activeSheet = ActiveSheet.None
        }
    }

    // The winning point takes the watch straight to the finished screen,
    // same as mobile and iOS. END MATCH remains for ending mid-match.
    LaunchedEffect(state.isMatchOver) {
        if (state.isMatchOver && appScreen == AppScreen.Counter) {
            val healthMetrics = healthServicesManager.endWorkoutAndGetMetrics()
            viewModel.finishMatch(healthMetrics)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            liveBroadcaster.clearLiveScore()
            activeSheet = ActiveSheet.None
            appScreen = AppScreen.MatchFinished
        }
    }

    // If spectator is watching and the match ends, go back to counter
    LaunchedEffect(spectatorState) {
        if (appScreen == AppScreen.Spectator && spectatorState == null) {
            appScreen = AppScreen.Counter
        }
    }
    var saveTapSignal by remember { mutableIntStateOf(0) }

    var isPressedA by remember { mutableStateOf(false) }
    var isPressedB by remember { mutableStateOf(false) }
    var pressedAtA by remember { mutableLongStateOf(0L) }
    var pressedAtB by remember { mutableLongStateOf(0L) }
    var simultaneousLongPressHandled by remember { mutableStateOf(false) }

    val simultaneousWindowMs = 420L

    fun onPressStateChange(isPlayerA: Boolean, isPressed: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (isPlayerA) {
            isPressedA = isPressed
            if (isPressed) pressedAtA = now
        } else {
            isPressedB = isPressed
            if (isPressed) pressedAtB = now
        }

        if (!isPressedA && !isPressedB) {
            simultaneousLongPressHandled = false
        }
    }

    // Simultaneous long press requires both downs close in time + both still pressed to reduce false positives.
    fun shouldOpenAdminSheet(): Boolean {
        return isPressedA && isPressedB && abs(pressedAtA - pressedAtB) <= simultaneousWindowMs
    }

    fun handleLongPress(isPlayerA: Boolean) {
        if (shouldOpenAdminSheet()) {
            if (!simultaneousLongPressHandled) {
                simultaneousLongPressHandled = true
                activeSheet = ActiveSheet.Admin
            }
            return
        }

        if (simultaneousLongPressHandled) return
        activeSheet = if (isPlayerA) ActiveSheet.PlayerA else ActiveSheet.PlayerB
    }

    LaunchedEffect(transientMessage) {
        if (transientMessage != null) {
            delay(1200)
            transientMessage = null
        }
    }

    LaunchedEffect(appScreen) {
        if (appScreen == AppScreen.Counter) {
            viewModel.onCounterScreenVisible()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (appScreen) {
            AppScreen.Counter -> {
                CounterScreen(
                    state = state,
                    onTapPointA = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pointSound.playPlayerASound()
                        viewModel.addPointToPlayerA()
                        broadcastCurrentState("A")
                    },
                    onTapPointB = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pointSound.playPlayerBSound()
                        viewModel.addPointToPlayerB()
                        broadcastCurrentState("B")
                    },
                    onLongPressPointA = { handleLongPress(true) },
                    onLongPressPointB = { handleLongPress(false) },
                    onPressStateA = { onPressStateChange(true, it) },
                    onPressStateB = { onPressStateChange(false, it) },
                    onEndMatch = { activeSheet = ActiveSheet.EndMatchConfirm },
                    onStartTimer = { viewModel.startTimer() },
                    onPauseTimer = { viewModel.pauseTimer() }
                )
            }

            AppScreen.MatchFinished -> {
                MatchFinishedScreen(
                    summary = finishedSummary,
                    isSaved = isSaved,
                    onSave = {
                        saveTapSignal++
                        val saved = viewModel.saveFinishedMatch()
                        val sharePayload = viewModel.buildShareStubText()
                        val summaryToSend = finishedSummary
                        Log.i(
                            WEAR_DATA_LAYER_TAG,
                            "Save tapped saved=$saved hasSummary=${summaryToSend != null}"
                        )
                        if (saved) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            transientMessage = context.getString(R.string.status_saved)
                        } else {
                            transientMessage = context.getString(R.string.status_already_saved)
                        }
                        if (saved && summaryToSend != null) {
                            val setScoresText = buildSetScoresText(state)
                            val pointEventsJson = viewModel.pointEventsJson()
                            uiScope.launch(Dispatchers.IO) {
                                val pendingMessage = createPendingMatchMessage(summaryToSend, setScoresText, pointEventsJson)
                                PendingMatchStore.savePending(context.applicationContext, pendingMessage)
                                when (sendPendingMatchToPhone(
                                    context = context.applicationContext,
                                    pending = pendingMessage,
                                    reason = "save_tap"
                                )) {
                                    PhoneSendResult.Sent -> {
                                        Log.i(WEAR_DATA_LAYER_TAG, "Match sent to phone")
                                    }

                                    PhoneSendResult.NoConnectedPhone -> {
                                        withContext(Dispatchers.Main) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            transientMessage = context.getString(R.string.status_phone_not_connected)
                                        }
                                    }

                                    PhoneSendResult.Failed -> {
                                        withContext(Dispatchers.Main) {
                                            transientMessage = context.getString(R.string.status_saved_send_failed)
                                        }
                                    }
                                }
                            }
                        } else if (saved && summaryToSend == null) {
                            Log.w(WEAR_DATA_LAYER_TAG, "Save succeeded but no finishedSummary available to send")
                        }
                        if (sharePayload.isNotBlank()) {
                            // Share stub prepared for future phone handoff flow.
                        }
                    },
                    onNewMatch = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.startNewMatch()
                        startWorkoutIfPermitted()
                        // Broadcast fresh 0-0 state so phone keeps showing LIVE
                        broadcastCurrentState("")
                        appScreen = AppScreen.Counter
                    },
                    saveTapSignal = saveTapSignal
                )
            }

            AppScreen.Spectator -> {
                val currentSpectatorState = spectatorState
                if (currentSpectatorState != null) {
                    SpectatorScreen(
                        liveState = currentSpectatorState
                    )
                } else {
                    // Match ended while in spectator mode — handled by LaunchedEffect above
                }
            }
        }

        // "Watch Live" floating banner when a match from another scorer is detected
        if (appScreen == AppScreen.Counter &&
            (spectatorUiState.hasActiveLiveMatch || spectatorState != null)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                PlayceButton(
                    text = stringResource(R.string.btn_watch_live),
                    variant = PlayceButtonVariant.Primary,
                    onClick = {
                        appScreen = AppScreen.Spectator
                    }
                )
            }
        }

        if (activeSheet != ActiveSheet.None) {
            val title: String
            val actions: List<SheetAction>
            var message: String? = null

            when (activeSheet) {
                ActiveSheet.PlayerA -> {
                    title = state.playerAName.take(12)
                    actions = listOf(
                        SheetAction(stringResource(R.string.action_undo_short, state.playerAName.take(8))) {
                            if (viewModel.undoLastPointForPlayerA()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                broadcastCurrentState("")
                            }
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(stringResource(R.string.btn_cancel)) { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.PlayerB -> {
                    title = state.playerBName.take(12)
                    actions = listOf(
                        SheetAction(stringResource(R.string.action_undo_short, state.playerBName.take(8))) {
                            if (viewModel.undoLastPointForPlayerB()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                broadcastCurrentState("")
                            }
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(stringResource(R.string.btn_cancel)) { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.Admin -> {
                    title = stringResource(R.string.sheet_admin)
                    actions = listOf(
                        SheetAction(stringResource(R.string.action_reset_game)) {
                            viewModel.resetGame()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            broadcastCurrentState("")
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(stringResource(R.string.action_reset_match)) {
                            viewModel.resetMatch()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            liveBroadcaster.clearLiveScore()
                            transientMessage = context.getString(R.string.status_match_reset)
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(stringResource(R.string.btn_cancel)) { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.EndMatchConfirm -> {
                    title = stringResource(R.string.sheet_end_match_confirm)
                    actions = listOf(
                        // Manual path for ending mid-match; a match won on points navigates automatically.
                        SheetAction(stringResource(R.string.btn_finish)) {
                            uiScope.launch {
                                val healthMetrics = healthServicesManager.endWorkoutAndGetMetrics()
                                viewModel.finishMatch(healthMetrics)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                liveBroadcaster.clearLiveScore()
                                appScreen = AppScreen.MatchFinished
                                activeSheet = ActiveSheet.None
                            }
                        },
                        SheetAction(stringResource(R.string.btn_cancel)) { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.InitialServer -> {
                    title = stringResource(R.string.prompt_who_serves_first)
                    actions = listOf(
                        SheetAction(state.playerAName.take(12)) {
                            viewModel.setInitialServer(true)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            broadcastCurrentState("")
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(state.playerBName.take(12)) {
                            viewModel.setInitialServer(false)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            broadcastCurrentState("")
                            activeSheet = ActiveSheet.None
                        }
                    )
                }

                ActiveSheet.DecidingTiebreak -> {
                    title = stringResource(R.string.title_sets_tied)
                    message = stringResource(R.string.prompt_decide_tiebreak)
                    // Rendered two-per-row: [TB to 7 | TB to 10] with the decline below.
                    actions = listOf(
                        SheetAction(stringResource(R.string.action_tiebreak_to_7)) {
                            viewModel.startDecidingTiebreak(7)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            broadcastCurrentState("")
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(stringResource(R.string.action_super_tiebreak_to_10)) {
                            viewModel.startDecidingTiebreak(10)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            broadcastCurrentState("")
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction(stringResource(R.string.action_continue_normal_set)) {
                            viewModel.declineDecidingTiebreak()
                            activeSheet = ActiveSheet.None
                        }
                    )
                }

                ActiveSheet.None -> {
                    title = ""
                    actions = emptyList()
                }
            }

            BottomActionSheet(
                title = title,
                message = message,
                actions = actions,
                actionsPerRow = if (activeSheet == ActiveSheet.DecidingTiebreak) 2 else 1,
                onDismiss = {
                    when (activeSheet) {
                        // Ignoring the serve question keeps the default (Player A serves).
                        ActiveSheet.InitialServer -> viewModel.dismissInitialServerPrompt()
                        // Swiping away the offer counts as "continue normal set";
                        // undoing the set-closing point re-arms the question.
                        ActiveSheet.DecidingTiebreak -> viewModel.declineDecidingTiebreak()
                        else -> Unit
                    }
                    activeSheet = ActiveSheet.None
                }
            )
        }

        AnimatedVisibility(
            visible = transientMessage != null,
            enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.94f),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.94f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = transientMessage.orEmpty(),
                modifier = Modifier
                    .clip(PlayceWearShapes.Chip)
                    .background(PlayceWearColors.SurfaceElevated)
                    .border(1.dp, PlayceWearColors.Accent.copy(alpha = 0.2f), PlayceWearShapes.Chip)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = PlayceWearColors.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }

        // Versioned "What's New" overlay, shown once after an update changes the controls.
        if (showWhatsNew) {
            WhatsNewOverlay(
                onDismiss = {
                    WhatsNewPrefs.markSeen(context)
                    showWhatsNew = false
                }
            )
        }

        // Ambient mode: simplified, burn-in-friendly scoreboard drawn over
        // whatever screen was active. No touch targets, no seconds.
        if (isAmbient) {
            AmbientMatchScreen(state = state)
        }
    }
}

@Composable
private fun AmbientMatchScreen(state: MatchState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${state.playerAName.take(6).uppercase()} · ${state.playerBName.take(6).uppercase()}",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${state.pointLabelForA()} – ${state.pointLabelForB()}",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.label_sets) + " ${state.playerA.sets}-${state.playerB.sets}" +
                "   " + stringResource(R.string.label_games) + " ${state.playerA.games}-${state.playerB.games}",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        if (state.hasTimerStarted) {
            Spacer(modifier = Modifier.height(10.dp))
            val minutes = state.elapsedSeconds / 60
            Text(
                text = if (minutes >= 60) "%d:%02d".format(minutes / 60, minutes % 60) else "$minutes min",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun WhatsNewOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceWearColors.Scrim)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { /* consume taps behind the overlay */ })
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayceChip(text = stringResource(R.string.whatsnew_title), accent = true)
            Text(
                text = stringResource(R.string.whatsnew_message),
                color = PlayceWearColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            PlayceButton(
                text = stringResource(R.string.whatsnew_button),
                onClick = onDismiss,
                variant = PlayceButtonVariant.Primary
            )
        }
    }
}

@Composable
private fun rememberSpectatorUiState(
    context: Context,
    localNodeId: String
): SpectatorUiState {
    var spectatorObserver by remember { mutableStateOf<LiveScoreObserver?>(null) }
    var hasActiveLiveMatch by remember { mutableStateOf(false) }
    val emptySpectatorState = remember { mutableStateOf<WearLiveMatchState?>(null) }

    LaunchedEffect(context, localNodeId) {
        spectatorObserver?.stopListening()
        spectatorObserver = null
        hasActiveLiveMatch = false

        if (localNodeId.isEmpty()) return@LaunchedEffect

        val observer = LiveScoreObserver(context, localNodeId)
        observer.startListening()
        spectatorObserver = observer

        withContext(Dispatchers.IO) {
            try {
                val dataItems = Tasks.await(
                    Wearable.getDataClient(context)
                        .getDataItems(Uri.parse("wear://*${LiveScoreBroadcaster.LIVE_PATH}"))
                )
                for (item in dataItems) {
                    val dataMap = DataMapItem.fromDataItem(item).dataMap
                    val isActive = dataMap.getBoolean("isMatchActive", false)
                    val scorerNode = dataMap.getString("scorerNodeId", "")
                    if (isActive && scorerNode.isNotEmpty() && scorerNode != localNodeId) {
                        hasActiveLiveMatch = true
                        break
                    }
                }
                dataItems.release()
            } catch (e: Exception) {
                Log.w(WEAR_DATA_LAYER_TAG, "Failed to check existing live DataItem", e)
            }
        }
    }

    DisposableEffect(spectatorObserver) {
        onDispose { spectatorObserver?.stopListening() }
    }

    val spectatorState by (spectatorObserver?.state?.collectAsState() ?: emptySpectatorState)

    return SpectatorUiState(
        hasActiveLiveMatch = hasActiveLiveMatch,
        spectatorState = spectatorState
    )
}

@Composable
private fun CounterScreen(
    state: MatchState,
    onTapPointA: () -> Unit,
    onTapPointB: () -> Unit,
    onLongPressPointA: () -> Unit,
    onLongPressPointB: () -> Unit,
    onPressStateA: (Boolean) -> Unit,
    onPressStateB: (Boolean) -> Unit,
    onEndMatch: () -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val serverName = if (state.currentServerIsPlayerA()) state.playerAName else state.playerBName
    val serverLabel = stringResource(R.string.serve_indicator, serverName.take(6).uppercase())
    val serveIndicatorState = remember(state, serverLabel) {
        ServeIndicatorState(
            serverLabel = serverLabel,
            isPlayerAServing = state.currentServerIsPlayerA(),
            serveOnLeftSide = state.serveStartsOnLeftSide()
        )
    }

    Scaffold(
        modifier = Modifier.background(PlayceWearColors.Background),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ServeSideHalo(serveOnLeftSide = serveIndicatorState.serveOnLeftSide)

            ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayceWearSpacing.Sm),
                state = listState,
                autoCentering = AutoCenteringParams(itemIndex = 1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PlayceWearSpacing.Sm)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        CompactScore(label = stringResource(R.string.label_sets), a = state.playerA.sets, b = state.playerB.sets)
                        CompactScore(label = stringResource(R.string.label_games), a = state.playerA.games, b = state.playerB.games)
                    }
                }

                item {
                    PlayceChip(
                        text = serveIndicatorState.serverLabel,
                        accent = true
                    )
                }

                item {
                    PointsBoard(
                        pointA = state.pointLabelForA(),
                        pointB = state.pointLabelForB()
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AddPointGestureButton(
                            label = "+${state.playerAName.take(1).uppercase()}",
                            onPressStateChange = onPressStateA,
                            onTap = onTapPointA,
                            onLongPress = onLongPressPointA,
                            isServing = serveIndicatorState.isPlayerAServing
                        )
                        AddPointGestureButton(
                            label = "+${state.playerBName.take(1).uppercase()}",
                            onPressStateChange = onPressStateB,
                            onTap = onTapPointB,
                            onLongPress = onLongPressPointB,
                            isServing = !serveIndicatorState.isPlayerAServing
                        )
                    }
                }

                item {
                    TimerFooter(
                        elapsedSeconds = state.elapsedSeconds,
                        isRunning = state.isRunning,
                        hasStarted = state.hasTimerStarted,
                        onStart = onStartTimer,
                        onPause = onPauseTimer
                    )
                }

                item {
                    EndMatchButton(onClick = onEndMatch)
                }
            }
        }
    }
}

@Composable
private fun ServeSideHalo(serveOnLeftSide: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .align(if (serveOnLeftSide) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 2.dp)
                .size(width = 14.dp, height = 116.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(PlayceWearColors.Accent.copy(alpha = 0.18f))
                .border(
                    width = 1.dp,
                    color = PlayceWearColors.Accent.copy(alpha = 0.32f),
                    shape = RoundedCornerShape(999.dp)
                )
        )
    }
}

@Composable
private fun MatchFinishedScreen(
    summary: FinishedMatchSummary?,
    isSaved: Boolean,
    onSave: () -> Unit,
    onNewMatch: () -> Unit,
    saveTapSignal: Int
) {
    val safeSummary = summary
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceWearColors.Background)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (safeSummary == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.title_match_finished),
                    fontWeight = FontWeight.Black,
                    color = PlayceWearColors.TextPrimary
                )
                Text(stringResource(R.string.label_no_summary), color = PlayceWearColors.TextSecondary, fontSize = 11.sp)
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PlayceWearSpacing.Sm)
        ) {
            SyncStatusLabel(saveTapSignal = saveTapSignal)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PlayceWearSpacing.Sm)
            ) {
                PlayceChip(text = stringResource(R.string.title_match_finished), accent = true)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = safeSummary.playerAName.take(8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayceWearColors.Accent
                    )
                    Text(
                        text = safeSummary.setsScore,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = PlayceWearColors.TextPrimary
                    )
                    Text(
                        text = safeSummary.playerBName.take(8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayceWearColors.TextSecondary
                    )
                }
                PlayceCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = true
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = safeSummary.setsDetail,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlayceWearColors.TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.label_duration, formatTime(safeSummary.durationSeconds)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlayceWearColors.TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Text(
                            text = formatTimestamp(safeSummary.createdAt),
                            fontSize = 9.sp,
                            color = PlayceWearColors.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PlayceButton(
                    text = if (isSaved) stringResource(R.string.status_saved) else stringResource(R.string.btn_save_match),
                    onClick = onSave,
                    enabled = !isSaved,
                    variant = PlayceButtonVariant.Primary
                )
                PlayceButton(
                    text = stringResource(R.string.btn_new_match),
                    onClick = onNewMatch,
                    variant = PlayceButtonVariant.Secondary
                )
            }
        }
    }
}


private sealed class SyncStatus {
    data class RetryIn(val seconds: Long) : SyncStatus()
    object Syncing : SyncStatus()
    object Synced : SyncStatus()
}

@Composable
private fun SyncStatusLabel(saveTapSignal: Int) {
    val context = LocalContext.current
    val syncedVisibleWindowMs = 4_000L
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var status by remember { mutableStateOf<SyncStatus?>(null) }
    var lastSaveTapAtMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(saveTapSignal) {
        if (saveTapSignal > 0) {
            lastSaveTapAtMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    LaunchedEffect(nowMillis, lastSaveTapAtMillis) {
        val pending = PendingMatchStore.getPending(context.applicationContext)
        val nextStatus = when {
            pending != null && nowMillis < pending.nextRetryAt -> {
                val remainingSeconds = ((pending.nextRetryAt - nowMillis + 999L) / 1000L).coerceAtLeast(1L)
                SyncStatus.RetryIn(remainingSeconds)
            }
            pending != null -> SyncStatus.Syncing
            lastSaveTapAtMillis > 0L && (nowMillis - lastSaveTapAtMillis) <= syncedVisibleWindowMs ->
                SyncStatus.Synced
            else -> null
        }
        if (status != nextStatus) {
            status = nextStatus
        }
    }
    val currentStatus = status ?: return

    val normalizedLabel = when (currentStatus) {
        is SyncStatus.RetryIn ->
            stringResource(R.string.status_retry_in, currentStatus.seconds).uppercase(Locale.getDefault())
        SyncStatus.Syncing -> stringResource(R.string.status_syncing).uppercase(Locale.getDefault())
        SyncStatus.Synced -> stringResource(R.string.status_sent)
    }
    val isSynced = currentStatus == SyncStatus.Synced

    Text(
        text = normalizedLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = when (currentStatus) {
            SyncStatus.Synced -> PlayceWearColors.Accent
            is SyncStatus.RetryIn -> PlayceWearColors.TextSecondary
            else -> PlayceWearColors.TextPrimary
        },
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(PlayceWearShapes.Chip)
            .background(
                if (isSynced) PlayceWearColors.AccentSoft else PlayceWearColors.Surface
            )
            .border(
                1.dp,
                if (isSynced) PlayceWearColors.Accent.copy(alpha = 0.28f) else PlayceWearColors.Border,
                PlayceWearShapes.Chip
            )
            .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
    )
}


@Composable
private fun EndMatchButton(onClick: () -> Unit) {
    PlayceButton(
        text = stringResource(R.string.btn_end_match),
        onClick = onClick,
        variant = PlayceButtonVariant.Secondary
    )
}

@Composable
private fun CompactScore(label: String, a: Int, b: Int) {
    PlayceCard(accentBorder = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlayceWearColors.TextSecondary
            )
            Text(
                text = "$a - $b",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = PlayceWearColors.TextPrimary
            )
        }
    }
}

@Composable
private fun PointsBoard(pointA: String, pointB: String) {
    PlayceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        accentBorder = true
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pointFontSize = when {
                maxWidth < 146.dp -> 36.sp
                maxWidth < 164.dp -> 42.sp
                maxWidth < 182.dp -> 48.sp
                else -> 56.sp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BigPoint(
                    label = "A",
                    points = pointA,
                    pointFontSize = pointFontSize,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "-",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = PlayceWearColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                BigPoint(
                    label = "B",
                    points = pointB,
                    pointFontSize = pointFontSize,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BigPoint(
    label: String,
    points: String,
    modifier: Modifier = Modifier,
    pointFontSize: TextUnit = 56.sp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlayceChip(text = label, accent = false)
        AnimatedContent(
            targetState = points,
            transitionSpec = {
                (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.88f))
                    .togetherWith(fadeOut(animationSpec = tween(120)))
            },
            label = "PointAnimation$label"
        ) { targetValue ->
            Text(
                text = targetValue,
                fontSize = pointFontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = PlayceWearColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun AddPointGestureButton(
    label: String,
    onPressStateChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    isServing: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (isPressed) PlayceWearColors.AccentPressed else PlayceWearColors.Accent)
            .border(
                width = if (isServing) 3.dp else 1.dp,
                color = if (isServing) {
                    PlayceWearColors.TextPrimary.copy(alpha = 0.9f)
                } else {
                    PlayceWearColors.Accent.copy(alpha = 0.4f)
                },
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressStateChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            isPressed = false
                            onPressStateChange(false)
                        }
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = PlayceWearColors.Background
        )
    }
}

@Composable
private fun TimerFooter(
    elapsedSeconds: Int,
    isRunning: Boolean,
    hasStarted: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit
) {
    PlayceCard(accentBorder = false) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.label_timer),
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlayceWearColors.Accent
            )
            Text(
                text = formatTime(elapsedSeconds),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isRunning) PlayceWearColors.TextPrimary else PlayceWearColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (isRunning) {
                PlayceButton(
                    text = stringResource(R.string.btn_pause),
                    onClick = onPause,
                    variant = PlayceButtonVariant.Secondary
                )
            } else {
                PlayceButton(
                    text = if (hasStarted) stringResource(R.string.btn_resume) else stringResource(R.string.btn_start),
                    onClick = onStart,
                    variant = PlayceButtonVariant.Primary
                )
            }
        }
    }
}

@Composable
private fun BottomActionSheet(
    title: String,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    message: String? = null,
    actionsPerRow: Int = 1
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceWearColors.Scrim)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(PlayceWearShapes.Sheet)
                .background(PlayceWearColors.Surface)
                .border(1.dp, PlayceWearColors.Border, PlayceWearShapes.Sheet)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlayceChip(text = title, accent = true)

            if (message != null) {
                Text(
                    text = message,
                    color = PlayceWearColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            actions.chunked(actionsPerRow).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowActions.forEach { action ->
                        PlayceButton(
                            text = action.label,
                            onClick = action.onClick,
                            modifier = Modifier.weight(1f),
                            variant = when {
                                action.label.contains("END MATCH", ignoreCase = true) -> PlayceButtonVariant.Danger
                                else -> PlayceButtonVariant.Secondary
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun formatTimestamp(epochMillis: Long): String {
    return SimpleDateFormat("HH:mm  dd/MM", Locale.getDefault()).format(Date(epochMillis))
}

private fun buildFinalScoreText(summary: FinishedMatchSummary): String {
    // Keep payload consistent with the prominent final result shown in end screen.
    return summary.setsScore
}

private fun createPendingMatchMessage(
    summary: FinishedMatchSummary,
    setScoresText: String?,
    pointEventsJson: String? = null
): PendingMatchMessage {
    val idempotencyKey = UUID.randomUUID().toString()
    val payload = buildMatchFinishedPayload(summary, setScoresText, idempotencyKey, pointEventsJson)
    val now = System.currentTimeMillis()
    return PendingMatchMessage(
        idempotencyKey = idempotencyKey,
        payload = payload,
        createdAtMillis = now,
        attemptCount = 0,
        nextRetryAtMillis = now,
        targetNodeId = null
    )
}

private fun buildMatchFinishedPayload(
    summary: FinishedMatchSummary,
    setScoresText: String?,
    idempotencyKey: String,
    pointEventsJson: String? = null
): ByteArray {
    val createdAt = if (summary.createdAt > 0L) summary.createdAt else System.currentTimeMillis()
    val finalScoreText = buildFinalScoreText(summary)
    val dataMap = DataMap().apply {
        putLong("createdAt", createdAt)
        putLong("durationSeconds", summary.durationSeconds.toLong())
        putString("finalScoreText", finalScoreText)
        putString("playerAName", summary.playerAName)
        putString("playerBName", summary.playerBName)
        if (!setScoresText.isNullOrBlank()) {
            putString("setScoresText", setScoresText)
        }
        summary.caloriesKcal?.let { putDouble("caloriesKcal", it) }
        summary.avgHeartRateBpm?.let { putInt("avgHeartRateBpm", it) }
        summary.maxHeartRateBpm?.let { putInt("maxHeartRateBpm", it) }
        putString("idempotencyKey", idempotencyKey)
        if (!pointEventsJson.isNullOrBlank()) {
            putString("pointEventsJson", pointEventsJson)
        }
    }
    return dataMap.toByteArray()
}

private fun sendPendingMatchToPhone(
    context: Context,
    pending: PendingMatchMessage,
    reason: String
): PhoneSendResult {
    Log.i(
        WEAR_DATA_LAYER_TAG,
        "Sending pending /match_finished reason=$reason idempotencyKey=${pending.idempotencyKey} attempt=${pending.attemptCount} nextRetryAt=${pending.nextRetryAtMillis} targetNodeId=${pending.targetNodeId.orEmpty()} payloadSize=${pending.payload.size}"
    )

    return try {
        val connectedNodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        Log.i(
            WEAR_DATA_LAYER_TAG,
            "connectedNodes count=${connectedNodes.size} ids=${connectedNodes.joinToString { it.id }}"
        )
        if (connectedNodes.size > 1) {
            Log.w(WEAR_DATA_LAYER_TAG, "Multiple connected nodes detected for idempotencyKey=${pending.idempotencyKey}")
        }
        var node = connectedNodes.firstOrNull { it.id == pending.targetNodeId }
        if (pending.targetNodeId != null && node == null) {
            Log.w(
                WEAR_DATA_LAYER_TAG,
                "Configured targetNodeId=${pending.targetNodeId} not found for idempotencyKey=${pending.idempotencyKey}. Falling back."
            )
        }
        if (node == null) {
            node = connectedNodes.firstOrNull()
        }
        if (node == null) {
            val nextRetryAt = System.currentTimeMillis() + computeRetryDelayMillis(pending.attemptCount)
            PendingMatchStore.updateAfterAttempt(
                context = context,
                idempotencyKey = pending.idempotencyKey,
                attemptCount = pending.attemptCount + 1,
                nextRetryAtMillis = nextRetryAt
            )
            Log.w(WEAR_DATA_LAYER_TAG, "No connected phone node for /match_finished idempotencyKey=${pending.idempotencyKey}")
            return PhoneSendResult.NoConnectedPhone
        }
        if (connectedNodes.size == 1 && pending.targetNodeId != node.id) {
            PendingMatchStore.updateTargetNodeId(context, pending.idempotencyKey, node.id)
            Log.i(WEAR_DATA_LAYER_TAG, "Pinned targetNodeId=${node.id} for idempotencyKey=${pending.idempotencyKey}")
        }
        Log.i(
            WEAR_DATA_LAYER_TAG,
            "Sending /match_finished to nodeId=${node.id} displayName=${node.displayName} isNearby=${node.isNearby} idempotencyKey=${pending.idempotencyKey}"
        )
        Tasks.await(
            Wearable.getMessageClient(context).sendMessage(
                node.id,
                "/match_finished",
                pending.payload
            )
        )
        PendingMatchStore.updateAfterAttempt(
            context = context,
            idempotencyKey = pending.idempotencyKey,
            attemptCount = pending.attemptCount + 1,
            nextRetryAtMillis = System.currentTimeMillis() + ACK_WAIT_RETRY_MS
        )
        Log.i(WEAR_DATA_LAYER_TAG, "Sent /match_finished to node=${node.id} idempotencyKey=${pending.idempotencyKey}")
        PhoneSendResult.Sent
    } catch (t: Throwable) {
        val nextRetryAt = System.currentTimeMillis() + computeRetryDelayMillis(pending.attemptCount)
        PendingMatchStore.updateAfterAttempt(
            context = context,
            idempotencyKey = pending.idempotencyKey,
            attemptCount = pending.attemptCount + 1,
            nextRetryAtMillis = nextRetryAt
        )
        Log.e(WEAR_DATA_LAYER_TAG, "Failed sending /match_finished idempotencyKey=${pending.idempotencyKey}", t)
        PhoneSendResult.Failed
    }
}

private fun computeRetryDelayMillis(attemptCount: Int): Long {
    val cappedAttempt = attemptCount.coerceIn(0, 4)
    return 5_000L * (1L shl cappedAttempt)
}

private val retryCoordinatorLock = Any()
private var lastRetryTriggerElapsedRealtime: Long = 0L

private fun triggerPendingRetry(context: Context, reason: String) {
    val nowElapsedRealtime = SystemClock.elapsedRealtime()
    synchronized(retryCoordinatorLock) {
        if (nowElapsedRealtime - lastRetryTriggerElapsedRealtime < RETRY_TRIGGER_THROTTLE_MS) {
            Log.d(WEAR_DATA_LAYER_TAG, "Retry trigger throttled reason=$reason")
            return
        }
        lastRetryTriggerElapsedRealtime = nowElapsedRealtime
    }

    retryScope.launch {
        val pending = PendingMatchStore.readPending(context) ?: return@launch
        val now = System.currentTimeMillis()
        if (now < pending.nextRetryAtMillis) {
            Log.d(
                WEAR_DATA_LAYER_TAG,
                "Retry skipped (not due yet) reason=$reason idempotencyKey=${pending.idempotencyKey} nextRetryAt=${pending.nextRetryAtMillis}"
            )
            return@launch
        }
        val result = sendPendingMatchToPhone(context, pending, reason = reason)
        Log.i(WEAR_DATA_LAYER_TAG, "Retry executed reason=$reason idempotencyKey=${pending.idempotencyKey} result=$result")
    }
}

private fun buildSetScoresText(matchState: MatchState): String? {
    val completedSetsText = matchState.completedSets.joinToString(" ") { "${it.a}-${it.b}" }
    return completedSetsText.ifBlank { null }
}


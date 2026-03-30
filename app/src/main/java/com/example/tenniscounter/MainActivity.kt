package com.example.tenniscounter

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val WEAR_DATA_LAYER_TAG = "WearDataLayer"
private const val ACK_WAIT_RETRY_MS = 10_000L
private const val RETRY_TRIGGER_THROTTLE_MS = 1_500L
private val retryScope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
    EndMatchConfirm
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

/**
 * Represents a physical button press event from the watch hardware.
 * STEM_1 = top button → Player A point
 * STEM_2 = bottom button → Player B point
 *
 * Only active when [hardwareButtonsEnabled] is true (user opt-in).
 * Does NOT intercept KEYCODE_HOME or KEYCODE_BACK — those always work normally.
 */
enum class HardwareButtonEvent {
    STEM_1_PLAYER_A,
    STEM_2_PLAYER_B
}

class MainActivity : ComponentActivity() {

    /** Emits hardware button presses so composables can react. */
    private val _hardwareButtonEvent = MutableStateFlow<HardwareButtonEvent?>(null)

    /**
     * Opt-in flag: hardware buttons only score points when this is true.
     * Controlled from the UI via the Admin sheet toggle.
     */
    var hardwareButtonsEnabled: Boolean = false

    /** True when the screen is interactive (not ambient/off). */
    private var isScreenInteractive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TennisWearTheme {
                TennisCounterApp(
                    hardwareButtonFlow = _hardwareButtonEvent,
                    onHardwareButtonsToggled = { enabled -> hardwareButtonsEnabled = enabled }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        triggerPendingRetry(applicationContext, "onStart")
        isScreenInteractive = true
    }

    override fun onResume() {
        super.onResume()
        triggerPendingRetry(applicationContext, "onResume")
        isScreenInteractive = true
    }

    override fun onPause() {
        super.onPause()
        // Screen off / ambient mode → disable button scoring to prevent accidental presses
        isScreenInteractive = false
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Only intercept STEM buttons, never HOME or BACK
        if (!hardwareButtonsEnabled || !isScreenInteractive) {
            return super.onKeyDown(keyCode, event)
        }

        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_STEM_1 -> {
                _hardwareButtonEvent.value = HardwareButtonEvent.STEM_1_PLAYER_A
                true
            }
            android.view.KeyEvent.KEYCODE_STEM_2 -> {
                _hardwareButtonEvent.value = HardwareButtonEvent.STEM_2_PLAYER_B
                true
            }
            // KEYCODE_HOME and KEYCODE_BACK always pass through to the system
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@Composable
private fun TennisCounterApp(
    viewModel: TennisViewModel = viewModel(),
    hardwareButtonFlow: MutableStateFlow<HardwareButtonEvent?> = MutableStateFlow(null),
    onHardwareButtonsToggled: (Boolean) -> Unit = {}
) {
    val state by viewModel.matchState.collectAsState()
    val finishedSummary by viewModel.finishedMatch.collectAsState()
    val isSaved by viewModel.isFinishedMatchSaved.collectAsState()
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val pointSound = remember { PointSoundManager() }
    val liveBroadcaster = remember { LiveScoreBroadcaster(context) }
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
    var hwButtonsEnabled by remember { mutableStateOf(false) }

    // Handle physical watch button presses (STEM_1 = Player A, STEM_2 = Player B)
    // Only processes when: opt-in enabled, match running, screen on, no sheet open, on Counter screen
    val hardwareButtonEvent by hardwareButtonFlow.collectAsState()
    LaunchedEffect(hardwareButtonEvent) {
        val event = hardwareButtonEvent ?: return@LaunchedEffect
        val canScore = hwButtonsEnabled &&
            !state.isMatchOver &&
            appScreen == AppScreen.Counter &&
            activeSheet == ActiveSheet.None
        if (canScore) {
            when (event) {
                HardwareButtonEvent.STEM_1_PLAYER_A -> {
                    viewModel.addPointToPlayerA()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    pointSound.playPlayerASound()
                    broadcastCurrentState("A")
                }
                HardwareButtonEvent.STEM_2_PLAYER_B -> {
                    viewModel.addPointToPlayerB()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    pointSound.playPlayerBSound()
                    broadcastCurrentState("B")
                }
            }
        }
        hardwareButtonFlow.value = null // consume the event
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
                            transientMessage = "Saved OK"
                        } else {
                            transientMessage = "Already saved"
                        }
                        if (saved && summaryToSend != null) {
                            val setScoresText = buildSetScoresText(state)
                            uiScope.launch(Dispatchers.IO) {
                                val pendingMessage = createPendingMatchMessage(summaryToSend, setScoresText)
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
                                            transientMessage = "Phone not connected"
                                        }
                                    }

                                    PhoneSendResult.Failed -> {
                                        withContext(Dispatchers.Main) {
                                            transientMessage = "Saved OK, send failed"
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
                    text = "Watch Live",
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

            when (activeSheet) {
                ActiveSheet.PlayerA -> {
                    title = state.playerAName.take(12)
                    actions = listOf(
                        SheetAction("Undo (${state.playerAName.take(8)})") {
                            if (viewModel.undoLastPointForPlayerA()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                broadcastCurrentState("")
                            }
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Cancel") { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.PlayerB -> {
                    title = state.playerBName.take(12)
                    actions = listOf(
                        SheetAction("Undo (${state.playerBName.take(8)})") {
                            if (viewModel.undoLastPointForPlayerB()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                broadcastCurrentState("")
                            }
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Cancel") { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.Admin -> {
                    title = "Admin"
                    val buttonToggleLabel = if (hwButtonsEnabled)
                        "Buttons: ON (tap to disable)"
                    else
                        "Buttons: OFF (tap to enable)"
                    actions = listOf(
                        SheetAction(buttonToggleLabel) {
                            hwButtonsEnabled = !hwButtonsEnabled
                            onHardwareButtonsToggled(hwButtonsEnabled)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            transientMessage = if (hwButtonsEnabled)
                                "HW buttons ON: Top=+A, Bottom=+B"
                            else
                                "HW buttons OFF"
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Reset current game") {
                            viewModel.resetGame()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            broadcastCurrentState("")
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Reset match") {
                            viewModel.resetMatch()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            liveBroadcaster.clearLiveScore()
                            transientMessage = "Match reset"
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Cancel") { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.EndMatchConfirm -> {
                    title = "End match?"
                    actions = listOf(
                        // Navigation to final screen is manual and only happens after explicit Finish confirmation.
                        SheetAction("Finish") {
                            viewModel.finishMatch()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            liveBroadcaster.clearLiveScore()
                            // Auto-disable hardware buttons when match ends
                            hwButtonsEnabled = false
                            onHardwareButtonsToggled(false)
                            appScreen = AppScreen.MatchFinished
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Cancel") { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.None -> {
                    title = ""
                    actions = emptyList()
                }
            }

            BottomActionSheet(
                title = title,
                actions = actions,
                onDismiss = { activeSheet = ActiveSheet.None }
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
    val serveIndicatorState = remember(state) {
        ServeIndicatorState(
            serverLabel = if (state.currentServerIsPlayerA()) "${state.playerAName.take(6).uppercase()} SERVES" else "${state.playerBName.take(6).uppercase()} SERVES",
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
                        CompactScore(label = "SETS", a = state.playerA.sets, b = state.playerB.sets)
                        CompactScore(label = "GAMES", a = state.playerA.games, b = state.playerB.games)
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
                    "MATCH FINISHED",
                    fontWeight = FontWeight.Black,
                    color = PlayceWearColors.TextPrimary
                )
                Text("No summary", color = PlayceWearColors.TextSecondary, fontSize = 11.sp)
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
                PlayceChip(text = "MATCH FINISHED", accent = true)
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
                            text = "Duration ${formatTime(safeSummary.durationSeconds)}",
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
                    text = if (isSaved) "Saved OK" else "SAVE MATCH",
                    onClick = onSave,
                    enabled = !isSaved,
                    variant = PlayceButtonVariant.Primary
                )
                PlayceButton(
                    text = "NEW MATCH",
                    onClick = onNewMatch,
                    variant = PlayceButtonVariant.Secondary
                )
            }
        }
    }
}


@Composable
private fun SyncStatusLabel(saveTapSignal: Int) {
    val context = LocalContext.current
    val syncedVisibleWindowMs = 4_000L
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var statusText by remember { mutableStateOf<String?>(null) }
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
                "Retry in ${remainingSeconds}s"
            }
            pending != null -> "Syncing..."
            lastSaveTapAtMillis > 0L && (nowMillis - lastSaveTapAtMillis) <= syncedVisibleWindowMs ->
                "Synced \u2713"
            else -> null
        }
        if (statusText != nextStatus) {
            statusText = nextStatus
        }
    }
    val label = statusText ?: return

    val normalizedLabel = when {
        label.startsWith("Retry in", ignoreCase = true) -> label.uppercase(Locale.getDefault())
        label.startsWith("Retry", ignoreCase = true) -> "RETRY"
        label.startsWith("Syncing", ignoreCase = true) -> "SYNCING..."
        label.startsWith("Synced", ignoreCase = true) -> "SENT"
        else -> label.uppercase(Locale.getDefault())
    }

    Text(
        text = normalizedLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = when {
            label.startsWith("Synced") -> PlayceWearColors.Accent
            label.startsWith("Retry") -> PlayceWearColors.TextSecondary
            else -> PlayceWearColors.TextPrimary
        },
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(PlayceWearShapes.Chip)
            .background(
                if (label.startsWith("Synced")) PlayceWearColors.AccentSoft else PlayceWearColors.Surface
            )
            .border(
                1.dp,
                if (label.startsWith("Synced")) PlayceWearColors.Accent.copy(alpha = 0.28f) else PlayceWearColors.Border,
                PlayceWearShapes.Chip
            )
            .padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)
    )
}


@Composable
private fun EndMatchButton(onClick: () -> Unit) {
    PlayceButton(
        text = "END MATCH",
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
                text = "TIMER",
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
                    text = "PAUSE",
                    onClick = onPause,
                    variant = PlayceButtonVariant.Secondary
                )
            } else {
                PlayceButton(
                    text = if (hasStarted) "RESUME" else "START",
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
    onDismiss: () -> Unit
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

            actions.forEach { action ->
                PlayceButton(
                    text = action.label,
                    onClick = action.onClick,
                    variant = when {
                        action.label.contains("END MATCH", ignoreCase = true) -> PlayceButtonVariant.Danger
                        else -> PlayceButtonVariant.Secondary
                    }
                )
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
    setScoresText: String?
): PendingMatchMessage {
    val idempotencyKey = UUID.randomUUID().toString()
    val payload = buildMatchFinishedPayload(summary, setScoresText, idempotencyKey)
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
    idempotencyKey: String
): ByteArray {
    val createdAt = if (summary.createdAt > 0L) summary.createdAt else System.currentTimeMillis()
    val finalScoreText = buildFinalScoreText(summary)
    val dataMap = DataMap().apply {
        putLong("createdAt", createdAt)
        putLong("durationSeconds", summary.durationSeconds.toLong())
        putString("finalScoreText", finalScoreText)
        if (!setScoresText.isNullOrBlank()) {
            putString("setScoresText", setScoresText)
        }
        putString("idempotencyKey", idempotencyKey)
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


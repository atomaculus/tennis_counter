package com.example.tenniscounter

import android.content.Context
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.Wearable
import com.example.tenniscounter.ui.FinishedMatchSummary
import com.example.tenniscounter.ui.MatchState
import com.example.tenniscounter.ui.TennisViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CourtGreen = Color(0xFF0F3415)
private val CourtGreenDark = Color(0xFF0A250F)
private val WhiteStrong = Color(0xFFF8FFF8)
private val WhiteSoft = Color(0xFFD9E7D9)
private val ScrimBlack = Color(0xAA000000)
private const val WEAR_DATA_LAYER_TAG = "WearDataLayer"

private enum class AppScreen {
    Counter,
    MatchFinished
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

data class SheetAction(
    val label: String,
    val onClick: () -> Unit
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TennisWearTheme {
                TennisCounterApp()
            }
        }
    }
}

@Composable
fun TennisWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = WhiteStrong,
            secondary = Color.White,
            background = CourtGreenDark,
            onBackground = WhiteStrong,
            onPrimary = CourtGreenDark
        ),
        content = content
    )
}

@Composable
private fun TennisCounterApp(viewModel: TennisViewModel = viewModel()) {
    val state by viewModel.matchState.collectAsState()
    val finishedSummary by viewModel.finishedMatch.collectAsState()
    val isSaved by viewModel.isFinishedMatchSaved.collectAsState()
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var appScreen by remember { mutableStateOf(AppScreen.Counter) }
    var activeSheet by remember { mutableStateOf(ActiveSheet.None) }
    var transientMessage by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(
        appScreen,
        state.playerA.points,
        state.playerA.games,
        state.playerA.sets,
        state.playerB.points,
        state.playerB.games,
        state.playerB.sets,
        state.elapsedSeconds,
        state.isRunning
    ) {
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
                        viewModel.addPointToPlayerA()
                    },
                    onTapPointB = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.addPointToPlayerB()
                    },
                    onLongPressPointA = { handleLongPress(true) },
                    onLongPressPointB = { handleLongPress(false) },
                    onPressStateA = { onPressStateChange(true, it) },
                    onPressStateB = { onPressStateChange(false, it) },
                    onEndMatch = { activeSheet = ActiveSheet.EndMatchConfirm }
                )
            }

            AppScreen.MatchFinished -> {
                MatchFinishedScreen(
                    summary = finishedSummary,
                    isSaved = isSaved,
                    onSave = {
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
                                when (sendMatchFinishedToPhone(context.applicationContext, summaryToSend, setScoresText)) {
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
                        appScreen = AppScreen.Counter
                    }
                )
            }
        }

        if (activeSheet != ActiveSheet.None) {
            val title: String
            val actions: List<SheetAction>

            when (activeSheet) {
                ActiveSheet.PlayerA -> {
                    title = "Player A"
                    actions = listOf(
                        SheetAction("Undo last point (Player A)") {
                            if (viewModel.undoLastPointForPlayerA()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Cancel") { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.PlayerB -> {
                    title = "Player B"
                    actions = listOf(
                        SheetAction("Undo last point (Player B)") {
                            if (viewModel.undoLastPointForPlayerB()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Cancel") { activeSheet = ActiveSheet.None }
                    )
                }

                ActiveSheet.Admin -> {
                    title = "Admin"
                    actions = listOf(
                        SheetAction("Reset current game") {
                            viewModel.resetGame()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeSheet = ActiveSheet.None
                        },
                        SheetAction("Reset match") {
                            viewModel.resetMatch()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(CourtGreen)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = WhiteStrong,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
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
    onEndMatch: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        modifier = Modifier.background(CourtGreenDark),
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 1),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                PointsBoard(
                    pointA = state.pointLabelForA(),
                    pointB = state.pointLabelForB()
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AddPointGestureButton(
                        label = "+A",
                        onPressStateChange = onPressStateA,
                        onTap = onTapPointA,
                        onLongPress = onLongPressPointA
                    )
                    AddPointGestureButton(
                        label = "+B",
                        onPressStateChange = onPressStateB,
                        onTap = onTapPointB,
                        onLongPress = onLongPressPointB
                    )
                }
            }

            item {
                TimerFooter(elapsedSeconds = state.elapsedSeconds)
            }

            item {
                EndMatchButton(onClick = onEndMatch)
            }
        }
    }
}

@Composable
private fun MatchFinishedScreen(
    summary: FinishedMatchSummary?,
    isSaved: Boolean,
    onSave: () -> Unit,
    onNewMatch: () -> Unit
) {
    val safeSummary = summary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CourtGreenDark)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        if (safeSummary == null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MATCH FINISHED", fontWeight = FontWeight.Black, color = WhiteStrong)
                Text("No summary", color = WhiteSoft, fontSize = 11.sp)
            }
            return@Box
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MATCH FINISHED",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = WhiteSoft
                )
                Text(
                    text = safeSummary.setsScore,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = WhiteStrong
                )
                Text(
                    text = safeSummary.setsDetail,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WhiteSoft,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Duration ${formatTime(safeSummary.durationSeconds)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhiteStrong,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = formatTimestamp(safeSummary.createdAt),
                    fontSize = 9.sp,
                    color = WhiteSoft,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onSave,
                    enabled = !isSaved,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WhiteStrong,
                        contentColor = CourtGreenDark
                    )
                ) {
                    Text(
                        text = if (isSaved) "Saved OK" else "SAVE MATCH",
                        fontWeight = FontWeight.Black,
                        color = CourtGreenDark
                    )
                }
                Button(
                    onClick = onNewMatch,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = CourtGreen,
                        contentColor = WhiteStrong
                    )
                ) {
                    Text("NEW MATCH", fontWeight = FontWeight.Black, color = WhiteStrong)
                }
            }
        }
    }
}

@Composable
private fun EndMatchButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = WhiteStrong.copy(alpha = 0.18f),
            contentColor = WhiteStrong
        )
    ) {
        Text(
            text = "END MATCH",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = WhiteStrong
        )
    }
}

@Composable
private fun CompactScore(label: String, a: Int, b: Int) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CourtGreen)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WhiteSoft
        )
        Text(
            text = "$a - $b",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = WhiteStrong
        )
    }
}

@Composable
private fun PointsBoard(pointA: String, pointB: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BigPoint(label = "A", points = pointA)
        Text(
            text = "-",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = WhiteSoft
        )
        BigPoint(label = "B", points = pointB)
    }
}

@Composable
private fun BigPoint(label: String, points: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WhiteSoft
        )
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
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = WhiteStrong
            )
        }
    }
}

@Composable
private fun AddPointGestureButton(
    label: String,
    onPressStateChange: (Boolean) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(WhiteStrong)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressStateChange(true)
                        try {
                            tryAwaitRelease()
                        } finally {
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
            color = CourtGreenDark
        )
    }
}

@Composable
private fun TimerFooter(elapsedSeconds: Int) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CourtGreen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TIMER",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = WhiteSoft
        )
        Text(
            text = formatTime(elapsedSeconds),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = WhiteStrong
        )
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
            .background(ScrimBlack)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(CourtGreen)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WhiteSoft
            )

            actions.forEach { action ->
                Button(
                    onClick = action.onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = WhiteStrong,
                        contentColor = CourtGreenDark
                    )
                ) {
                    Text(
                        text = action.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CourtGreenDark
                    )
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

private fun sendMatchFinishedToPhone(
    context: Context,
    summary: FinishedMatchSummary,
    setScoresText: String?
): PhoneSendResult {
    val idempotencyKey = UUID.randomUUID().toString()
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
    val payload = dataMap.toByteArray()
    Log.i(
        WEAR_DATA_LAYER_TAG,
        "Preparing /match_finished finalScoreText=$finalScoreText setScoresText=${setScoresText.orEmpty()} durationSeconds=${summary.durationSeconds} createdAt=$createdAt idempotencyKey=$idempotencyKey payloadSize=${payload.size}"
    )

    return try {
        val connectedNodes = Tasks.await(Wearable.getNodeClient(context).connectedNodes)
        Log.i(
            WEAR_DATA_LAYER_TAG,
            "connectedNodes count=${connectedNodes.size} ids=${connectedNodes.joinToString { it.id }}"
        )
        val node = connectedNodes.firstOrNull()
        if (node == null) {
            Log.w(WEAR_DATA_LAYER_TAG, "No connected phone node for /match_finished")
            return PhoneSendResult.NoConnectedPhone
        }
        Log.i(
            WEAR_DATA_LAYER_TAG,
            "Sending /match_finished to nodeId=${node.id} displayName=${node.displayName} isNearby=${node.isNearby}"
        )
        Tasks.await(
            Wearable.getMessageClient(context).sendMessage(
                node.id,
                "/match_finished",
                payload
            )
        )
        Log.i(WEAR_DATA_LAYER_TAG, "Sent /match_finished to node=${node.id} idempotencyKey=$idempotencyKey")
        PhoneSendResult.Sent
    } catch (t: Throwable) {
        Log.e(WEAR_DATA_LAYER_TAG, "Failed sending /match_finished", t)
        PhoneSendResult.Failed
    }
}

private fun buildSetScoresText(matchState: MatchState): String? {
    val completedSetsText = matchState.completedSets.joinToString(" ") { "${it.a}-${it.b}" }
    return completedSetsText.ifBlank { null }
}

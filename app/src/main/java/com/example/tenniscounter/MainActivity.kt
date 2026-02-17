package com.example.tenniscounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import com.example.tenniscounter.ui.TennisViewModel

private val CourtGreen = Color(0xFF0F3415)
private val CourtGreenDark = Color(0xFF0A250F)
private val WhiteStrong = Color(0xFFF8FFF8)
private val WhiteSoft = Color(0xFFD9E7D9)

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
    val haptic = LocalHapticFeedback.current
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
                    AddPointButton(
                        label = "+A",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.addPointToPlayerA()
                        }
                    )
                    AddPointButton(
                        label = "+B",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.addPointToPlayerB()
                        }
                    )
                }
            }

            item {
                TimerFooter(elapsedSeconds = state.elapsedSeconds)
            }
        }
    }
}

@Composable
fun CompactScore(label: String, a: Int, b: Int) {
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
fun PointsBoard(pointA: String, pointB: String) {
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
fun BigPoint(label: String, points: String) {
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
fun AddPointButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = WhiteStrong,
            contentColor = CourtGreenDark
        )
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
fun TimerFooter(elapsedSeconds: Int) {
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

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

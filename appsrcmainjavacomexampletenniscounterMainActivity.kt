package com.example.tenniscounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tenniscounter.ui.TennisViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                TennisCounterApp()
            }
        }
    }
}

@Composable
private fun TennisCounterApp(viewModel: TennisViewModel = viewModel()) {
    val state by viewModel.matchState.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            viewModel.tickClock()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(text = "Tiempo ${formatTime(state.elapsedSeconds)}")

        PlayerPanel(
            name = "Jugador A",
            points = state.pointLabelForA(),
            games = state.playerA.games,
            sets = state.playerA.sets,
            onAdd = viewModel::addPointToPlayerA,
            onSubtract = viewModel::removePointFromPlayerA
        )

        PlayerPanel(
            name = "Jugador B",
            points = state.pointLabelForB(),
            games = state.playerB.games,
            sets = state.playerB.sets,
            onAdd = viewModel::addPointToPlayerB,
            onSubtract = viewModel::removePointFromPlayerB
        )
    }
}

@Composable
private fun PlayerPanel(
    name: String,
    points: String,
    games: Int,
    sets: Int,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = name)
        Text(text = "Puntos: $points | Games: $games | Sets: $sets")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onSubtract) { Text("-") }
            Button(onClick = onAdd) { Text("+") }
        }
    }
}

private fun formatTime(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

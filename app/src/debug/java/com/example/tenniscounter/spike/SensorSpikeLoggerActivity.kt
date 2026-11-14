package com.example.tenniscounter.spike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text

/**
 * Dev-only entry point for the N1 sensor spike. Not in the launcher; open with:
 *
 * adb shell am start -n com.playce.tenniscounter.app/com.example.tenniscounter.spike.SensorSpikeLoggerActivity
 *
 * Pull the CSVs with:
 *
 * adb pull /sdcard/Android/data/com.playce.tenniscounter.app/files/sensor_spike .
 */
class SensorSpikeLoggerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by SensorSpikeLoggerService.state.collectAsState()
            val context = LocalContext.current

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (state.isLogging) {
                        "REC ${state.sampleCount / 1000}k · ${state.markerCount} marks"
                    } else {
                        state.fileName?.let { "Saved $it" } ?: "Sensor spike"
                    },
                    color = if (state.isLogging) Color(0xFFB8FF2B) else Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        SensorSpikeLoggerService.send(
                            context,
                            if (state.isLogging) SensorSpikeLoggerService.ACTION_STOP
                            else SensorSpikeLoggerService.ACTION_START
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (state.isLogging) Color(0xFFCC3333) else Color(0xFF2B6B2B)
                    )
                ) {
                    Text(
                        text = if (state.isLogging) "STOP" else "START",
                        fontWeight = FontWeight.Black
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { SensorSpikeLoggerService.send(context, SensorSpikeLoggerService.ACTION_MARK) },
                    enabled = state.isLogging,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF444444))
                ) {
                    Text(text = "MARK", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

package com.example.tenniscounter.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.garmin.GarminSdkState
import com.example.tenniscounter.mobile.ui.theme.PlayceColors

/**
 * Discreet status chip showing the Garmin Connect IQ connection state.
 * Hidden entirely when the SDK reports nothing relevant (uninitialized at startup).
 */
@Composable
fun GarminConnectionBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val manager = remember(context) {
        MobileServiceLocator.garminConnectivityManager(context.applicationContext)
    }
    val state = manager.connectionState.collectAsStateWithLifecycle().value

    if (state.sdkState == GarminSdkState.UNINITIALIZED ||
        state.sdkState == GarminSdkState.INITIALIZING) {
        return
    }

    val (label, dotColor) = when (state.sdkState) {
        GarminSdkState.READY -> {
            val connected = state.connectedDeviceCount
            val known = state.knownDevices.size
            when {
                connected > 0 -> "Garmin: connected ($connected)" to PlayceColors.Accent
                known > 0 -> "Garmin: paired, offline" to PlayceColors.TextSecondary
                else -> "Garmin: no device paired" to PlayceColors.TextSecondary
            }
        }
        GarminSdkState.NOT_INSTALLED -> "Garmin: install Connect Mobile" to PlayceColors.Danger
        GarminSdkState.SERVICE_NOT_FOUND -> "Garmin: service unavailable" to PlayceColors.Danger
        GarminSdkState.ERROR -> "Garmin: error" to PlayceColors.Danger
        else -> return
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PlayceColors.SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = PlayceColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


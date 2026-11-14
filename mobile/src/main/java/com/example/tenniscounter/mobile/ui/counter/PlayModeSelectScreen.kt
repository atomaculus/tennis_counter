package com.example.tenniscounter.mobile.ui.counter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.ui.components.PlayceWordmark
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle

/**
 * Entry point for the "Play" tab when there's no session already in
 * progress: pick a modality before landing on its screen. "Partido" is the
 * visually highlighted default (solid, primary style); the other two are
 * outlined.
 */
@Composable
fun PlayModeSelectScreen(
    onSelectMatch: () -> Unit,
    onSelectParedon: () -> Unit,
    onSelectVeintiuno: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlayceWordmark()
            PrimaryButton(
                text = stringResource(R.string.mode_match),
                onClick = onSelectMatch,
                modifier = Modifier.fillMaxWidth(),
                style = PrimaryButtonStyle.Solid
            )
            PrimaryButton(
                text = stringResource(R.string.mode_paredon),
                onClick = onSelectParedon,
                modifier = Modifier.fillMaxWidth(),
                style = PrimaryButtonStyle.Outline
            )
            PrimaryButton(
                text = stringResource(R.string.mode_veintiuno),
                onClick = onSelectVeintiuno,
                modifier = Modifier.fillMaxWidth(),
                style = PrimaryButtonStyle.Outline
            )
        }
    }
}

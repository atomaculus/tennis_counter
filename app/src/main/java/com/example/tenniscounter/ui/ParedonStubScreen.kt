package com.example.tenniscounter.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.example.tenniscounter.R
import com.example.tenniscounter.ui.components.PlayceButton
import com.example.tenniscounter.ui.components.PlayceButtonVariant
import com.example.tenniscounter.ui.components.PlayceChip
import com.example.tenniscounter.ui.components.PlayceWearColors
import com.example.tenniscounter.ui.components.PlayceWearSpacing

/** Placeholder for the future paredón mode. No scroll, per project rule. */
@Composable
fun ParedonStubScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceWearColors.Background)
            .padding(horizontal = PlayceWearSpacing.Lg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PlayceWearSpacing.Md)
        ) {
            PlayceChip(text = stringResource(R.string.paredon_stub_title), accent = true)
            Text(
                text = stringResource(R.string.paredon_stub_message),
                color = PlayceWearColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            PlayceButton(
                text = stringResource(R.string.btn_back),
                onClick = onBack,
                variant = PlayceButtonVariant.Secondary
            )
        }
    }
}

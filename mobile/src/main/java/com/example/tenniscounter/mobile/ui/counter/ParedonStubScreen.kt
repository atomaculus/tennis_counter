package com.example.tenniscounter.mobile.ui.counter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors

/** Placeholder for the future paredón mode on the phone. */
@Composable
fun ParedonStubScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
            Text(
                text = stringResource(R.string.paredon_stub_message),
                color = PlayceColors.TextPrimary,
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            PrimaryButton(
                text = stringResource(R.string.btn_back),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                style = PrimaryButtonStyle.Outline
            )
        }
    }
}

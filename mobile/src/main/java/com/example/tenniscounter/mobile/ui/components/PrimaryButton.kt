package com.example.tenniscounter.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

enum class PrimaryButtonStyle {
    Solid,
    Outline
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PrimaryButtonStyle = PrimaryButtonStyle.Solid,
    enabled: Boolean = true
) {
    if (style == PrimaryButtonStyle.Outline) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            border = BorderStroke(1.dp, PlayceColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PlayceColors.TextPrimary,
                containerColor = PlayceColors.Surface
            )
        ) {
            Text(text)
        }
        return
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PlayceColors.Accent,
            contentColor = PlayceColors.Background,
            disabledContainerColor = PlayceColors.Border,
            disabledContentColor = PlayceColors.TextSecondary
        )
    ) {
        Text(text)
    }
}

@Preview
@Composable
private fun PrimaryButtonPreview() {
    PlayceTheme {
        PrimaryButton(
            text = "Share your Playce",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

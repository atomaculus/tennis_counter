package com.example.tenniscounter.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = PlayceColors.TextSecondary,
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
    )
}

@Preview
@Composable
private fun SectionHeaderPreview() {
    PlayceTheme {
        SectionHeader("Score recap")
    }
}

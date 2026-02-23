package com.example.tenniscounter.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tenniscounter.mobile.data.local.MatchEntity
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

@Composable
fun MatchCard(
    match: MatchEntity,
    dateText: String,
    durationText: String?,
    setScoresText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PlayceColors.SurfaceElevated),
        border = BorderStroke(1.dp, PlayceColors.Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = match.finalScoreText,
                color = PlayceColors.TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            if (!setScoresText.isNullOrBlank()) {
                Text(
                    text = setScoresText,
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateText,
                    color = PlayceColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                )
                if (!durationText.isNullOrBlank()) {
                    Text(
                        text = durationText,
                        color = PlayceColors.TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MatchCardPreview() {
    PlayceTheme {
        MatchCard(
            match = MatchEntity(
                id = 1,
                createdAt = System.currentTimeMillis(),
                durationSeconds = 4812,
                finalScoreText = "6-4 3-6 6-3",
                setScoresText = "6-4 3-6 6-3",
                idempotencyKey = "preview"
            ),
            dateText = "22 Feb 2026, 18:40",
            durationText = "1:20:12",
            setScoresText = "6-4 | 3-6 | 6-3",
            onClick = {}
        )
    }
}

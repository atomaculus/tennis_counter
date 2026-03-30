package com.example.tenniscounter.mobile.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tenniscounter.mobile.ui.share.ShareCardData
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

@Composable
fun ShareCard(
    data: ShareCardData,
    photoBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(photoBitmap) { photoBitmap?.asImageBitmap() }
    val setScores = remember(data.setScoresText) {
        data.setScoresText?.trim()?.split(Regex("\\s+"))?.joinToString("  ·  ").orEmpty()
    }

    // Capture composable colors for use in non-composable lambdas (Canvas, buildAnnotatedString)
    val accentColor = PlayceColors.Accent
    val textPrimaryColor = PlayceColors.TextPrimary
    val textSecondaryColor = PlayceColors.TextSecondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlayceColors.Background)
            .clip(RoundedCornerShape(28.dp))
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D0D0D),
                                Color(0xFF111111),
                                Color(0xFF080808)
                            )
                        )
                    )
            )
        }

        // Gradient overlay - stronger at bottom for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.15f),
                            0.35f to Color.Black.copy(alpha = 0.25f),
                            0.65f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Subtle accent line
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = accentColor.copy(alpha = 0.25f),
                start = Offset(size.width * 0.08f, size.height * 0.18f),
                end = Offset(size.width * 0.38f, size.height * 0.18f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp, vertical = 52.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: PLAYCE wordmark with brand colors
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PlayceWordmark(fontSize = 18.sp, letterSpacing = 3.sp)
                Text(
                    text = "MATCH RESULT",
                    color = PlayceColors.TextSecondary.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp
                )
            }

            // Center: Score display
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val scoreSize = (maxWidth.value * 0.22f).coerceIn(54f, 110f).sp
                    Text(
                        text = data.scoreText.replace(" ", "\n"),
                        color = PlayceColors.TextPrimary,
                        fontSize = scoreSize,
                        lineHeight = (scoreSize.value * 0.9f).sp,
                        fontWeight = FontWeight.Black
                    )
                }
                if (setScores.isNotBlank()) {
                    Text(
                        text = setScores,
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Bottom: Metadata
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thin accent divider
                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                    drawLine(
                        color = accentColor.copy(alpha = 0.2f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.dateText,
                        color = PlayceColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = data.durationText.removePrefix("Duration: "),
                        color = PlayceColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                PlayceWordmarkSmall()
            }
        }
    }
}

@Composable
private fun PlayceWordmarkSmall() {
    val accentColor = PlayceColors.Accent
    val textPrimaryColor = PlayceColors.TextPrimary
    val textSecondaryColor = PlayceColors.TextSecondary
    val brandText = buildAnnotatedString {
        withStyle(SpanStyle(color = textSecondaryColor.copy(alpha = 0.6f))) {
            append("Tracked with ")
        }
        withStyle(SpanStyle(color = textPrimaryColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)) {
            append("PLAY")
        }
        withStyle(SpanStyle(color = accentColor.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)) {
            append("CE")
        }
    }
    Text(
        text = brandText,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Start
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShareCardPreview916() {
    PlayceTheme {
        Box(
            modifier = Modifier
                .background(PlayceColors.Background)
                .padding(16.dp)
        ) {
            ShareCard(
                data = ShareCardData(
                    scoreText = "6-4 / 3-6 / 6-3",
                    setScoresText = "6-4 3-6 6-3",
                    durationText = "Duration: 01:20:12",
                    dateText = "22 Feb 2026, 18:40",
                    photoUri = null
                ),
                photoBitmap = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun ShareCardPreviewSquare() {
    PlayceTheme {
        Box(
            modifier = Modifier
                .background(PlayceColors.Background)
                .padding(16.dp)
        ) {
            ShareCard(
                data = ShareCardData(
                    scoreText = "7-6 / 6-4",
                    setScoresText = "7-6 6-4",
                    durationText = "Duration: 00:58:09",
                    dateText = "22 Feb 2026, 20:05",
                    photoUri = null
                ),
                photoBitmap = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }
    }
}

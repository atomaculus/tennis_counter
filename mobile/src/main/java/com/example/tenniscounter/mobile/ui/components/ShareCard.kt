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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val setScores = remember(data.setScoresText) { data.setScoresText?.trim().orEmpty() }
    val showAce = remember(data.scoreText, data.durationText) {
        data.scoreText != "0-0" || data.durationText != "Duration: 00:00"
    }

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
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF121212),
                                Color(0xFF060606),
                                Color(0xFF000000)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = PlayceColors.Accent.copy(alpha = 0.14f),
                start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.22f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.22f),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 44.dp, vertical = 52.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PLAYCE",
                    color = PlayceColors.TextPrimary.copy(alpha = 0.92f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    letterSpacing = 2.sp
                )
                if (showAce) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(PlayceColors.AccentMuted)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ACE CAPTURED",
                            color = PlayceColors.Accent,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

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
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.dateText,
                        color = PlayceColors.TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = data.durationText.removePrefix("Duration: "),
                        color = PlayceColors.TextPrimary,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "Captured with PLAYCE",
                    color = PlayceColors.TextSecondary.copy(alpha = 0.86f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
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

package com.example.tenniscounter.mobile.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShareCardData(
    val scoreText: String,
    val setScoresText: String?,
    val durationText: String,
    val dateText: String,
    val photoUri: String?
)

@Composable
fun ShareCard(
    data: ShareCardData,
    photoBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    val imageBitmap = remember(photoBitmap) { photoBitmap?.asImageBitmap() }
    val displayScore = remember(data.scoreText) { data.scoreText.replace('-', '–') }
    val displaySetScores = remember(data.setScoresText) { data.setScoresText?.formatSetScoresForDisplay() }

    Box(modifier = modifier.fillMaxSize()) {
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
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0A3D62),
                                Color(0xFF0B8457),
                                Color(0xFFF39C12)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.52f)
                        )
                    )
                )
        )

        Text(
            text = "MATCH RESULT",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 54.dp)
        )

        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        ) {
            val scoreFontSize = (maxWidth.value * 0.31f).coerceIn(84f, 128f).sp
            Text(
                text = displayScore,
                color = Color.White,
                fontSize = scoreFontSize,
                fontWeight = FontWeight.Black,
                lineHeight = scoreFontSize,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            if (!displaySetScores.isNullOrBlank()) {
                Text(
                    text = displaySetScores,
                    color = Color.White.copy(alpha = 0.93f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(top = 168.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 56.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = data.durationText,
                color = Color.White.copy(alpha = 0.93f),
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = data.dateText,
                color = Color.White.copy(alpha = 0.84f),
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun String.formatSetScoresForDisplay(): String {
    return trim().split(Regex("\\s+")).joinToString(" · ")
}

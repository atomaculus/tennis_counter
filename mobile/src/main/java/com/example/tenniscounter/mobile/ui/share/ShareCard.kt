package com.example.tenniscounter.mobile.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShareCardData(
    val scoreText: String,
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

    Box(modifier = modifier.fillMaxSize()) {
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
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(64.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = "MATCH RESULT", color = Color.White, fontSize = 24.sp)
            Text(
                text = data.scoreText,
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Black
            )
            Text(text = data.durationText, color = Color.White, fontSize = 34.sp)
            Text(text = data.dateText, color = Color.White, fontSize = 30.sp)
        }
    }
}

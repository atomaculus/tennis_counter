package com.example.tenniscounter.mobile.ui.share

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class ShareCardData(
    val scoreText: String,
    val setScoresText: String?,
    val durationText: String,
    val dateText: String,
    val photoUri: String?
)

/** Vertical anchor presets for the movable stat block. */
enum class ShareCardAnchor(val verticalFraction: Float) {
    TOP(0.20f),
    CENTER(0.50f),
    BOTTOM(0.80f)
}

/**
 * User-editable layout + content for the share card. Player names are entered
 * at share time because finished matches do not persist them.
 */
data class ShareCardLayout(
    val anchor: ShareCardAnchor = ShareCardAnchor.BOTTOM,
    // Fine-adjustment offset on top of the anchor, as a fraction of card size.
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val showSetScores: Boolean = true,
    val showDuration: Boolean = true,
    val showDate: Boolean = false,
    val playerA: String = "",
    val playerB: String = ""
) {
    val playersText: String?
        get() {
            val a = playerA.trim()
            val b = playerB.trim()
            if (a.isEmpty() && b.isEmpty()) return null
            val left = a.ifEmpty { "—" }
            val right = b.ifEmpty { "—" }
            return "$left  vs  $right"
        }
}

private val Accent = Color(0xFFB8FF2C)

@Composable
fun ShareCard(
    data: ShareCardData,
    layout: ShareCardLayout,
    photoBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    // When provided, the block is draggable and reports clamped absolute offsets.
    onOffsetChange: ((Float, Float) -> Unit)? = null
) {
    val imageBitmap = remember(photoBitmap) { photoBitmap?.asImageBitmap() }
    val displayScore = remember(data.scoreText) { data.scoreText.replace('-', '–') }
    val displaySetScores = remember(data.setScoresText) { data.setScoresText?.formatSetScoresForDisplay() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        val cardWidthDp = maxWidth.value

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
                                Color(0xFF0D0D0D),
                                Color(0xFF111111),
                                Color(0xFF080808)
                            )
                        )
                    )
            )
        }

        // Top scrim so the fixed logo stays legible over any photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.42f),
                            0.30f to Color.Transparent
                        )
                    )
                )
        )

        var blockW by remember { mutableStateOf(0) }
        var blockH by remember { mutableStateOf(0) }

        val marginFrac = 0.035f
        val halfWFrac = if (containerW > 0) (blockW / containerW) / 2f else 0f
        val halfHFrac = if (containerH > 0) (blockH / containerH) / 2f else 0f

        val resolvedXFrac = clampFrac(0.5f + layout.offsetX, halfWFrac + marginFrac, 1f - halfWFrac - marginFrac)
        val resolvedYFrac = clampFrac(layout.anchor.verticalFraction + layout.offsetY, halfHFrac + marginFrac, 1f - halfHFrac - marginFrac)

        val centerX = resolvedXFrac * containerW
        val centerY = resolvedYFrac * containerH

        val scoreSize = (cardWidthDp * 0.22f).sp
        val parcialesSize = (cardWidthDp * 0.062f).sp
        val playersSize = (cardWidthDp * 0.05f).sp
        val metaSize = (cardWidthDp * 0.05f).sp
        val wordmarkSize = (cardWidthDp * 0.06f).sp
        val matchResultSize = (cardWidthDp * 0.03f).sp

        val textShadow = Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            offset = Offset(0f, containerW * 0.004f),
            blurRadius = containerW * 0.014f
        )

        // Fixed brand lockup, always pinned to the top-left corner.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding((cardWidthDp * 0.055f).dp)
        ) {
            Row {
                Text("PLAY", color = Color.White, fontSize = wordmarkSize, fontWeight = FontWeight.Black, style = TextStyle(shadow = textShadow))
                Text("CE", color = Accent, fontSize = wordmarkSize, fontWeight = FontWeight.Black, style = TextStyle(shadow = textShadow))
            }
            Text(
                text = "MATCH RESULT",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = matchResultSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                style = TextStyle(shadow = textShadow)
            )
        }

        val currentLayout by rememberUpdatedState(layout)
        val dragModifier = if (onOffsetChange != null) {
            Modifier.pointerInput(containerW, containerH, blockW, blockH) {
                // Accumulate drag deltas locally; the closure must not read the
                // (stale) layout parameter, only the up-to-date snapshot at start.
                var accX = 0f
                var accY = 0f
                detectDragGestures(
                    onDragStart = {
                        accX = currentLayout.offsetX
                        accY = currentLayout.offsetY
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (containerW > 0 && containerH > 0) {
                            val anchorFrac = currentLayout.anchor.verticalFraction
                            accX += dragAmount.x / containerW
                            accY += dragAmount.y / containerH
                            accX = clampFrac(0.5f + accX, halfWFrac + marginFrac, 1f - halfWFrac - marginFrac) - 0.5f
                            accY = clampFrac(anchorFrac + accY, halfHFrac + marginFrac, 1f - halfHFrac - marginFrac) - anchorFrac
                            onOffsetChange(accX, accY)
                        }
                    }
                )
            }
        } else {
            Modifier
        }

        // Movable match info. No background box — legibility comes from the
        // text shadow, so it reads on any photo while staying clean.
        Column(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (centerX - blockW / 2f).roundToInt(),
                        (centerY - blockH / 2f).roundToInt()
                    )
                }
                .onSizeChanged {
                    blockW = it.width
                    blockH = it.height
                }
                .then(dragModifier),
            verticalArrangement = Arrangement.spacedBy((cardWidthDp * 0.016f).dp)
        ) {
            Text(
                text = displayScore,
                color = Color.White,
                fontSize = scoreSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                style = TextStyle(shadow = textShadow)
            )

            if (layout.showSetScores && !displaySetScores.isNullOrBlank()) {
                Text(
                    text = displaySetScores,
                    color = Color.White,
                    fontSize = parcialesSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = TextStyle(shadow = textShadow)
                )
            }

            layout.playersText?.let { players ->
                Text(
                    text = players,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = playersSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = TextStyle(shadow = textShadow)
                )
            }

            if (layout.showDuration || layout.showDate) {
                val meta = buildString {
                    if (layout.showDuration) append(data.durationText)
                    if (layout.showDuration && layout.showDate) append("  ·  ")
                    if (layout.showDate) append(data.dateText)
                }
                Text(
                    text = meta,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = metaSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    style = TextStyle(shadow = textShadow)
                )
            }
        }
    }
}

private fun clampFrac(value: Float, lower: Float, upper: Float): Float {
    if (lower > upper) return (lower + upper) / 2f
    return value.coerceIn(lower, upper)
}

private fun String.formatSetScoresForDisplay(): String {
    return trim().split(Regex("\\s+")).joinToString(" · ")
}

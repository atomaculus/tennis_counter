package com.example.tenniscounter.mobile.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.tenniscounter.mobile.ui.theme.PlayceColors
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

/**
 * PLAYCE brand wordmark: PLAY in white, CE in lime.
 * P(white) L(white) A(white) Y(white) C(lime) E(lime)
 */
@Composable
fun PlayceWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
    letterSpacing: TextUnit = 3.sp
) {
    val brandText = buildAnnotatedString {
        val lime = SpanStyle(color = PlayceColors.Accent, fontWeight = FontWeight.Black)
        val white = SpanStyle(color = PlayceColors.TextPrimary, fontWeight = FontWeight.Black)
        withStyle(white) { append("PLAY") }
        withStyle(lime) { append("CE") }
    }
    Text(
        text = brandText,
        fontSize = fontSize,
        letterSpacing = letterSpacing,
        modifier = modifier
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PlayceWordmarkPreview() {
    PlayceTheme {
        PlayceWordmark()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PlayceWordmarkSmallPreview() {
    PlayceTheme {
        PlayceWordmark(fontSize = 16.sp, letterSpacing = 2.sp)
    }
}

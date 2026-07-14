package com.example.tenniscounter.mobile.ui.whatsnew

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors

/**
 * Versioned "What's New" flag. Bump [CURRENT_WHATSNEW_VERSION] whenever the
 * controls change and the sheet should be shown again after an update.
 */
object WhatsNewPrefs {
    const val CURRENT_WHATSNEW_VERSION = 1
    private const val PREFS_NAME = "playce_whatsnew"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version"

    fun shouldShow(context: Context): Boolean {
        val lastSeen = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, 0)
        return lastSeen < CURRENT_WHATSNEW_VERSION
    }

    fun markSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_SEEN_VERSION, CURRENT_WHATSNEW_VERSION)
            .apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PlayceColors.Surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.whatsnew_title),
                style = MaterialTheme.typography.headlineSmall,
                color = PlayceColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            WhatsNewBullet(stringResource(R.string.whatsnew_bullet_controls))
            WhatsNewBullet(stringResource(R.string.whatsnew_bullet_server))
            WhatsNewBullet(stringResource(R.string.whatsnew_bullet_tiebreak))

            Spacer(modifier = Modifier.height(24.dp))

            PrimaryButton(
                text = stringResource(R.string.whatsnew_got_it),
                onClick = onDismiss,
                style = PrimaryButtonStyle.Solid,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WhatsNewBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "•",
            color = PlayceColors.Accent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = PlayceColors.TextSecondary,
            lineHeight = 22.sp
        )
    }
}

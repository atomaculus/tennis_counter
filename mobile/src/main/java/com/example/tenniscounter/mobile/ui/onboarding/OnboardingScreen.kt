package com.example.tenniscounter.mobile.ui.onboarding

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tenniscounter.mobile.R
import com.example.tenniscounter.mobile.ui.components.PrimaryButton
import com.example.tenniscounter.mobile.ui.components.PrimaryButtonStyle
import com.example.tenniscounter.mobile.ui.theme.PlayceColors

private data class OnboardingPage(
    val emoji: String,
    val titleRes: Int,
    val descriptionRes: Int
)

private val pages = listOf(
    OnboardingPage(
        emoji = "\uD83C\uDFBE",
        titleRes = R.string.onboarding_title_1,
        descriptionRes = R.string.onboarding_desc_1
    ),
    OnboardingPage(
        emoji = "\u231A",
        titleRes = R.string.onboarding_title_2,
        descriptionRes = R.string.onboarding_desc_2
    ),
    OnboardingPage(
        emoji = "\uD83D\uDCCA",
        titleRes = R.string.onboarding_title_3,
        descriptionRes = R.string.onboarding_desc_3
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlayceColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.app_name),
            color = PlayceColors.Accent,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedContent(
            targetState = currentPage,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn())
                    .togetherWith(slideOutHorizontally { -it } + fadeOut())
            },
            label = "onboarding_page"
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        // Page indicators
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (currentPage == index) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentPage == index) PlayceColors.Accent
                            else PlayceColors.TextSecondary.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentPage < pages.lastIndex) {
            PrimaryButton(
                text = stringResource(R.string.onboarding_next),
                onClick = { currentPage++ },
                style = PrimaryButtonStyle.Solid,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(
                text = stringResource(R.string.onboarding_skip),
                onClick = onFinish,
                style = PrimaryButtonStyle.Outline,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.onboarding_start),
                onClick = onFinish,
                style = PrimaryButtonStyle.Solid,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = page.emoji,
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = PlayceColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = PlayceColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

object OnboardingPrefs {
    private const val PREFS_NAME = "playce_onboarding"
    private const val KEY_COMPLETED = "onboarding_completed"

    fun isCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_COMPLETED, false)
    }

    fun markCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_COMPLETED, true)
            .apply()
    }
}

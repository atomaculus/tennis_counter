package com.example.tenniscounter.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.tenniscounter.mobile.ui.onboarding.OnboardingPrefs
import com.example.tenniscounter.mobile.ui.onboarding.OnboardingScreen
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayceTheme {
                var showOnboarding by remember {
                    mutableStateOf(!OnboardingPrefs.isCompleted(this@MainActivity))
                }

                if (showOnboarding) {
                    OnboardingScreen(onFinish = {
                        OnboardingPrefs.markCompleted(this@MainActivity)
                        showOnboarding = false
                    })
                } else {
                    MobileApp()
                }
            }
        }
    }
}

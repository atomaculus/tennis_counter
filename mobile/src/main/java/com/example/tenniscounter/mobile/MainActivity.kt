package com.example.tenniscounter.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlayceTheme {
                MobileApp()
            }
        }
    }
}

package com.example.tenniscounter.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.ui.onboarding.OnboardingPrefs
import com.example.tenniscounter.mobile.ui.onboarding.OnboardingScreen
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme

class MainActivity : ComponentActivity() {

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(TAG, "BLUETOOTH_CONNECT permission granted=$granted")
            if (granted) {
                MobileServiceLocator.garminConnectivityManager(applicationContext).initialize(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeInitializeGarmin()
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

    override fun onStart() {
        super.onStart()
        maybeInitializeGarmin()
    }

    private fun maybeInitializeGarmin() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permission = Manifest.permission.BLUETOOTH_CONNECT
            if (ContextCompat.checkSelfPermission(this, permission) !=
                PackageManager.PERMISSION_GRANTED) {
                bluetoothPermissionLauncher.launch(permission)
                return
            }
        }
        MobileServiceLocator.garminConnectivityManager(applicationContext).initialize(this)
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

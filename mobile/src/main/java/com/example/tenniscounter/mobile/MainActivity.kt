package com.example.tenniscounter.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.lifecycleScope
import com.example.tenniscounter.mobile.di.MobileServiceLocator
import com.example.tenniscounter.mobile.health.HealthConnectMatchWriter
import com.example.tenniscounter.mobile.ui.onboarding.OnboardingPrefs
import com.example.tenniscounter.mobile.ui.onboarding.OnboardingScreen
import com.example.tenniscounter.mobile.ui.theme.PlayceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val healthConnectPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    )

    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.i(TAG, "BLUETOOTH_CONNECT permission granted=$granted")
            if (granted) {
                MobileServiceLocator.garminConnectivityManager(applicationContext).initialize(this)
            }
        }

    private val healthConnectPermissionLauncher =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            val hasAllPermissions = granted.containsAll(healthConnectPermissions)
            Log.i(TAG, "Health Connect write permissions granted=$hasAllPermissions")
            if (hasAllPermissions) {
                retryRecentHealthConnectWrites()
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
                        maybeRequestHealthConnectPermissions()
                    })
                } else {
                    LaunchedEffect(Unit) {
                        maybeRequestHealthConnectPermissions()
                    }
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

    private fun maybeRequestHealthConnectPermissions() {
        if (HealthConnectClient.getSdkStatus(this) != HealthConnectClient.SDK_AVAILABLE) {
            Log.i(TAG, "Health Connect unavailable; write permissions not requested")
            return
        }

        lifecycleScope.launch {
            runCatching {
                val grantedPermissions = HealthConnectClient.getOrCreate(this@MainActivity)
                    .permissionController
                    .getGrantedPermissions()
                if (!grantedPermissions.containsAll(healthConnectPermissions)) {
                    healthConnectPermissionLauncher.launch(healthConnectPermissions)
                } else {
                    retryRecentHealthConnectWrites()
                }
            }.onFailure {
                Log.w(TAG, "Health Connect permission check failed", it)
            }
        }
    }

    private fun retryRecentHealthConnectWrites() {
        lifecycleScope.launch {
            runCatching {
                val matches = MobileServiceLocator.matchRepository(applicationContext)
                    .getRecentMatchesWithHealthMetrics()
                Log.i(TAG, "Retrying Health Connect writes for ${matches.size} recent matches with metrics")
                val writer = HealthConnectMatchWriter(applicationContext)
                matches.forEach { match ->
                    writer.writeTennisSessionIfPermitted(match)
                }
            }.onFailure {
                Log.w(TAG, "Health Connect retry failed", it)
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

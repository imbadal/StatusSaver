package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper

class StatusGalleryActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StatusGalleryActivity_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== STATUS GALLERY ACTIVITY STARTED ===")
        Log.d(TAG, "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Check if permission is granted
        val preferenceUtils = PreferenceUtils(application)
        val safUri = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()

        Log.d(TAG, "SAF URI from preferences: $safUri")
        Log.d(TAG, "Onboarding completed: $onboardingCompleted")

        // Check permissions
        val hasPermissions = StorageAccessHelper.hasRequiredPermissions(this)
        Log.d(TAG, "Has required permissions: $hasPermissions")
        
        if (safUri.isNullOrBlank() || !onboardingCompleted) {
            Log.w(
                TAG,
                "❌ Permission not granted or onboarding not completed - launching onboarding"
            )
            // Permission not granted, launch onboarding
            val intent = Intent(
                this,
                com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity::class.java
            )
            startActivity(intent)
            finish()
            return
        }

        Log.d(TAG, "✅ Permission granted and onboarding completed - showing status gallery")
        
        // Permission granted, show status gallery
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    StandaloneStatusGallery(this)
                }
            }
        }
    }
} 
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
import com.inningsstudio.statussaver.core.utils.NavigationManager
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
        
        // Use NavigationManager to determine if we should show this activity or navigate elsewhere
        if (NavigationManager.shouldShowPrivacyPolicy(this)) {
            Log.d(TAG, "Privacy policy not accepted - navigating to PrivacyPolicyActivity")
            NavigationManager.navigateToNextActivity(this)
            finish()
            return
        }
        
        if (NavigationManager.shouldShowOnboarding(this)) {
            Log.d(TAG, "Onboarding not completed - navigating to OnBoardingActivity")
            NavigationManager.navigateToNextActivity(this)
            finish()
            return
        }

        Log.d(TAG, "✅ All checks passed - showing status gallery")
        
        // All checks passed, show status gallery
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
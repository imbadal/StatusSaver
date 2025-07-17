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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusGalleryActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StatusGalleryActivity_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== STATUS GALLERY ACTIVITY STARTED ===")
        Log.d(TAG, "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Show loading UI immediately
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    // Show a simple loading screen while checking navigation
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
        }
        
        // Check navigation requirements in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check if we should show privacy policy
                val shouldShowPrivacyPolicy = NavigationManager.shouldShowPrivacyPolicy(this@StatusGalleryActivity)
                if (shouldShowPrivacyPolicy) {
                    Log.d(TAG, "Privacy policy not accepted - navigating to PrivacyPolicyActivity")
                    withContext(Dispatchers.Main) {
                        NavigationManager.navigateToNextActivity(this@StatusGalleryActivity)
                        finish()
                    }
                    return@launch
                }
                
                // Check if we should show onboarding
                val shouldShowOnboarding = NavigationManager.shouldShowOnboarding(this@StatusGalleryActivity)
                if (shouldShowOnboarding) {
                    Log.d(TAG, "Onboarding not completed - navigating to OnBoardingActivity")
                    withContext(Dispatchers.Main) {
                        NavigationManager.navigateToNextActivity(this@StatusGalleryActivity)
                        finish()
                    }
                    return@launch
                }

                Log.d(TAG, "✅ All checks passed - showing status gallery")
                
                // All checks passed, show status gallery
                withContext(Dispatchers.Main) {
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black
                            ) {
                                StandaloneStatusGallery(this@StatusGalleryActivity)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during navigation check", e)
                withContext(Dispatchers.Main) {
                    // Show error state or fallback
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Error loading app",
                                        color = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 
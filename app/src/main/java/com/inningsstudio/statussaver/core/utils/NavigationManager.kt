package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.content.Intent
import com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity
import com.inningsstudio.statussaver.presentation.ui.privacy.PrivacyPolicyActivity
import com.inningsstudio.statussaver.presentation.ui.status.StatusGalleryActivity

class NavigationManager(private val context: Context) {
    
    companion object {
        fun getNextActivity(context: Context): Class<*> {
            val preferenceUtils = PreferenceUtils(context.applicationContext as android.app.Application)
            
            // Check if privacy policy is accepted
            if (!preferenceUtils.isPrivacyPolicyAccepted()) {
                return PrivacyPolicyActivity::class.java
            }
            
            // Check if onboarding is completed and permissions are granted
            val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
            val safUri = preferenceUtils.getUriFromPreference()
            
            if (!onboardingCompleted || safUri.isNullOrBlank()) {
                return OnBoardingActivity::class.java
            }
            
            // Everything is set up, go to StatusGalleryActivity
            return StatusGalleryActivity::class.java
        }
        
        fun navigateToNextActivity(context: Context) {
            val nextActivity = getNextActivity(context)
            
            val intent = Intent(context, nextActivity)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }
        
        fun shouldShowPrivacyPolicy(context: Context): Boolean {
            val preferenceUtils = PreferenceUtils(context.applicationContext as android.app.Application)
            return !preferenceUtils.isPrivacyPolicyAccepted()
        }
        
        fun shouldShowOnboarding(context: Context): Boolean {
            val preferenceUtils = PreferenceUtils(context.applicationContext as android.app.Application)
            val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
            val safUri = preferenceUtils.getUriFromPreference()
            return !onboardingCompleted || safUri.isNullOrBlank()
        }
    }
} 
package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.content.Intent
import com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity
import com.inningsstudio.statussaver.presentation.ui.privacy.PrivacyPolicyActivity
import com.inningsstudio.statussaver.presentation.ui.status.StatusGalleryActivity

object NotificationIntentHandler {
    
    /**
     * Creates the appropriate intent for notification tap based on user state
     */
    fun createNotificationIntent(context: Context, data: Map<String, String> = emptyMap()): Intent {
        val preferenceUtils = PreferenceUtils(context.applicationContext as android.app.Application)
        
        val intent = if (!preferenceUtils.isPrivacyPolicyAccepted()) {
            // Privacy policy not accepted, show privacy policy
            Intent(context, PrivacyPolicyActivity::class.java)
        } else {
            // Privacy policy accepted, check onboarding status
            val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
            val safUri = preferenceUtils.getUriFromPreference()
            
            if (!onboardingCompleted || safUri.isNullOrBlank()) {
                Intent(context, OnBoardingActivity::class.java)
            } else {
                Intent(context, StatusGalleryActivity::class.java)
            }
        }
        
        // Set flags and add data
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        data.forEach { (key, value) ->
            intent.putExtra(key, value)
        }
        
        return intent
    }
} 
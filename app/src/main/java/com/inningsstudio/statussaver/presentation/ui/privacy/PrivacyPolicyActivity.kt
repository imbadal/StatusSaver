package com.inningsstudio.statussaver.presentation.ui.privacy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.inningsstudio.statussaver.R
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var privacyCheckBox: CheckBox
    private lateinit var proceedButton: Button
    private lateinit var privacyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.whatsapp_green)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        preferenceUtils = PreferenceUtils(application)
        
        setContentView(R.layout.activity_privacy_policy)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        privacyCheckBox = findViewById(R.id.privacyCheckBox)
        proceedButton = findViewById(R.id.proceedButton)
        privacyText = findViewById(R.id.privacyText)
    }

    private fun setupListeners() {
        privacyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            proceedButton.isEnabled = isChecked
            proceedButton.alpha = if (isChecked) 1.0f else 0.5f
        }

        proceedButton.setOnClickListener {
            if (privacyCheckBox.isChecked) {
                // Mark privacy policy as accepted
                preferenceUtils.setPrivacyPolicyAccepted(true)
                
                // Check if onboarding is completed
                val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
                val safUri = preferenceUtils.getUriFromPreference()
                
                if (onboardingCompleted && !safUri.isNullOrBlank()) {
                    // Onboarding completed, go to status gallery activity
                    val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.status.StatusGalleryActivity::class.java)
                    startActivity(intent)
                } else {
                    // Onboarding not completed, go to onboarding
                    val intent = Intent(this, OnBoardingActivity::class.java)
                    startActivity(intent)
                }
                finish()
            }
        }
    }
} 
package com.inningsstudio.statussaver.presentation.ui.privacy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.inningsstudio.statussaver.R
import com.inningsstudio.statussaver.core.utils.NavigationManager
import com.inningsstudio.statussaver.core.utils.PreferenceUtils

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var privacyCheckBox: CheckBox
    private lateinit var proceedButton: LinearLayout
    private lateinit var readPrivacyPolicyButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if privacy policy is already accepted
        preferenceUtils = PreferenceUtils(application)
        if (preferenceUtils.isPrivacyPolicyAccepted()) {
            // Privacy policy already accepted, navigate to appropriate activity
            NavigationManager.navigateToNextActivity(this)
            finish()
            return
        }
        
        window.statusBarColor = ContextCompat.getColor(this, R.color.whatsapp_green)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        setContentView(R.layout.activity_privacy_policy)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        privacyCheckBox = findViewById(R.id.privacyCheckBox)
        proceedButton = findViewById(R.id.proceedButton)
        readPrivacyPolicyButton = findViewById(R.id.readPrivacyPolicyButton)
    }

    private fun setupListeners() {
        // Ensure button is visible but disabled initially
        proceedButton.isEnabled = false
        proceedButton.visibility = android.view.View.VISIBLE
        
        privacyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            proceedButton.isEnabled = isChecked
        }

        readPrivacyPolicyButton.setOnClickListener {
            // Open privacy policy URL in browser
            val privacyPolicyUrl = getString(R.string.privacy_policy_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
            startActivity(intent)
        }

        proceedButton.setOnClickListener {
            if (privacyCheckBox.isChecked) {
                // Mark privacy policy as accepted
                preferenceUtils.setPrivacyPolicyAccepted(true)
                NavigationManager.navigateToNextActivity(this)
                finish()
            }
        }
    }
} 
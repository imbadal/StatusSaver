package com.inningsstudio.statussaver.presentation.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.appcompat.app.AppCompatActivity
import com.inningsstudio.statussaver.R
import com.inningsstudio.statussaver.core.utils.NavigationManager
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import androidx.documentfile.provider.DocumentFile
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class OnBoardingActivity : AppCompatActivity() {
    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var onboardingTitle: TextView
    private lateinit var onboardingDescription: TextView
    private lateinit var onboardingActionButton: TextView
    private lateinit var onboardingActionButtonContainer: LinearLayout

    private var currentStep = 0
    private var folderPermissionGranted = false
    private val totalSteps: Int = 2
    
    // Debouncing variables
    private var isProcessingClick = false
    private var lastClickTime = 0L
    private val CLICK_DEBOUNCE_TIME = 500L // 500ms debounce

    // Activity result launcher for folder selection
    private val folderSelectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("OnBoardingActivity", "Folder selection result: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val treeUri = result.data?.data
            android.util.Log.d("OnBoardingActivity", "Selected URI: $treeUri")
            treeUri?.let {
                // Process folder selection in background
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        android.util.Log.d("OnBoardingActivity", "Processing folder selection")
                        // Check if the last segment is '.Statuses'
                        val pickedFolderName = DocumentFile.fromTreeUri(this@OnBoardingActivity, treeUri)?.name
                        android.util.Log.d("OnBoardingActivity", "Picked folder name: $pickedFolderName")
                        if (pickedFolderName != ".Statuses") {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@OnBoardingActivity, "Please select the .Statuses folder itself, not its parent.", Toast.LENGTH_LONG).show()
                                resetButtonState()
                            }
                            return@launch
                        }
                        
                        android.util.Log.d("OnBoardingActivity", "Taking persistable URI permission")
                        // Take permission in background
                        contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        
                        val actualPath = treeUri.toString()
                        android.util.Log.d("OnBoardingActivity", "Saving URI to preferences: $actualPath")
                        preferenceUtils.setUriToPreference(actualPath)
                        folderPermissionGranted = true
                        
                        withContext(Dispatchers.Main) {
                            android.util.Log.d("OnBoardingActivity", "Moving to next step")
                            moveToNextStep()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OnBoardingActivity", "Error processing folder selection", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@OnBoardingActivity, "Error processing folder selection", Toast.LENGTH_LONG).show()
                            resetButtonState()
                        }
                    }
                }
            } ?: run {
                android.util.Log.w("OnBoardingActivity", "No URI in result data")
                resetButtonState()
            }
        } else {
            android.util.Log.w("OnBoardingActivity", "Folder selection cancelled or failed")
            resetButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.whatsapp_green)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        preferenceUtils = PreferenceUtils(application)
        
        // Check if we already have everything we need in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val shouldShowPrivacyPolicy = NavigationManager.shouldShowPrivacyPolicy(this@OnBoardingActivity)
                val shouldShowOnboarding = NavigationManager.shouldShowOnboarding(this@OnBoardingActivity)
                
                if (!shouldShowPrivacyPolicy && !shouldShowOnboarding) {
                    // Everything is already set up, go to status gallery activity
                    withContext(Dispatchers.Main) {
                        NavigationManager.navigateToNextActivity(this@OnBoardingActivity)
                        finish()
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    setContentView(R.layout.activity_onboarding)
                    initializeViews()
                    
                    // Check current permission status
                    val safUri = preferenceUtils.getUriFromPreference()
                    folderPermissionGranted = !safUri.isNullOrBlank()
                    
                    updateStepUI()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OnBoardingActivity, "Error initializing app", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun initializeViews() {
        onboardingTitle = findViewById(R.id.onboardingTitle)
        onboardingDescription = findViewById(R.id.onboardingDescription)
        onboardingActionButton = findViewById(R.id.onboardingActionButton)
        onboardingActionButtonContainer = findViewById(R.id.onboardingActionButtonContainer)
    }

    private fun updateStepUI() {
        // Update content and button
        when (currentStep) {
            0 -> {
                onboardingTitle.text = "Grant Folder Access"
                onboardingDescription.text = "We need access to your .Statuses folder to work properly. Please select the folder when prompted."
                onboardingActionButton.text = "Select Folder"
                onboardingActionButton.isEnabled = true
                
                // Set click listener on the container
                onboardingActionButtonContainer.setOnClickListener {
                    android.util.Log.d("OnBoardingActivity", "Select Folder button clicked")
                    if (isClickAllowed()) {
                        onboardingActionButton.isEnabled = false
                        onboardingActionButton.text = "Processing..."
                        val start = System.currentTimeMillis()
                        pickFolder()
                        val end = System.currentTimeMillis()
                        android.util.Log.d("OnBoardingActivity", "pickFolder() triggered in ${end - start} ms")
                    }
                }
            }
            1 -> {
                onboardingTitle.text = "You're All Set!"
                onboardingDescription.text = "Note: This app does not store or upload any image or video data to a remote server. All saved statuses remain stored locally on your device. Please respect others’ privacy and seek permission before downloading or sharing any status content."
                onboardingActionButton.text = "Get Started"
                onboardingActionButton.isEnabled = true
                
                // Set click listener on the container
                onboardingActionButtonContainer.setOnClickListener {
                    android.util.Log.d("OnBoardingActivity", "Get Started button clicked")
                    if (isClickAllowed()) {
                        onboardingActionButton.isEnabled = false
                        onboardingActionButton.text = "Processing..."
                        val start = System.currentTimeMillis()
                        completeOnboarding()
                        val end = System.currentTimeMillis()
                        android.util.Log.d("OnBoardingActivity", "completeOnboarding() triggered in ${end - start} ms")
                    }
                }
            }
        }
    }

    private fun isClickAllowed(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_DEBOUNCE_TIME || isProcessingClick) {
            android.util.Log.d("OnBoardingActivity", "Click blocked - debouncing or processing")
            return false
        }
        lastClickTime = currentTime
        isProcessingClick = true
        android.util.Log.d("OnBoardingActivity", "Click allowed")
        return true
    }

    private fun moveToNextStep() {
        if (currentStep < totalSteps - 1) {
            currentStep++
            updateStepUI()
        }
        isProcessingClick = false
        onboardingActionButton.isEnabled = true
        onboardingActionButton.text = if (currentStep == 0) "Select Folder" else "Get Started"
    }

    private fun completeOnboarding() {
        if (folderPermissionGranted) {
            // Process completion in background
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val start = System.currentTimeMillis()
                    preferenceUtils.setOnboardingCompleted(true)
                    val end = System.currentTimeMillis()
                    android.util.Log.d("OnBoardingActivity", "setOnboardingCompleted() took ${end - start} ms")
                    
                    withContext(Dispatchers.Main) {
                        try {
                            android.util.Log.d("OnBoardingActivity", "Starting navigation to next activity")
                            NavigationManager.navigateToNextActivity(this@OnBoardingActivity)
                            android.util.Log.d("OnBoardingActivity", "Navigation completed, finishing activity")
                            finish()
                        } catch (e: Exception) {
                            android.util.Log.e("OnBoardingActivity", "Error during navigation", e)
                            Toast.makeText(this@OnBoardingActivity, "Error navigating to next screen", Toast.LENGTH_LONG).show()
                            resetButtonState()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OnBoardingActivity", "Error completing onboarding", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OnBoardingActivity, "Error completing onboarding", Toast.LENGTH_LONG).show()
                        resetButtonState()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Please complete all steps first", Toast.LENGTH_SHORT).show()
            resetButtonState()
        }
    }

    private fun resetButtonState() {
        isProcessingClick = false
        onboardingActionButton.isEnabled = true
        onboardingActionButton.text = if (currentStep == 0) "Select Folder" else "Get Started"
    }

    private fun pickFolder() {
        val targetPath = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        try {
            android.util.Log.d("OnBoardingActivity", "Opening folder picker")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val storageVolume = sm.primaryStorageVolume
                var intent = storageVolume.createOpenDocumentTreeIntent()
                val initialUri = intent.getParcelableExtra<Uri>(DocumentsContract.EXTRA_INITIAL_URI)
                val replace = initialUri.toString().replace("/root/", "/document/")
                val finalUri = Uri.parse("$replace%3A" + targetPath.replace("/", "%2F"))
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, finalUri)
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                folderSelectionLauncher.launch(intent)
            } else {
                val treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A" + Uri.encode(targetPath))
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeUri)
                    putExtra("android.content.extra.SHOW_ADVANCED", true)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                }
                folderSelectionLauncher.launch(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("OnBoardingActivity", "Error opening folder picker", e)
            Toast.makeText(this, "Error opening folder picker", Toast.LENGTH_LONG).show()
            resetButtonState()
        }
    }
}
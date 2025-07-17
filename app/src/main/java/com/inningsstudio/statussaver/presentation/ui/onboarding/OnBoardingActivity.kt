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
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import androidx.documentfile.provider.DocumentFile
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class OnBoardingActivity : AppCompatActivity() {
    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var onboardingTitle: TextView
    private lateinit var onboardingDescription: TextView
    private lateinit var onboardingActionButton: TextView


    private var currentStep = 0
    private var folderPermissionGranted = false
    private val totalSteps: Int = 2

    // Activity result launcher for folder selection
    private val folderSelectionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val treeUri = result.data?.data
            treeUri?.let {
                // Check if the last segment is '.Statuses'
                val pickedFolderName = DocumentFile.fromTreeUri(this, treeUri)?.name
                if (pickedFolderName != ".Statuses") {
                    Toast.makeText(this, "Please select the .Statuses folder itself, not its parent.", Toast.LENGTH_LONG).show()
                    return@let
                }
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val actualPath = treeUri.toString()
                preferenceUtils.setUriToPreference(actualPath)
                folderPermissionGranted = true
                moveToNextStep()
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.whatsapp_green)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        preferenceUtils = PreferenceUtils(application)
        
        // Check if we already have everything we need
        val safUri = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
        val hasRequiredPermissions = StorageAccessHelper.hasRequiredPermissions(this)
        
        if (!safUri.isNullOrBlank() && onboardingCompleted && hasRequiredPermissions) {
            // Everything is already set up, go to status gallery activity
            val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.status.StatusGalleryActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_onboarding)
        initializeViews()
        
        // Check current permission status
        folderPermissionGranted = !safUri.isNullOrBlank()
        
        updateStepUI()
    }

    private fun initializeViews() {
        onboardingTitle = findViewById(R.id.onboardingTitle)
        onboardingDescription = findViewById(R.id.onboardingDescription)
        onboardingActionButton = findViewById(R.id.onboardingActionButton)
    }

    private fun updateStepUI() {
        // Update content and button
        when (currentStep) {
            0 -> {
                onboardingTitle.text = "Grant Folder Access"
                onboardingDescription.text = "To save your WhatsApp statuses, we need access to your .Statuses folder. Please select the folder when prompted."
                onboardingActionButton.text = "Select Folder"
                onboardingActionButton.isEnabled = true
                onboardingActionButton.setOnClickListener {
                    pickFolder()
                }
            }
            1 -> {
                onboardingTitle.text = "You're All Set!"
                onboardingDescription.text = "Perfect! You can now save and manage your WhatsApp statuses easily. Let's get started."
                onboardingActionButton.text = "Get Started"
                onboardingActionButton.isEnabled = true
                onboardingActionButton.setOnClickListener {
                    completeOnboarding()
                }
            }
        }
    }





    private fun moveToNextStep() {
        if (currentStep < totalSteps - 1) {
            currentStep++
            updateStepUI()
        }
    }

    private fun completeOnboarding() {
        if (folderPermissionGranted) {
            preferenceUtils.setOnboardingCompleted(true)
            navigateToMainActivity()
        } else {
            Toast.makeText(this, "Please complete all steps first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.status.StatusGalleryActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun pickFolder() {
        val targetPath = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        try {
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
            Toast.makeText(this, "Error opening folder picker", Toast.LENGTH_LONG).show()
        }
    }
}
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

class OnBoardingActivity : AppCompatActivity() {
    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var stepCounter: TextView
    private lateinit var stepIndicatorContainer: LinearLayout
    private lateinit var onboardingTitle: TextView
    private lateinit var onboardingDescription: TextView
    private lateinit var onboardingActionButton: Button
    private lateinit var onboardingContentLayout: LinearLayout

    private var currentStep = 0
    private var folderPermissionGranted = false
    private var mediaPermissionsGranted = false
    private val totalSteps: Int = 3

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
                updateStepUI()
            }
        }
    }

    // Activity result launcher for permissions
    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            mediaPermissionsGranted = true
            Toast.makeText(this, "Media permissions granted!", Toast.LENGTH_SHORT).show()
            updateStepUI()
        } else {
            Toast.makeText(this, "Media permissions are required to access WhatsApp statuses", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.purple_700)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        preferenceUtils = PreferenceUtils(application)
        
        // Check if we already have everything we need
        val safUri = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
        val hasRequiredPermissions = StorageAccessHelper.hasRequiredPermissions(this)
        
        if (!safUri.isNullOrBlank() && onboardingCompleted && hasRequiredPermissions) {
            // Everything is already set up, go to main activity
            val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        setContentView(R.layout.activity_onboarding)
        initializeViews()
        
        // Check current permission status
        mediaPermissionsGranted = StorageAccessHelper.hasRequiredPermissions(this)
        folderPermissionGranted = !safUri.isNullOrBlank()
        
        updateStepUI()
    }

    private fun initializeViews() {
        stepCounter = findViewById(R.id.stepCounter)
        stepIndicatorContainer = findViewById(R.id.stepIndicatorContainer)
        onboardingContentLayout = findViewById(R.id.onboardingContentLayout)
        onboardingTitle = findViewById(R.id.onboardingTitle)
        onboardingDescription = findViewById(R.id.onboardingDescription)
        onboardingActionButton = findViewById(R.id.onboardingActionButton)
    }

    private fun updateStepUI() {
        // Update step counter
        stepCounter.text = "Step ${currentStep + 1} of $totalSteps"
        // Update step indicators
        updateStepIndicators()
        // Update content and button
        when (currentStep) {
            0 -> {
                onboardingTitle.text = if (mediaPermissionsGranted) "Permissions Granted!" else "Grant Media Permissions"
                onboardingDescription.text = if (mediaPermissionsGranted)
                    "Great! You've granted the necessary media permissions. Let's continue to the next step."
                else
                    "We need access to your media files to read WhatsApp statuses. Please grant the required permissions."
                onboardingActionButton.text = if (mediaPermissionsGranted) "Continue" else "Grant Permissions"
                onboardingActionButton.isEnabled = true
                onboardingActionButton.setOnClickListener {
                    if (mediaPermissionsGranted) {
                        moveToNextStep()
                    } else {
                        requestMediaPermissions()
                    }
                }
            }
            1 -> {
                onboardingTitle.text = if (folderPermissionGranted) "Folder Selected!" else "Pick .Statuses Folder"
                onboardingDescription.text = if (folderPermissionGranted)
                    "Great! You've selected the WhatsApp .Statuses folder. You can now proceed."
                else
                    "We need access to your WhatsApp .Statuses folder to save your statuses. Please pick the folder using the system folder picker."
                onboardingActionButton.text = if (folderPermissionGranted) "Continue" else "Pick .Statuses Folder"
                onboardingActionButton.isEnabled = true
                onboardingActionButton.setOnClickListener {
                    if (folderPermissionGranted) {
                        moveToNextStep()
                    } else {
                        pickFolder()
                    }
                }
            }
            2 -> {
                onboardingTitle.text = "Welcome to StatusSaver!"
                onboardingDescription.text = "You're all set! Now you can save and manage your WhatsApp statuses easily. Tap 'Get Started' to begin!"
                onboardingActionButton.text = "Get Started"
                onboardingActionButton.isEnabled = true
                onboardingActionButton.setOnClickListener {
                    completeOnboarding()
                }
            }
        }
    }

    private fun requestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 and below
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        
        permissionLauncher.launch(permissions)
    }

    private fun updateStepIndicators() {
        stepIndicatorContainer.removeAllViews()
        for (i in 0 until totalSteps) {
            val indicator = ImageView(this)
            val size = resources.getDimensionPixelSize(R.dimen.step_indicator_size)
            val params = LinearLayout.LayoutParams(size, size)
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.step_indicator_margin)
            indicator.layoutParams = params
            when {
                i < currentStep -> indicator.setImageResource(R.drawable.step_indicator_completed)
                i == currentStep -> indicator.setImageResource(R.drawable.step_indicator_current)
                else -> indicator.setImageResource(R.drawable.step_indicator_pending)
            }
            indicator.alpha = if (i == currentStep) 1.0f else 0.5f
            stepIndicatorContainer.addView(indicator)
        }
    }

    private fun moveToNextStep() {
        if (currentStep < totalSteps - 1) {
            currentStep++
            updateStepUI()
        }
    }

    private fun completeOnboarding() {
        if (mediaPermissionsGranted && folderPermissionGranted) {
            preferenceUtils.setOnboardingCompleted(true)
            navigateToMainActivity()
        } else {
            Toast.makeText(this, "Please complete all steps first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun pickFolder() {
        val targetPath = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
        val context = this
        AlertDialog.Builder(this)
            .setTitle("Grant WhatsApp Status Folder Access")
            .setMessage(
                "If the next screen does not open the .Statuses folder, please navigate to:\n\n" +
                "Android > media > com.whatsapp > WhatsApp > Media > .Statuses\n\n" +
                "and select it. This is required to access WhatsApp statuses."
            )
            .setPositiveButton("Continue") { _, _ ->
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
                    Toast.makeText(context, "Error opening folder picker", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
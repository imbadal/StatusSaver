package com.inningsstudio.statussaver.presentation.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.inningsstudio.statussaver.R
import com.inningsstudio.statussaver.core.constants.Const
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ImageView
import android.util.TypedValue
import java.io.File
import android.app.Dialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.inningsstudio.statussaver.presentation.ui.MainActivity
import com.inningsstudio.statussaver.presentation.viewmodel.OnboardingViewModel
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity

class OnBoardingActivity : FragmentActivity() {

    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var viewPager: ViewPager2
    private lateinit var stepCounter: TextView
    private lateinit var backButton: ImageView
    private lateinit var nextButton: TextView
    private lateinit var step1Container: LinearLayout
    private lateinit var step2Container: LinearLayout
    private lateinit var step3Container: LinearLayout
    private lateinit var step4Container: LinearLayout
    
    private var currentStep = 0
    private var folderPermissionGranted = false
    private var storagePermissionGranted = false
    private var android15PermissionGranted = false
    private var permissionAttempts = 0
    private val maxPermissionAttempts = 3
    private var permissionMaxAttemptsReached = false
    
    // Check if Android 15 permission step is needed
    private val isAndroid15StepNeeded: Boolean
        get() = Build.VERSION.SDK_INT >= 34 // Android 15
    
    // Total number of steps (3 for older devices, 4 for Android 15+)
    private val totalSteps: Int
        get() = if (isAndroid15StepNeeded) 4 else 3

    // Activity result launcher for folder selection
    private val folderSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val treeUri = result.data?.data
            treeUri?.let {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val actualPath = treeUri.toString()
                preferenceUtils.setUriToPreference(actualPath)
                folderPermissionGranted = true
                updateStepIndicator()
                updateButtonText()
                updateStepContent()
                
                // Auto-navigate to next step
                moveToNextStep()
            }
        }
    }

    // Activity result launcher for permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            storagePermissionGranted = true
            permissionMaxAttemptsReached = false
            permissionAttempts = 0
            preferenceUtils.setPermissionAttempts(0)
            
            // Update UI to show permissions are granted
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
            
            // Auto-navigate to next step
            moveToNextStep()
        } else {
            permissionAttempts++
            preferenceUtils.setPermissionAttempts(permissionAttempts)
            
            // Check if any permission was permanently denied (user checked "Don't ask again")
            val permanentlyDenied = permissions.any { (permission, granted) ->
                !granted && !shouldShowRequestPermissionRationale(permission)
            }
            
            if (permanentlyDenied || permissionAttempts >= maxPermissionAttempts) {
                permissionMaxAttemptsReached = true
                updateStepIndicator()
                updateButtonText()
                updateStepContent()
                showManualPermissionDialog()
            } else {
                updateStepContent()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set status bar color to match the gradient background start color
        window.statusBarColor = android.graphics.Color.parseColor("#667eea")
        
        // Make status bar icons dark (black) for better visibility on light background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and 
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        
        // Enable edge-to-edge display but keep content safe from status bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        
        setContentView(R.layout.activity_onboarding)

        preferenceUtils = PreferenceUtils(application)
        
        // Check if onboarding is already completed (all 3 steps done)
        if (isOnboardingFullyCompleted()) {
            // Skip onboarding if already completed
            navigateToMainActivity()
            return
        }

        initializeViews()
        setupViewPager()
        
        // Restore step states when resuming onboarding (after views are initialized)
        restoreStepStates()
        
        updateStepIndicator()
        updateButtonText()
        updateStepContent()
        
        // Navigate to the appropriate step
        viewPager.currentItem = currentStep

        permissionAttempts = preferenceUtils.getPermissionAttempts()
    }

    override fun onResume() {
        super.onResume()
        
        // Check if permissions were granted when user returns from settings
        if (permissionMaxAttemptsReached && checkStoragePermissions()) {
            // User granted permissions manually in settings
            storagePermissionGranted = true
            permissionMaxAttemptsReached = false
            permissionAttempts = 0
            
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
            
            // Auto-navigate to next step
            moveToNextStep()
        }
        
        // Check if Android 15 permission was granted when user returns from settings
        if (isAndroid15StepNeeded && !android15PermissionGranted && StorageAccessHelper.hasManageExternalStoragePermission()) {
            android15PermissionGranted = true
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
            moveToNextStep()
        }
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.onboardingViewPager)
        stepCounter = findViewById(R.id.stepCounter)
        backButton = findViewById(R.id.backButton)
        nextButton = findViewById(R.id.nextButton)
        step1Container = findViewById(R.id.step1Container)
        step2Container = findViewById(R.id.step2Container)
        step3Container = findViewById(R.id.step3Container)
        step4Container = findViewById(R.id.step4Container)

        backButton.setOnClickListener { moveToPreviousStep() }
        nextButton.setOnClickListener { handleNextButtonClick() }
        
        // Show/hide Android 15 step based on device version
        updateStepVisibility()
    }

    private fun updateStepVisibility() {
        val android15StepDivider = findViewById<View>(R.id.android15StepDivider)
        
        if (isAndroid15StepNeeded) {
            step4Container.visibility = View.VISIBLE
            android15StepDivider.visibility = View.VISIBLE
        } else {
            step4Container.visibility = View.GONE
            android15StepDivider.visibility = View.GONE
        }
    }

    private fun setupViewPager() {
        val onboardingAdapter = OnboardingAdapter()
        viewPager.adapter = onboardingAdapter
        
        // Disable swiping completely to enforce step-by-step progression
        viewPager.isUserInputEnabled = false
        
        // Add page change callback to prevent swiping without completing current step
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentStep = position
                updateStepIndicator()
                updateButtonText()
                updateStepCounter()
            }
        })
    }

    private fun updateStepIndicator() {
        updateStepIndicatorState(step1Container, 0, storagePermissionGranted)
        updateStepIndicatorState(step2Container, 1, folderPermissionGranted)
        
        if (isAndroid15StepNeeded) {
            updateStepIndicatorState(step3Container, 2, android15PermissionGranted)
            updateStepIndicatorState(step4Container, 3, false) // Welcome step is always last
        } else {
            updateStepIndicatorState(step3Container, 2, false) // Welcome step for older devices
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun updateStepIndicatorState(container: LinearLayout, stepIndex: Int, isCompleted: Boolean) {
        val frameLayout = container.getChildAt(0) as FrameLayout
        val stepNumber = container.getChildAt(1) as TextView
        val checkIcon = frameLayout.getChildAt(0) as ImageView

        val tickSize = dpToPx(32)

        when {
            currentStep == stepIndex && isCompleted -> {
                frameLayout.setBackgroundResource(R.drawable.step_indicator_selected_complete)
                checkIcon.visibility = View.VISIBLE
                stepNumber.visibility = View.GONE
                checkIcon.setImageResource(R.drawable.tick_green_selected)
                checkIcon.layoutParams = FrameLayout.LayoutParams(
                    tickSize, tickSize
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
            currentStep == stepIndex && !isCompleted -> {
                frameLayout.setBackgroundResource(R.drawable.step_indicator_selected_incomplete)
                checkIcon.visibility = View.VISIBLE
                stepNumber.visibility = View.GONE
                checkIcon.setImageResource(R.drawable.tick_white_selected)
                checkIcon.layoutParams = FrameLayout.LayoutParams(
                    tickSize, tickSize
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
            currentStep != stepIndex && isCompleted -> {
                frameLayout.setBackgroundResource(R.drawable.step_indicator_unselected_complete)
                checkIcon.visibility = View.VISIBLE
                stepNumber.visibility = View.GONE
                checkIcon.setImageResource(R.drawable.tick_green_unselected)
                checkIcon.layoutParams = FrameLayout.LayoutParams(
                    tickSize, tickSize
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
            else -> {
                frameLayout.setBackgroundResource(R.drawable.step_indicator_unselected_incomplete)
                checkIcon.visibility = View.VISIBLE
                stepNumber.visibility = View.GONE
                checkIcon.setImageResource(R.drawable.tick_white)
                checkIcon.layoutParams = FrameLayout.LayoutParams(
                    tickSize, tickSize
                ).apply {
                    gravity = android.view.Gravity.CENTER
                }
            }
        }
    }

    private fun updateStepCounter() {
        stepCounter.text = "Step ${currentStep + 1} of $totalSteps"
    }

    private fun updateButtonText() {
        backButton.visibility = if (currentStep > 0) View.VISIBLE else View.GONE
        
        when (currentStep) {
            0 -> {
                when {
                    storagePermissionGranted -> nextButton.text = "Next"
                    permissionMaxAttemptsReached -> nextButton.text = "Open Settings"
                    else -> nextButton.text = "Grant Permissions"
                }
            }
            1 -> {
                if (folderPermissionGranted) {
                    nextButton.text = "Next"
                } else {
                    if (checkStoragePermissions()) {
                        nextButton.text = "Create Folder"
                    } else {
                        nextButton.text = "Grant Permissions"
                    }
                }
            }
            2 -> {
                if (android15PermissionGranted) {
                    nextButton.text = "Next"
                } else {
                    nextButton.text = "Grant Permissions"
                }
            }
            3 -> nextButton.text = "Get Started"
        }
    }

    private fun handleNextButtonClick() {
        when (currentStep) {
            0 -> {
                when {
                    storagePermissionGranted -> moveToNextStep()
                    permissionMaxAttemptsReached -> openAppSettings()
                    else -> requestStoragePermissions()
                }
            }
            1 -> {
                if (!folderPermissionGranted) {
                    if (checkStoragePermissions()) {
                        // Permissions already granted, create folder
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            createAndOpenStatusWpFolder()
                        } else {
                            createStatusWpFolderLegacy()
                        }
                    } else {
                        // Request permissions first
                        requestStoragePermissions()
                    }
                } else {
                    moveToNextStep()
                }
            }
            2 -> {
                if (!android15PermissionGranted) {
                    requestAndroid15Permissions()
                } else {
                    moveToNextStep()
                }
            }
            3 -> completeOnboarding()
        }
    }

    private fun requestFolderPermission() {
        when {
            // Android 10+ (API 29+) - Use SAF (Storage Access Framework)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // For modern Android, first check if we have storage permissions
                if (checkStoragePermissions()) {
                    // Permissions granted, create StatusWp folder and open it
                    createAndOpenStatusWpFolder()
                } else {
                    // Request permissions first
                    requestStoragePermissions()
                }
            }
            
            // Android 9 and below (API 28-) - Use traditional storage permissions
            else -> {
                handleLegacyFolderAccess()
            }
        }
    }

    private fun createAndOpenStatusWpFolder() {
        try {
            // Create StatusWp folder in external storage
            val externalDir = getExternalFilesDir(null)
            val statusWpDir = File(externalDir, "StatusWp")
            
            if (!statusWpDir.exists()) {
                val created = statusWpDir.mkdirs()
                if (!created) {
                    // Fallback to default external directory
                    preferenceUtils.setUriToPreference(externalDir?.absolutePath ?: "")
                    folderPermissionGranted = true
                    updateStepIndicator()
                    updateButtonText()
                    updateStepContent()
                    Toast.makeText(this, "Using default folder for statuses", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // Now open the folder picker pointing to the StatusWp folder
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra("android.content.extra.SHOW_ADVANCE", true)
            
            // Try to set initial URI to the StatusWp folder
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val statusWpUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary:Android/data/${packageName}/files/StatusWp")
                    intent.putExtra("android.content.extra.INITIAL_URI", statusWpUri)
                } catch (e: Exception) {
                    // Continue without INITIAL_URI
                }
            }
            
            folderSelectionLauncher.launch(intent)
            
        } catch (e: Exception) {
            // Fallback to traditional folder selection
            showFolderSelectionGuide()
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra("android.content.extra.SHOW_ADVANCE", true)
            folderSelectionLauncher.launch(intent)
        }
    }

    private fun showFolderSelectionGuide() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Folder")
            .setMessage("The folder picker will open. To create the StatusWp folder:\n\n" +
                    "1. Navigate to the root directory (usually 'Internal storage')\n" +
                    "2. Tap the '+' button to create a new folder\n" +
                    "3. Name it 'StatusWp'\n" +
                    "4. Select the StatusWp folder")
            .setPositiveButton("Got it!") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }

    private fun handleLegacyFolderAccess() {
        // For older Android versions, we'll rely on storage permissions
        // and create the StatusWp folder programmatically
        if (checkStoragePermissions()) {
            // Permissions already granted, create folder and proceed
            createStatusWpFolderLegacy()
        } else {
            // Request storage permissions first
            requestStoragePermissions()
        }
    }

    private fun createStatusWpFolderLegacy() {
        try {
            // For older Android versions, try to create StatusWp folder in external storage
            val externalDir = getExternalFilesDir(null)
            val statusWpDir = File(externalDir, "StatusWp")
            
            if (!statusWpDir.exists()) {
                val created = statusWpDir.mkdirs()
                if (created) {
                    // Store the path for later use
                    preferenceUtils.setUriToPreference(statusWpDir.absolutePath)
                    folderPermissionGranted = true
                    updateStepIndicator()
                    updateButtonText()
                    updateStepContent()
                    moveToNextStep()
                } else {
                    // Fallback: use default external directory
                    preferenceUtils.setUriToPreference(externalDir?.absolutePath ?: "")
                    folderPermissionGranted = true
                    updateStepIndicator()
                    updateButtonText()
                    updateStepContent()
                    moveToNextStep()
                }
            } else {
                // Folder already exists
                preferenceUtils.setUriToPreference(statusWpDir.absolutePath)
                folderPermissionGranted = true
                updateStepIndicator()
                updateButtonText()
                updateStepContent()
                moveToNextStep()
            }
        } catch (e: Exception) {
            // Final fallback: use app's internal directory
            val internalDir = filesDir
            preferenceUtils.setUriToPreference(internalDir.absolutePath)
            folderPermissionGranted = true
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
            moveToNextStep()
        }
    }

    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            storagePermissionGranted = true
            moveToNextStep()
        } else {
            // Only check for permanently denied if user has actually denied at least once
            if (permissionAttempts > 0) {
                val permanentlyDenied = permissionsToRequest.any { permission ->
                    !shouldShowRequestPermissionRationale(permission)
                }
                
                val exceededMaxAttempts = permissionAttempts >= maxPermissionAttempts
                
                if (permanentlyDenied || exceededMaxAttempts) {
                    // User has permanently denied permissions or exceeded max attempts
                    permissionMaxAttemptsReached = true
                    updateStepIndicator()
                    updateButtonText()
                    updateStepContent()
                    showManualPermissionDialog()
                    return
                }
            }
            
            // If permissionAttempts == 0 or user still has attempts, request permissions normally
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun showManualPermissionDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_permission_settings, null)
        dialog.setContentView(view)
        dialog.setCancelable(false)

        // Configure dialog window for bottom sheet appearance
        val window = dialog.window
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(android.view.Gravity.BOTTOM)
            
            // Add animation for bottom sheet appearance
            attributes = attributes.apply {
                windowAnimations = android.R.style.Animation_Dialog
            }
        }

        view.findViewById<TextView>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }
        view.findViewById<TextView>(R.id.openSettingsButton).setOnClickListener {
            dialog.dismiss()
            openAppSettings()
        }

        dialog.show()
    }

    private fun moveToNextStep() {
        if (currentStep < totalSteps - 1 && isStepCompleted(currentStep)) {
            currentStep++
            viewPager.currentItem = currentStep
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
        }
    }

    private fun moveToPreviousStep() {
        if (currentStep > 0) {
            currentStep--
            viewPager.currentItem = currentStep
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
        }
    }

    private fun completeOnboarding() {
        // Check if all required steps are completed
        val allStepsCompleted = when {
            isAndroid15StepNeeded -> folderPermissionGranted && storagePermissionGranted && android15PermissionGranted
            else -> folderPermissionGranted && storagePermissionGranted
        }
        
        if (allStepsCompleted) {
            preferenceUtils.setOnboardingCompleted(true)
            navigateToMainActivity()
        } else {
            // If not all steps are completed, show appropriate message
            when {
                !storagePermissionGranted -> {
                    if (permissionMaxAttemptsReached) {
                        Toast.makeText(this, "Please grant permissions manually in Settings", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Please grant storage permissions first", Toast.LENGTH_SHORT).show()
                    }
                }
                !folderPermissionGranted -> Toast.makeText(this, "Please select a folder first", Toast.LENGTH_SHORT).show()
                isAndroid15StepNeeded && !android15PermissionGranted -> Toast.makeText(this, "Please grant Android 15+ permissions first", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(this, "Please complete all steps first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun fetchImages(statusUri: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(Const.STATUS_URI, statusUri)
        startActivity(intent)
        finish()
    }

    private fun isOnboardingFullyCompleted(): Boolean {
        // Check if onboarding was marked as completed
        if (!preferenceUtils.isOnboardingCompleted()) {
            return false
        }
        
        // Check if all required permissions are granted
        val storagePermissionsGranted = checkStoragePermissions()
        val folderSelected = !preferenceUtils.getUriFromPreference().isNullOrBlank()
        val android15PermissionGranted = if (isAndroid15StepNeeded) {
            StorageAccessHelper.hasManageExternalStoragePermission()
        } else {
            true
        }
        
        return storagePermissionsGranted && folderSelected && android15PermissionGranted
    }

    private fun restoreStepStates() {
        // Check if folder permission was already granted
        val uriTree = preferenceUtils.getUriFromPreference()
        if (!uriTree.isNullOrBlank()) {
            folderPermissionGranted = true
        }

        // Check if storage permissions were already granted
        if (checkStoragePermissions()) {
            storagePermissionGranted = true
        } else {
            // Only show manual permission if user has actually denied at least once
            if (permissionAttempts > 0) {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                } else {
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                }
                
                // Check if any permission is permanently denied (user checked "Don't ask again")
                val permanentlyDenied = permissions.any { permission ->
                    ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED &&
                    !shouldShowRequestPermissionRationale(permission)
                }
                
                // Check if user has exceeded max attempts
                val exceededMaxAttempts = permissionAttempts >= maxPermissionAttempts
                
                if (permanentlyDenied || exceededMaxAttempts) {
                    permissionMaxAttemptsReached = true
                }
            }
        }
        
        // Check if Android 15 permission was already granted
        if (isAndroid15StepNeeded && StorageAccessHelper.hasManageExternalStoragePermission()) {
            android15PermissionGranted = true
        }

        // Determine which step to show based on completion status
        currentStep = when {
            !storagePermissionGranted -> 0
            !folderPermissionGranted -> 1
            isAndroid15StepNeeded && !android15PermissionGranted -> 2
            else -> if (isAndroid15StepNeeded) 3 else 2
        }
    }

    private fun checkStoragePermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isStepCompleted(step: Int): Boolean {
        return when (step) {
            0 -> storagePermissionGranted
            1 -> folderPermissionGranted
            2 -> if (isAndroid15StepNeeded) android15PermissionGranted else true
            3 -> true // Welcome step is always considered complete
            else -> false
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun updateStepContent() {
        // Update the current step's content based on state
        when (currentStep) {
            0 -> updatePermissionsContent()
            1 -> updateFolderSelectionContent()
            2 -> updateAndroid15Content()
            3 -> updateWelcomeContent()
        }
    }

    private fun updateFolderSelectionContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun updatePermissionsContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun updateAndroid15Content() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun updateWelcomeContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun requestAndroid15Permissions() {
        if (Build.VERSION.SDK_INT >= 34) {
            // Check if permission is already granted
            if (StorageAccessHelper.hasManageExternalStoragePermission()) {
                android15PermissionGranted = true
                updateStepIndicator()
                updateButtonText()
                updateStepContent()
                moveToNextStep()
            } else {
                // Request the permission
                StorageAccessHelper.requestManageExternalStoragePermission(this)
            }
        } else {
            // For older devices, skip this step
            android15PermissionGranted = true
            moveToNextStep()
        }
    }

    // ViewPager Adapter for onboarding steps
    private inner class OnboardingAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
        
        private val layouts = if (isAndroid15StepNeeded) {
            listOf(
                R.layout.step_permissions,
                R.layout.step_folder_selection,
                R.layout.step_android15,
                R.layout.step_welcome
            )
        } else {
            listOf(
                R.layout.step_permissions,
                R.layout.step_folder_selection,
                R.layout.step_welcome
            )
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = layoutInflater.inflate(viewType, parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            when (position) {
                0 -> bindPermissionsStep(holder.itemView)
                1 -> bindFolderSelectionStep(holder.itemView)
                2 -> if (isAndroid15StepNeeded) bindAndroid15Step(holder.itemView) else bindWelcomeStep(holder.itemView)
                3 -> bindWelcomeStep(holder.itemView)
            }
        }

        override fun getItemCount(): Int = layouts.size

        override fun getItemViewType(position: Int): Int = layouts[position]

        inner class OnboardingViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
        
        private fun bindPermissionsStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            val manualStepsLayout = view.findViewById<LinearLayout>(R.id.manualPermissionSteps)
            
            when {
                storagePermissionGranted -> {
                    titleText?.text = "Permissions Granted!"
                    descriptionText?.text = "Thank you for granting storage permissions. You can now proceed to create your StatusWp folder."
                    manualStepsLayout?.visibility = View.GONE
                    descriptionText?.visibility = View.VISIBLE
                }
                permissionMaxAttemptsReached -> {
                    titleText?.text = "Manual Permission Required"
                    descriptionText?.visibility = View.GONE
                    manualStepsLayout?.visibility = View.VISIBLE
                }
                else -> {
                    titleText?.text = "Grant Permissions"
                    descriptionText?.text = "We need storage permissions to save your WhatsApp statuses. Please grant the required permissions to continue."
                    manualStepsLayout?.visibility = View.GONE
                    descriptionText?.visibility = View.VISIBLE
                }
            }
        }

        private fun bindFolderSelectionStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            if (folderPermissionGranted) {
                titleText?.text = "Folder Selected!"
                descriptionText?.text = "Thank you for choosing a folder. You can now proceed to the next step."
            } else {
                titleText?.text = "Choose a Folder"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    descriptionText?.text = "We'll create a 'StatusWp' folder for you and open it for confirmation. Please grant storage permissions first."
                } else {
                    descriptionText?.text = "We'll create a 'StatusWp' folder for you automatically. Please grant storage permissions to continue."
                }
            }
        }

        private fun bindAndroid15Step(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            if (android15PermissionGranted) {
                titleText?.text = "Android 15+ Permissions Granted!"
                descriptionText?.text = "Thank you for granting Android 15+ permissions. You can now proceed to the next step."
            } else {
                titleText?.text = "Android 15+ Permissions"
                descriptionText?.text = "We need additional permissions for Android 15+ devices. Please grant the required permissions to continue."
            }
        }

        private fun bindWelcomeStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            titleText?.text = "Welcome to StatusSaver!"
            descriptionText?.text = "You're all set! Now you can save and manage your WhatsApp statuses easily. Tap 'Get Started' to begin saving your WhatsApp statuses!"
        }
    }
}
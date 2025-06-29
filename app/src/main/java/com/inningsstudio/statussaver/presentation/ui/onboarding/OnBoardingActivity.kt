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
import com.inningsstudio.statussaver.core.utils.StatusPathDetector

class OnBoardingActivity : FragmentActivity() {

    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var viewPager: ViewPager2
    private lateinit var stepCounter: TextView
    private lateinit var backButton: ImageView
    private lateinit var nextButton: TextView
    private lateinit var step1Container: LinearLayout
    private lateinit var step2Container: LinearLayout
    private lateinit var step3Container: LinearLayout
    
    private var currentStep = 0
    private var storagePermissionGranted = false
    private var folderPermissionGranted = false
    private var permissionAttempts = 0
    private val maxPermissionAttempts = 3
    private var permissionMaxAttemptsReached = false
    
    // 3 steps: permissions, folder picker, welcome
    private val totalSteps: Int = 3

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
    }

    private fun initializeViews() {
        viewPager = findViewById(R.id.onboardingViewPager)
        stepCounter = findViewById(R.id.stepCounter)
        backButton = findViewById(R.id.backButton)
        nextButton = findViewById(R.id.nextButton)
        step1Container = findViewById(R.id.step1Container)
        step2Container = findViewById(R.id.step2Container)
        step3Container = findViewById(R.id.step3Container)

        backButton.setOnClickListener { moveToPreviousStep() }
        nextButton.setOnClickListener { handleNextButtonClick() }
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
        updateStepIndicatorState(step3Container, 2, false) // Welcome step is always last
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
            1 -> nextButton.text = "Pick .Statuses Folder"
            2 -> nextButton.text = "Get Started"
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
            1 -> pickFolder()
            2 -> completeOnboarding()
        }
    }

    private fun requestStoragePermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Request media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 and below (API < 33) - Request storage permissions
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
        if (storagePermissionGranted && folderPermissionGranted) {
            preferenceUtils.setOnboardingCompleted(true)
            navigateToMainActivity()
        } else {
            when {
                !storagePermissionGranted -> {
                    if (permissionMaxAttemptsReached) {
                        Toast.makeText(this, "Please grant permissions manually in Settings", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Please grant storage permissions first", Toast.LENGTH_SHORT).show()
                    }
                }
                !folderPermissionGranted -> Toast.makeText(this, "Please pick the .Statuses folder first", Toast.LENGTH_SHORT).show()
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
        
        return storagePermissionsGranted
    }

    private fun restoreStepStates() {
        if (checkStoragePermissions()) {
            storagePermissionGranted = true
        } else {
            if (permissionAttempts > 0) {
                val permanentlyDenied = !shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
                val exceededMaxAttempts = permissionAttempts >= maxPermissionAttempts
                if (permanentlyDenied || exceededMaxAttempts) {
                    permissionMaxAttemptsReached = true
                }
            }
        }
        
        // Check if folder permission is granted
        if (storagePermissionGranted) {
            folderPermissionGranted = true
        }
        
        currentStep = when {
            !storagePermissionGranted -> 0
            !folderPermissionGranted -> 1
            else -> 2
        }
    }

    private fun checkStoragePermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Check media permissions
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            // Android 12 and below (API < 33) - Check storage permissions
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
            2 -> true // Welcome step is always considered complete
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
            1 -> updateFolderContent()
            2 -> updateWelcomeContent()
        }
    }

    private fun updatePermissionsContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun updateFolderContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun updateWelcomeContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    private fun pickFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        folderSelectionLauncher.launch(intent)
    }

    // ViewPager Adapter for onboarding steps
    private inner class OnboardingAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
        
        private val layouts = listOf(
            R.layout.step_permissions,
            R.layout.step_folder_picker,
            R.layout.step_welcome
        )

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OnboardingViewHolder {
            val view = layoutInflater.inflate(viewType, parent, false)
            return OnboardingViewHolder(view)
        }

        override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            when (position) {
                0 -> bindPermissionsStep(holder.itemView)
                1 -> bindFolderStep(holder.itemView)
                2 -> bindWelcomeStep(holder.itemView)
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
                    descriptionText?.text = "Thank you for granting permissions. The app will now automatically detect and access your WhatsApp .Statuses folder."
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
                    descriptionText?.text = "We need media permissions to access your WhatsApp .Statuses folder and save statuses. The app will automatically detect the correct folder location."
                    manualStepsLayout?.visibility = View.GONE
                    descriptionText?.visibility = View.VISIBLE
                }
            }
        }

        private fun bindFolderStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            if (folderPermissionGranted) {
                titleText?.text = "Folder Selected!"
                descriptionText?.text = "Great! You've selected the WhatsApp .Statuses folder. You can now proceed to save your statuses."
            } else {
                titleText?.text = "Pick .Statuses Folder"
                descriptionText?.text = "We need access to your WhatsApp .Statuses folder to save your statuses. Please pick the folder using the system folder picker."
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
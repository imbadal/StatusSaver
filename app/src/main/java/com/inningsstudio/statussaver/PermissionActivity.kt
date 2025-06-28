package com.inningsstudio.statussaver

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
import androidx.viewpager2.widget.ViewPager2
import com.inningsstudio.statussaver.Const.STATUS_URI
import android.widget.LinearLayout
import android.widget.FrameLayout
import android.widget.ImageView
import android.util.TypedValue

class PermissionActivity : ComponentActivity() {

    private lateinit var preferenceUtils: PreferenceUtils
    private lateinit var viewPager: ViewPager2
    private lateinit var stepCounter: TextView
    private lateinit var backButton: ImageView
    private lateinit var nextButton: TextView
    private lateinit var step1Container: LinearLayout
    private lateinit var step2Container: LinearLayout
    private lateinit var step3Container: LinearLayout
    
    private var currentStep = 0
    private var folderPermissionGranted = false
    private var storagePermissionGranted = false
    private var permissionAttempts = 0
    private val maxPermissionAttempts = 3
    private var permissionMaxAttemptsReached = false

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
                Toast.makeText(this, "Folder access granted successfully!", Toast.LENGTH_SHORT).show()
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
            updateStepIndicator()
            updateButtonText()
            updateStepContent()
            Toast.makeText(this, "Storage permissions granted successfully!", Toast.LENGTH_SHORT).show()
        } else {
            permissionAttempts++
            if (permissionAttempts >= maxPermissionAttempts) {
                permissionMaxAttemptsReached = true
                updateStepIndicator()
                updateButtonText()
                updateStepContent()
                showManualPermissionDialog()
            } else {
                updateStepContent()
                Toast.makeText(this, "Permissions required to continue. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set status bar color to match the gradient background
        window.statusBarColor = resources.getColor(R.color.primary_color, null)
        
        setContentView(R.layout.activity_permission)

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
        updateStepIndicatorState(step1Container, 0, folderPermissionGranted)
        updateStepIndicatorState(step2Container, 1, storagePermissionGranted)
        updateStepIndicatorState(step3Container, 2, false) // Step 3 is always last
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
        stepCounter.text = "Step ${currentStep + 1} of 3"
    }

    private fun updateButtonText() {
        backButton.visibility = if (currentStep > 0) View.VISIBLE else View.GONE
        
        when (currentStep) {
            0 -> {
                if (folderPermissionGranted) {
                    nextButton.text = "Next"
                } else {
                    nextButton.text = "Select Folder"
                }
            }
            1 -> {
                when {
                    storagePermissionGranted -> nextButton.text = "Next"
                    permissionMaxAttemptsReached -> nextButton.text = "Open Settings"
                    else -> nextButton.text = "Grant Permissions"
                }
            }
            2 -> nextButton.text = "Get Started"
        }
    }

    private fun handleNextButtonClick() {
        when (currentStep) {
            0 -> {
                if (!folderPermissionGranted) {
                    requestFolderPermission()
                } else {
                    moveToNextStep()
                }
            }
            1 -> {
                when {
                    storagePermissionGranted -> moveToNextStep()
                    permissionMaxAttemptsReached -> openAppSettings()
                    else -> requestStoragePermissions()
                }
            }
            2 -> completeOnboarding()
        }
    }

    private fun requestFolderPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra("android.content.extra.SHOW_ADVANCE", true)
            folderSelectionLauncher.launch(intent)
        } else {
            // For older Android versions, just proceed to next step
            // as we'll rely on storage permissions instead
            Toast.makeText(this, "Folder access will be handled via storage permissions", Toast.LENGTH_SHORT).show()
            folderPermissionGranted = true
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
            permissionLauncher.launch(permissionsToRequest)
        }
    }

    private fun showManualPermissionDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Manual Permission Required")
            .setMessage("We couldn't get permissions automatically. Please follow these steps:\n\n" +
                    "1. Tap 'Open Settings' below\n" +
                    "2. Scroll down and tap 'Permissions'\n" +
                    "3. Enable 'Storage' and 'Photos & Videos'\n" +
                    "4. Return to the app and tap 'Next'")
            .setPositiveButton("Open Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }

    private fun moveToNextStep() {
        if (currentStep < 2 && isStepCompleted(currentStep)) {
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
        // Only mark onboarding as completed if all steps are done
        if (folderPermissionGranted && storagePermissionGranted) {
            preferenceUtils.setOnboardingCompleted(true)
            navigateToMainActivity()
        } else {
            // If not all steps are completed, show appropriate message
            when {
                !folderPermissionGranted -> Toast.makeText(this, "Please select a folder first", Toast.LENGTH_SHORT).show()
                !storagePermissionGranted -> {
                    if (permissionMaxAttemptsReached) {
                        Toast.makeText(this, "Please grant permissions manually in Settings", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Please grant storage permissions first", Toast.LENGTH_SHORT).show()
                    }
                }
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
        intent.putExtra(STATUS_URI, statusUri)
        startActivity(intent)
        finish()
    }

    private fun isOnboardingFullyCompleted(): Boolean {
        val uriTree = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
        
        // Check if both folder permission and storage permission are granted
        val hasFolderPermission = !uriTree.isNullOrBlank()
        val hasStoragePermission = checkStoragePermissions()
        
        return onboardingCompleted && hasFolderPermission && hasStoragePermission
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
        }

        // Determine which step to show
        when {
            !folderPermissionGranted -> currentStep = 0
            !storagePermissionGranted -> currentStep = 1
            else -> currentStep = 2
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

    private fun isStepCompleted(stepIndex: Int): Boolean {
        return when (stepIndex) {
            0 -> folderPermissionGranted
            1 -> storagePermissionGranted
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
            0 -> updateFolderSelectionContent()
            1 -> updatePermissionsContent()
            2 -> updateWelcomeContent()
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

    private fun updateWelcomeContent() {
        // This will be handled by the ViewPager adapter
        // We'll need to refresh the current view
        viewPager.adapter?.notifyItemChanged(currentStep)
    }

    // ViewPager Adapter for onboarding steps
    private inner class OnboardingAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {
        
        private val layouts = listOf(
            R.layout.step_folder_selection,
            R.layout.step_permissions,
            R.layout.step_welcome
        )

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(layouts[viewType], parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            when (position) {
                0 -> bindFolderSelectionStep(holder.itemView)
                1 -> bindPermissionsStep(holder.itemView)
                2 -> bindWelcomeStep(holder.itemView)
            }
        }

        private fun bindFolderSelectionStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            if (folderPermissionGranted) {
                titleText?.text = "Folder Access Granted!"
                descriptionText?.text = "Thank you for granting folder access. You can now proceed to the next step to complete the setup."
            } else {
                titleText?.text = "Access WhatsApp Statuses"
                descriptionText?.text = "We need access to your WhatsApp status folder to save statuses. Please select the WhatsApp status folder to continue."
            }
        }

        private fun bindPermissionsStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            when {
                storagePermissionGranted -> {
                    titleText?.text = "Permissions Granted!"
                    descriptionText?.text = "Thank you for granting storage permissions. You can now proceed to complete the onboarding process."
                }
                permissionMaxAttemptsReached -> {
                    titleText?.text = "Manual Permission Required"
                    descriptionText?.text = "We couldn't get permissions automatically. Please open Settings and manually grant storage permissions to continue."
                }
                else -> {
                    titleText?.text = "Grant Permissions"
                    descriptionText?.text = "We need storage permissions to save your WhatsApp statuses to your device. Please grant the required permissions."
                }
            }
        }

        private fun bindWelcomeStep(view: View) {
            val titleText = view.findViewById<TextView>(R.id.stepTitle)
            val descriptionText = view.findViewById<TextView>(R.id.stepDescription)
            
            titleText?.text = "Welcome to StatusSaver!"
            descriptionText?.text = "You're all set! Now you can save and manage your WhatsApp statuses easily. Tap 'Get Started' to begin saving your WhatsApp statuses!"
        }

        override fun getItemCount(): Int = layouts.size

        override fun getItemViewType(position: Int): Int = position

        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
    }
}
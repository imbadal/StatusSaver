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
    private lateinit var backButton: TextView
    private lateinit var nextButton: TextView
    private lateinit var step1Container: LinearLayout
    private lateinit var step2Container: LinearLayout
    private lateinit var step3Container: LinearLayout
    
    private var currentStep = 0
    private var folderPermissionGranted = false
    private var storagePermissionGranted = false
    private var permissionAttempts = 0
    private val maxPermissionAttempts = 3

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
            moveToNextStep()
        } else {
            permissionAttempts++
            if (permissionAttempts >= maxPermissionAttempts) {
                showManualPermissionDialog()
            } else {
                Toast.makeText(this, "Permissions required to continue", Toast.LENGTH_SHORT).show()
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

        // Restore step states when resuming onboarding
        restoreStepStates()

        initializeViews()
        setupViewPager()
        updateStepIndicator()
        updateButtonText()
        
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
        
        // Add page change callback to prevent swiping without completing current step
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentStep = position
                updateStepIndicator()
                updateButtonText()
                updateStepCounter()
            }
            
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                // Prevent swiping if current step is not completed
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    val currentStepIndex = currentStep
                    if (!isStepCompleted(currentStepIndex)) {
                        // Revert to current step if trying to swipe without completion
                        viewPager.setCurrentItem(currentStepIndex, true)
                    }
                }
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
            0 -> nextButton.text = "Continue"
            1 -> nextButton.text = "Grant Permissions"
            2 -> nextButton.text = "Get Started"
        }
    }

    private fun handleNextButtonClick() {
        when (currentStep) {
            0 -> requestFolderPermission()
            1 -> requestStoragePermissions()
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
            .setTitle("Permissions Required")
            .setMessage("Please manually grant permissions in Settings:\n\n" +
                    "1. Go to Settings > Apps > StatusSaver\n" +
                    "2. Tap 'Permissions'\n" +
                    "3. Enable 'Storage' and 'Photos & Videos'\n" +
                    "4. Return to the app")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }

    private fun moveToNextStep() {
        if (currentStep < 2) {
            currentStep++
            viewPager.currentItem = currentStep
            updateStepIndicator()
        }
    }

    private fun moveToPreviousStep() {
        if (currentStep > 0) {
            currentStep--
            viewPager.currentItem = currentStep
        }
    }

    private fun completeOnboarding() {
        // Only mark onboarding as completed if all steps are done
        if (folderPermissionGranted && storagePermissionGranted) {
            preferenceUtils.setOnboardingCompleted(true)
            navigateToMainActivity()
        } else {
            // If not all steps are completed, show appropriate message
            Toast.makeText(this, "Please complete all steps first", Toast.LENGTH_SHORT).show()
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

    private fun isStepCompleted(stepIndex: Int): Boolean {
        return when (stepIndex) {
            0 -> folderPermissionGranted
            1 -> storagePermissionGranted
            2 -> true // Welcome step is always considered complete
            else -> false
        }
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
            // View binding logic if needed
        }

        override fun getItemCount(): Int = layouts.size

        override fun getItemViewType(position: Int): Int = position

        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView)
    }
}
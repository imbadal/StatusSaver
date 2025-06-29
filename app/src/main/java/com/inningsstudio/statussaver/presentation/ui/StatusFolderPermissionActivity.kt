package com.inningsstudio.statussaver.presentation.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.app.AlertDialog
import java.io.File
import android.os.Build
import android.os.storage.StorageManager
import android.content.Context

class StatusFolderPermissionActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StatusPermission"
        private const val REQUEST_CODE_PICK_FOLDER = 200

        // All possible WhatsApp status paths
        private val WHATSAPP_PATHS = arrayOf(
            "/WhatsApp/Media/.Statuses/",
            "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/",
            "/WhatsApp Business/Media/.Statuses/",
            "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/",
            "/GBWhatsApp/Media/.Statuses/",
            "/parallel_lite/0/WhatsApp/Media/.Statuses/",
            "/parallel_intl/0/WhatsApp/Media/.Statuses/",
            "/DualApp/WhatsApp/Media/.Statuses/",
            "/storage/emulated/999/WhatsApp/Media/.Statuses/",
            "/storage/ace-999/WhatsApp/Media/.Statuses/",
            "/storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/",
            "/DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
        )
    }

    private var statusFolderPath: String? = null
    private var selectedFolderUri: Uri? = null
    private lateinit var pathTextView: TextView
    private lateinit var accessButton: Button

    // Activity result launcher for folder picker
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                Log.d(TAG, "✅ Folder selected: $uri")
                selectedFolderUri = uri
                
                // Take persistent permission (READ ONLY)
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                
                Toast.makeText(this, "Folder access granted", Toast.LENGTH_SHORT).show()
                proceedToMainActivity()
            }
        } else {
            Log.d(TAG, "❌ Folder selection cancelled or failed")
            Toast.makeText(this, "Folder access is required to read WhatsApp statuses", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== STARTING STATUS PERMISSION ACTIVITY ===")

        // Set up simple UI
        setupUI()
        
        // Step 1: Search for the .Statuses folder and display the URL
        findAndDisplayStatusFolder()
    }

    private fun setupUI() {
        // Create a simple layout programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        // Title
        val titleText = TextView(this).apply {
            text = "WhatsApp Status Folder Access"
            textSize = 20f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(titleText)

        // Path display
        pathTextView = TextView(this).apply {
            text = "Searching for WhatsApp status folder..."
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }
        layout.addView(pathTextView)

        // Access button
        accessButton = Button(this).apply {
            text = "Get Folder Access"
            isEnabled = false
            setOnClickListener {
                // Step 3: Open that particular folder for access
                openFolderForAccess()
            }
        }
        layout.addView(accessButton)

        setContentView(layout)
    }

    private fun findAndDisplayStatusFolder() {
        Log.d(TAG, "=== STEP 1: SEARCHING FOR .STATUSES FOLDER ===")
        
        statusFolderPath = getAvailableWhatsAppPath()

        if (statusFolderPath != null) {
            Log.d(TAG, "✅ Status folder found: $statusFolderPath")
            
            // Display the found path
            pathTextView.text = "Found WhatsApp status folder:\n\n📁 $statusFolderPath"
            
            // Step 2: Enable the button to get access
            accessButton.isEnabled = true
            accessButton.text = "Get Access to This Folder"
            
        } else {
            Log.w(TAG, "❌ No WhatsApp .Statuses folder found")
            pathTextView.text = "❌ No WhatsApp status folder found\n\nPlease make sure WhatsApp is installed and has status files."
            accessButton.isEnabled = false
        }
    }

    private fun getAvailableWhatsAppPath(): String? {
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        Log.d(TAG, "Base storage path: $basePath")

        // Check each possible path
        for (path in WHATSAPP_PATHS) {
            val fullPath = if (path.startsWith("/storage/")) {
                path
            } else {
                basePath + path
            }

            val folder = File(fullPath)
            Log.d(TAG, "Checking path: $fullPath")

            if (folder.exists() && folder.isDirectory()) {
                Log.d(TAG, "✅ Valid path found: $fullPath")
                return fullPath
            }
        }

        return null
    }

    private fun openFolderForAccess() {
        Log.d(TAG, "=== STEP 3: OPENING FOLDER FOR ACCESS ===")
        
        statusFolderPath?.let { path ->
            Log.d(TAG, "Opening folder for access: $path")

            // Show dialog with instructions before launching picker
            AlertDialog.Builder(this)
                .setTitle("Grant WhatsApp Status Folder Access")
                .setMessage(
                    "If the next screen does not open the .Statuses folder, please navigate to:\n\n" +
                    "Android > media > com.whatsapp > WhatsApp > Media > .Statuses\n\n" +
                    "and select it. This is required to access WhatsApp statuses."
                )
                .setPositiveButton("Continue") { _, _ ->
                    try {
                        val targetPath = "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Android 11+ StorageManager approach
                            val sm = getSystemService(Context.STORAGE_SERVICE) as StorageManager
                            val storageVolume = sm.primaryStorageVolume
                            var intent = storageVolume.createOpenDocumentTreeIntent()
                            val initialUri = intent.getParcelableExtra<Uri>(DocumentsContract.EXTRA_INITIAL_URI)
                            val replace = initialUri.toString().replace("/root/", "/document/")
                            val finalUri = Uri.parse("$replace%3A" + targetPath.replace("/", "%2F"))
                            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, finalUri)
                            intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            Log.d(TAG, "Launching StorageManager intent with patched URI: $finalUri")
                            folderPickerLauncher.launch(intent)
                        } else {
                            // Fallback for older versions
                            val treeUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A" + Uri.encode(targetPath))
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                putExtra(DocumentsContract.EXTRA_INITIAL_URI, treeUri)
                                putExtra("android.content.extra.SHOW_ADVANCED", true)
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            }
                            Log.d(TAG, "Launching fallback intent with hardcoded URI: $treeUri")
                            folderPickerLauncher.launch(intent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error launching picker with advanced URI", e)
                        // Fallback: show manual instructions
                        showManualInstructions(path)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showManualInstructions(folderPath: String) {
        Log.d(TAG, "Showing manual instructions for path: $folderPath")
        
        val message = """
            Please manually navigate to this folder:
            
            📁 $folderPath
            
            This is where WhatsApp stores status files.
        """.trimIndent()
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Launch folder picker without initial URI
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        folderPickerLauncher.launch(intent)
    }

    private fun proceedToMainActivity() {
        Log.d(TAG, "=== PROCEEDING TO MAIN ACTIVITY ===")
        Log.d(TAG, "Status folder path: $statusFolderPath")
        Log.d(TAG, "Selected folder URI: $selectedFolderUri")

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("STATUS_FOLDER_PATH", statusFolderPath)
            putExtra("STATUS_FOLDER_URI", selectedFolderUri?.toString())
        }
        startActivity(intent)
        finish()
    }
}
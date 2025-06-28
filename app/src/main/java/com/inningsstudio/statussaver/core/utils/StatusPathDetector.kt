package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

object StatusPathDetector {
    
    private const val TAG = "StatusPathDetector"
    
    // Updated paths for Android 15 and Samsung devices
    private val STATUS_PATHS = listOf(
        // Primary Android 15 paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Alternative paths
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Media/Statuses",
        "WhatsApp/Statuses",
        
        // Samsung-specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Internal storage paths (for some devices)
        "0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "0/Android/data/com.whatsapp/files/Statuses",
        
        // Legacy paths (for older devices)
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Statuses",
        
        // WhatsApp Business paths
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "Android/data/com.whatsapp.w4b/files/Statuses",
        "WhatsApp Business/Media/.Statuses",
        "WhatsApp Business/Statuses"
    )
    
    data class StatusPathInfo(
        val path: String,
        val appName: String,
        val exists: Boolean,
        val hasFiles: Boolean,
        val fileCount: Int
    )
    
    /**
     * Detects all possible WhatsApp status paths and returns information about each
     */
    fun detectAllStatusPaths(context: Context): List<StatusPathInfo> {
        val allPaths = mutableListOf<StatusPathInfo>()
        
        // Check external storage paths (primary storage)
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val externalStoragePath = externalStorageDir.absolutePath
        
        Log.d(TAG, "Checking external storage path: $externalStoragePath")
        
        // Check official WhatsApp paths
        allPaths.addAll(checkPathsForApp(externalStoragePath, STATUS_PATHS, "WhatsApp"))
        
        // Check internal storage paths (some devices store data here)
        val internalStorageDir = context.filesDir.parentFile
        if (internalStorageDir != null) {
            val internalStoragePath = internalStorageDir.absolutePath
            Log.d(TAG, "Checking internal storage path: $internalStoragePath")
            allPaths.addAll(checkPathsForApp(internalStoragePath, STATUS_PATHS, "WhatsApp (Internal)"))
        }
        
        // Check if there's an SD card
        val sdCardDir = getSDCardDirectory(context)
        if (sdCardDir != null) {
            val sdCardPath = sdCardDir.absolutePath
            Log.d(TAG, "Checking SD card path: $sdCardPath")
            allPaths.addAll(checkPathsForApp(sdCardPath, STATUS_PATHS, "WhatsApp (SD Card)"))
        }
        
        Log.d(TAG, "Found ${allPaths.size} potential status paths")
        return allPaths
    }
    
    /**
     * Gets the best available status path (one that exists and has files)
     */
    fun getBestStatusPath(context: Context): String? {
        Log.d(TAG, "=== STATUS PATH DETECTION STARTED ===")
        Log.d(TAG, "Android version: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        
        val externalStorageDir = Environment.getExternalStorageDirectory()
        Log.d(TAG, "External storage directory: $externalStorageDir")
        
        // Check if external storage is available
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w(TAG, "External storage is not mounted")
            return null
        }
        
        for (path in STATUS_PATHS) {
            val fullPath = "$externalStorageDir/$path"
            Log.d(TAG, "Checking path: $fullPath")
            
            val directory = File(fullPath)
            if (directory.exists() && directory.isDirectory) {
                Log.d(TAG, "Found existing directory: $fullPath")
                
                // Check if directory is readable
                if (directory.canRead()) {
                    Log.d(TAG, "Directory is readable: $fullPath")
                    
                    // List files to verify it's not empty
                    val files = directory.listFiles()
                    if (files != null && files.isNotEmpty()) {
                        Log.d(TAG, "Directory contains ${files.size} files")
                        
                        // Log some file names for debugging
                        files.take(5).forEach { file ->
                            Log.d(TAG, "Sample file: ${file.name} (hidden: ${file.isHidden})")
                        }
                        
                        return fullPath
                    } else {
                        Log.d(TAG, "Directory is empty: $fullPath")
                    }
                } else {
                    Log.w(TAG, "Directory is not readable: $fullPath")
                }
            } else {
                Log.d(TAG, "Directory does not exist: $fullPath")
            }
        }
        
        // Try alternative storage locations
        val alternativePaths = listOf(
            "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "/storage/emulated/0/WhatsApp/Media/.Statuses",
            "/sdcard/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "/sdcard/WhatsApp/Media/.Statuses"
        )
        
        for (path in alternativePaths) {
            Log.d(TAG, "Checking alternative path: $path")
            val directory = File(path)
            if (directory.exists() && directory.isDirectory && directory.canRead()) {
                val files = directory.listFiles()
                if (files != null && files.isNotEmpty()) {
                    Log.d(TAG, "Found statuses in alternative path: $path")
                    return path
                }
            }
        }
        
        Log.w(TAG, "No valid WhatsApp status path found")
        return null
    }
    
    /**
     * Debug function to find any WhatsApp-related directories
     */
    private fun debugWhatsAppDirectories(context: Context) {
        Log.d(TAG, "=== DEBUGGING WHATSAPP DIRECTORIES ===")
        
        val externalStorageDir = Environment.getExternalStorageDirectory()
        Log.d(TAG, "External storage: ${externalStorageDir.absolutePath}")
        
        // Check for WhatsApp directory
        val whatsappDir = File(externalStorageDir, "WhatsApp")
        if (whatsappDir.exists()) {
            Log.d(TAG, "WhatsApp directory exists: ${whatsappDir.absolutePath}")
            listDirectoryContents(whatsappDir, "WhatsApp")
        } else {
            Log.d(TAG, "WhatsApp directory does not exist")
        }
        
        // Check for Android/media/com.whatsapp
        val androidMediaWhatsapp = File(externalStorageDir, "Android/media/com.whatsapp")
        if (androidMediaWhatsapp.exists()) {
            Log.d(TAG, "Android/media/com.whatsapp exists: ${androidMediaWhatsapp.absolutePath}")
            listDirectoryContents(androidMediaWhatsapp, "Android/media/com.whatsapp")
        } else {
            Log.d(TAG, "Android/media/com.whatsapp does not exist")
        }
        
        // Check for Android/data/com.whatsapp
        val androidDataWhatsapp = File(externalStorageDir, "Android/data/com.whatsapp")
        if (androidDataWhatsapp.exists()) {
            Log.d(TAG, "Android/data/com.whatsapp exists: ${androidDataWhatsapp.absolutePath}")
            listDirectoryContents(androidDataWhatsapp, "Android/data/com.whatsapp")
        } else {
            Log.d(TAG, "Android/data/com.whatsapp does not exist")
        }
        
        Log.d(TAG, "=== END DEBUGGING ===")
    }
    
    private fun listDirectoryContents(directory: File, prefix: String) {
        try {
            val files = directory.listFiles()
            if (files != null) {
                files.forEach { file ->
                    Log.d(TAG, "$prefix: ${file.name} (${if (file.isDirectory) "dir" else "file"})")
                    if (file.isDirectory && (file.name.contains("Media") || file.name.contains("Status"))) {
                        listDirectoryContents(file, "$prefix/${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing directory: ${directory.absolutePath}", e)
        }
    }
    
    /**
     * Checks if WhatsApp is installed
     */
    fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if WhatsApp Business is installed
     */
    fun isWhatsAppBusinessInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the device manufacturer
     */
    fun getDeviceManufacturer(): String {
        return Build.MANUFACTURER.lowercase()
    }
    
    /**
     * Gets the Android version
     */
    fun getAndroidVersion(): String {
        return Build.VERSION.RELEASE
    }
    
    private fun checkPathsForApp(basePath: String, paths: List<String>, appName: String): List<StatusPathInfo> {
        val results = mutableListOf<StatusPathInfo>()
        
        for (path in paths) {
            val fullPath = "$basePath/$path"
            val file = File(fullPath)
            
            val exists = file.exists() && file.isDirectory
            val hasFiles = if (exists) {
                val files = file.listFiles()
                files != null && files.isNotEmpty()
            } else false
            
            val fileCount = if (exists) {
                val files = file.listFiles()
                files?.size ?: 0
            } else 0
            
            // Additional debugging for the most common paths
            if (path.contains("Android/media/com.whatsapp") || path.contains("WhatsApp/Media")) {
                Log.d(TAG, "=== DETAILED DEBUG FOR: $fullPath ===")
                Log.d(TAG, "File exists: ${file.exists()}")
                Log.d(TAG, "Is directory: ${file.isDirectory}")
                Log.d(TAG, "Can read: ${file.canRead()}")
                Log.d(TAG, "Can write: ${file.canWrite()}")
                Log.d(TAG, "Absolute path: ${file.absolutePath}")
                
                if (file.exists()) {
                    try {
                        val files = file.listFiles()
                        Log.d(TAG, "Files in directory: ${files?.size ?: 0}")
                        files?.take(5)?.forEach { f ->
                            Log.d(TAG, "  - ${f.name} (${if (f.isDirectory) "dir" else "file"})")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error listing directory contents: ${file.absolutePath}", e)
                    }
                }
                Log.d(TAG, "=== END DETAILED DEBUG ===")
            }
            
            results.add(StatusPathInfo(fullPath, appName, exists, hasFiles, fileCount))
            
            Log.d(TAG, "Path: $fullPath, Exists: $exists, HasFiles: $hasFiles, FileCount: $fileCount")
        }
        
        return results
    }
    
    private fun getSDCardDirectory(context: Context): File? {
        return try {
            // Try to get SD card directory
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val externalDirs = context.getExternalFilesDirs(null)
                if (externalDirs.isNotEmpty() && externalDirs.size > 1) {
                    // First one is usually internal, second might be SD card
                    externalDirs[1]?.parentFile
                } else null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if the app has necessary permissions to access WhatsApp status
     */
    fun checkPermissions(context: Context): Boolean {
        Log.d(TAG, "=== CHECKING PERMISSIONS ===")
        
        // Check if we can access external storage
        val externalStorageDir = Environment.getExternalStorageDirectory()
        Log.d(TAG, "External storage accessible: ${externalStorageDir.canRead()}")
        Log.d(TAG, "External storage path: ${externalStorageDir.absolutePath}")
        
        // Check if we can access WhatsApp directory
        val whatsappDir = File(externalStorageDir, "WhatsApp")
        Log.d(TAG, "WhatsApp directory accessible: ${whatsappDir.canRead()}")
        
        // Check if we can access Android/media/com.whatsapp
        val androidMediaWhatsapp = File(externalStorageDir, "Android/media/com.whatsapp")
        Log.d(TAG, "Android/media/com.whatsapp accessible: ${androidMediaWhatsapp.canRead()}")
        
        // Check if we can access Android/data/com.whatsapp
        val androidDataWhatsapp = File(externalStorageDir, "Android/data/com.whatsapp")
        Log.d(TAG, "Android/data/com.whatsapp accessible: ${androidDataWhatsapp.canRead()}")
        
        Log.d(TAG, "=== END PERMISSIONS CHECK ===")
        
        return externalStorageDir.canRead()
    }
    
    /**
     * Checks if there are any WhatsApp statuses available
     */
    fun checkWhatsAppStatuses(context: Context): Boolean {
        Log.d(TAG, "=== CHECKING WHATSAPP STATUSES ===")
        
        val externalStorageDir = Environment.getExternalStorageDirectory()
        
        // Check the most common status directory for received statuses
        val statusDir = File(externalStorageDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
        Log.d(TAG, "Status directory exists: ${statusDir.exists()}")
        Log.d(TAG, "Status directory path: ${statusDir.absolutePath}")
        
        if (statusDir.exists()) {
            // List all files including hidden ones
            val allFiles = statusDir.listFiles { file ->
                true // Accept all files to see received statuses
            }
            Log.d(TAG, "Number of files in status directory (including hidden): ${allFiles?.size ?: 0}")
            
            if (!allFiles.isNullOrEmpty()) {
                Log.d(TAG, "Received status files found:")
                allFiles.take(10).forEach { file ->
                    Log.d(TAG, "  - ${file.name} (${file.length()} bytes, hidden: ${file.isHidden})")
                }
                return true
            } else {
                Log.d(TAG, "Status directory is empty - no received statuses found")
            }
        } else {
            Log.d(TAG, "Status directory does not exist")
        }
        
        // Check alternative locations for received statuses
        val alternativePaths = listOf(
            "WhatsApp/Media/.Statuses",
            "WhatsApp/Media/Statuses",
            "Android/data/com.whatsapp/files/Statuses",
            "Android/data/com.whatsapp/files/.Statuses"
        )
        
        alternativePaths.forEach { path ->
            val altDir = File(externalStorageDir, path)
            if (altDir.exists()) {
                val files = altDir.listFiles { file ->
                    true // Accept all files
                }
                Log.d(TAG, "Alternative path $path has ${files?.size ?: 0} files")
                if (!files.isNullOrEmpty()) {
                    files.take(5).forEach { file ->
                        Log.d(TAG, "  - ${file.name} (${file.length()} bytes, hidden: ${file.isHidden})")
                    }
                }
            }
        }
        
        Log.d(TAG, "=== END WHATSAPP STATUSES CHECK ===")
        return false
    }
    
    /**
     * Checks specifically for received WhatsApp statuses from contacts
     */
    fun checkReceivedStatuses(context: Context): Boolean {
        Log.d(TAG, "=== CHECKING FOR RECEIVED STATUSES FROM CONTACTS ===")
        
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val statusDir = File(externalStorageDir, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses")
        
        if (statusDir.exists()) {
            Log.d(TAG, "Found status directory: ${statusDir.absolutePath}")
            
            // List all files to see received statuses
            val allFiles = statusDir.listFiles { file ->
                true // Accept all files including hidden ones
            }
            
            if (!allFiles.isNullOrEmpty()) {
                Log.d(TAG, "Found ${allFiles.size} received status files:")
                allFiles.forEach { file ->
                    Log.d(TAG, "  Status: ${file.name} (${file.length()} bytes, hidden: ${file.isHidden})")
                }
                return true
            } else {
                Log.d(TAG, "No received statuses found - directory is empty")
                Log.d(TAG, "Note: Statuses expire after 24 hours and are automatically deleted by WhatsApp")
            }
        } else {
            Log.d(TAG, "Status directory not found")
        }
        
        Log.d(TAG, "=== END RECEIVED STATUSES CHECK ===")
        return false
    }
} 
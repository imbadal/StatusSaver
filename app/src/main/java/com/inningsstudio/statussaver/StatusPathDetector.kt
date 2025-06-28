package com.inningsstudio.statussaver

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

object StatusPathDetector {
    
    private const val TAG = "StatusPathDetector"
    
    // Common WhatsApp status paths across different devices and OS versions
    private val WHATSAPP_STATUS_PATHS = listOf(
        // Standard WhatsApp paths (Android 10+)
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Standard WhatsApp paths (Android 9 and below)
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Media/WhatsApp Statuses",
        "WhatsApp/Media/Statuses",
        "WhatsApp/Statuses",
        
        // Samsung specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Samsung/WhatsApp/Media/.Statuses",
        "Samsung/WhatsApp/Statuses",
        
        // Xiaomi specific paths
        "MIUI/Media/WhatsApp/Media/.Statuses",
        "MIUI/Media/WhatsApp/Statuses",
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // OnePlus specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "OnePlus/WhatsApp/Media/.Statuses",
        "OnePlus/WhatsApp/Statuses",
        
        // Huawei specific paths
        "Huawei/Media/WhatsApp/Media/.Statuses",
        "Huawei/Media/WhatsApp/Statuses",
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Oppo/Vivo specific paths
        "ColorOS/Media/WhatsApp/Media/.Statuses",
        "ColorOS/Media/WhatsApp/Statuses",
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Motorola specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Motorola/WhatsApp/Media/.Statuses",
        "Motorola/WhatsApp/Statuses",
        
        // LG specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "LG/WhatsApp/Media/.Statuses",
        "LG/WhatsApp/Statuses",
        
        // Sony specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Sony/WhatsApp/Media/.Statuses",
        "Sony/WhatsApp/Statuses",
        
        // Nokia specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Nokia/WhatsApp/Media/.Statuses",
        "Nokia/WhatsApp/Statuses",
        
        // Realme specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Realme/WhatsApp/Media/.Statuses",
        "Realme/WhatsApp/Statuses",
        
        // Poco specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Poco/WhatsApp/Media/.Statuses",
        "Poco/WhatsApp/Statuses",
        
        // Legacy paths for older Android versions
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Statuses",
        
        // Internal storage variations
        "0/WhatsApp/Media/.Statuses",
        "0/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "0/Android/data/com.whatsapp/files/Statuses",
        
        // SD card variations (if available)
        "1/WhatsApp/Media/.Statuses",
        "1/Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "1/Android/data/com.whatsapp/files/Statuses",
        
        // Additional variations for newer Android versions
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Some devices use different folder names
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Media/Statuses",
        "WhatsApp/Statuses",
        
        // Additional manufacturer-specific paths
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        "Android/media/com.whatsapp/WhatsApp/Media/Statuses",
        "Android/data/com.whatsapp/files/Statuses",
        
        // Some devices use different folder names
        "WhatsApp/Media/.Statuses",
        "WhatsApp/Media/Statuses",
        "WhatsApp/Statuses",
        
        // Additional variations for newer Android versions
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/data/com.whatsapp/files/Statuses"
    )
    
    // WhatsApp Business paths
    private val WHATSAPP_BUSINESS_STATUS_PATHS = listOf(
        "WhatsApp Business/Media/.Statuses",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "Android/data/com.whatsapp.w4b/files/Statuses",
        "WhatsApp Business/Media/Statuses",
        "WhatsApp Business/Statuses",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses",
        "Android/data/com.whatsapp.w4b/files/Statuses"
    )
    
    // GBWhatsApp paths (if installed)
    private val GBWHATSAPP_STATUS_PATHS = listOf(
        "GBWhatsApp/Media/.Statuses",
        "Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses",
        "Android/data/com.gbwhatsapp/files/Statuses",
        "GBWhatsApp/Media/Statuses",
        "GBWhatsApp/Statuses",
        "Android/media/com.gbwhatsapp/GBWhatsApp/Media/Statuses",
        "Android/data/com.gbwhatsapp/files/Statuses"
    )
    
    // YoWhatsApp paths (if installed)
    private val YOWHATSAPP_STATUS_PATHS = listOf(
        "YoWhatsApp/Media/.Statuses",
        "Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses",
        "Android/data/com.yowhatsapp/files/Statuses",
        "YoWhatsApp/Media/Statuses",
        "YoWhatsApp/Statuses",
        "Android/media/com.yowhatsapp/YoWhatsApp/Media/Statuses",
        "Android/data/com.yowhatsapp/files/Statuses"
    )
    
    // Additional WhatsApp mods
    private val WHATSAPP_MOD_STATUS_PATHS = listOf(
        // FMWhatsApp
        "FMWhatsApp/Media/.Statuses",
        "Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses",
        "Android/data/com.fmwhatsapp/files/Statuses",
        
        // WhatsApp Plus
        "WhatsApp Plus/Media/.Statuses",
        "Android/media/com.whatsapp.plus/WhatsApp Plus/Media/.Statuses",
        "Android/data/com.whatsapp.plus/files/Statuses",
        
        // Fouad WhatsApp
        "Fouad WhatsApp/Media/.Statuses",
        "Android/media/com.fouad.whatsapp/Fouad WhatsApp/Media/.Statuses",
        "Android/data/com.fouad.whatsapp/files/Statuses"
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
        
        // Check external storage paths
        val externalStorageDir = Environment.getExternalStorageDirectory()
        val externalStoragePath = externalStorageDir.absolutePath
        
        // Check all WhatsApp variants
        allPaths.addAll(checkPathsForApp(externalStoragePath, WHATSAPP_STATUS_PATHS, "WhatsApp"))
        allPaths.addAll(checkPathsForApp(externalStoragePath, WHATSAPP_BUSINESS_STATUS_PATHS, "WhatsApp Business"))
        allPaths.addAll(checkPathsForApp(externalStoragePath, GBWHATSAPP_STATUS_PATHS, "GBWhatsApp"))
        allPaths.addAll(checkPathsForApp(externalStoragePath, YOWHATSAPP_STATUS_PATHS, "YoWhatsApp"))
        allPaths.addAll(checkPathsForApp(externalStoragePath, WHATSAPP_MOD_STATUS_PATHS, "WhatsApp Mod"))
        
        // Check internal storage paths
        val internalStorageDir = context.filesDir.parentFile
        if (internalStorageDir != null) {
            val internalStoragePath = internalStorageDir.absolutePath
            allPaths.addAll(checkPathsForApp(internalStoragePath, WHATSAPP_STATUS_PATHS, "WhatsApp (Internal)"))
            allPaths.addAll(checkPathsForApp(internalStoragePath, WHATSAPP_BUSINESS_STATUS_PATHS, "WhatsApp Business (Internal)"))
        }
        
        // Check if there's an SD card
        val sdCardDir = getSDCardDirectory(context)
        if (sdCardDir != null) {
            val sdCardPath = sdCardDir.absolutePath
            allPaths.addAll(checkPathsForApp(sdCardPath, WHATSAPP_STATUS_PATHS, "WhatsApp (SD Card)"))
            allPaths.addAll(checkPathsForApp(sdCardPath, WHATSAPP_BUSINESS_STATUS_PATHS, "WhatsApp Business (SD Card)"))
        }
        
        Log.d(TAG, "Found ${allPaths.size} potential status paths")
        return allPaths
    }
    
    /**
     * Gets the best available status path (one that exists and has files)
     */
    fun getBestStatusPath(context: Context): String? {
        val allPaths = detectAllStatusPaths(context)
        
        // First priority: paths that exist and have files
        val validPaths = allPaths.filter { it.exists && it.hasFiles }
        if (validPaths.isNotEmpty()) {
            // Prefer WhatsApp over other variants
            val whatsappPath = validPaths.find { it.appName.contains("WhatsApp") && !it.appName.contains("Business") && !it.appName.contains("GB") && !it.appName.contains("Yo") && !it.appName.contains("Mod") }
            if (whatsappPath != null) {
                Log.d(TAG, "Using WhatsApp status path: ${whatsappPath.path}")
                return whatsappPath.path
            }
            
            // Fallback to any valid path
            Log.d(TAG, "Using status path: ${validPaths.first().path}")
            return validPaths.first().path
        }
        
        // Second priority: paths that exist but might be empty
        val existingPaths = allPaths.filter { it.exists }
        if (existingPaths.isNotEmpty()) {
            Log.d(TAG, "Using existing but empty path: ${existingPaths.first().path}")
            return existingPaths.first().path
        }
        
        Log.d(TAG, "No valid status paths found")
        return null
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
     * Checks if GBWhatsApp is installed
     */
    fun isGBWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.gbwhatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if YoWhatsApp is installed
     */
    fun isYoWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.yowhatsapp", 0)
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
} 
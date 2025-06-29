package com.inningsstudio.statussaver.core.utils

import android.os.Build
import android.os.Environment
import android.util.Log
import com.inningsstudio.statussaver.data.model.StatusModel
import java.io.File

class WhatsAppStatusReader {
    
    companion object {
        private const val TAG = "WhatsAppStatusReader"
        
        private const val WHATSAPP_STATUS_PATH = "/WhatsApp/Media/.Statuses/"
        private const val WHATSAPP_STATUS_PATH_SCOPED = "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
        private const val WHATSAPP_BUSINESS_STATUS_PATH = "/WhatsApp Business/Media/.Statuses/"
        private const val WHATSAPP_BUSINESS_STATUS_PATH_SCOPED = "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/"
        
        // Additional WhatsApp variants
        private const val WHATSAPP_GB_STATUS_PATH = "/GBWhatsApp/Media/.Statuses/"
        private const val WHATSAPP_GB_STATUS_PATH_SCOPED = "/Android/media/com.gbwhatsapp/GBWhatsApp/Media/.Statuses/"
        private const val WHATSAPP_FM_STATUS_PATH = "/FMWhatsApp/Media/.Statuses/"
        private const val WHATSAPP_FM_STATUS_PATH_SCOPED = "/Android/media/com.fmwhatsapp/FMWhatsApp/Media/.Statuses/"
        private const val WHATSAPP_YO_STATUS_PATH = "/YoWhatsApp/Media/.Statuses/"
        private const val WHATSAPP_YO_STATUS_PATH_SCOPED = "/Android/media/com.yowhatsapp/YoWhatsApp/Media/.Statuses/"
    }
    
    fun readAllStatuses(): List<StatusModel> {
        val statuses = mutableListOf<StatusModel>()
        
        // Get the correct path based on Android version
        val statusPath = getStatusPath()
        if (statusPath != null) {
            val statusFolder = File(statusPath)
            if (statusFolder.exists() && statusFolder.isDirectory()) {
                val files = statusFolder.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (isValidStatusFile(file)) {
                            val status = StatusModel(
                                id = file.absolutePath.hashCode().toLong(),
                                filePath = file.absolutePath,
                                fileName = file.name,
                                fileSize = file.length(),
                                lastModified = file.lastModified(),
                                isVideo = isVideoFile(file)
                            )
                            statuses.add(status)
                            Log.d(TAG, "Found status: ${file.name}")
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "Total statuses found: ${statuses.size}")
        return statuses
    }
    
    private fun getStatusPath(): String? {
        val externalStorage = Environment.getExternalStorageDirectory().absolutePath
        
        // Check all possible paths in order of preference
        val possiblePaths = listOf(
            // Scoped storage paths (Android 13+)
            externalStorage + WHATSAPP_STATUS_PATH_SCOPED,
            externalStorage + WHATSAPP_BUSINESS_STATUS_PATH_SCOPED,
            externalStorage + WHATSAPP_GB_STATUS_PATH_SCOPED,
            externalStorage + WHATSAPP_FM_STATUS_PATH_SCOPED,
            externalStorage + WHATSAPP_YO_STATUS_PATH_SCOPED,
            
            // Legacy paths (Android 12 and below)
            externalStorage + WHATSAPP_STATUS_PATH,
            externalStorage + WHATSAPP_BUSINESS_STATUS_PATH,
            externalStorage + WHATSAPP_GB_STATUS_PATH,
            externalStorage + WHATSAPP_FM_STATUS_PATH,
            externalStorage + WHATSAPP_YO_STATUS_PATH
        )
        
        for (path in possiblePaths) {
            val folder = File(path)
            if (folder.exists() && folder.isDirectory()) {
                Log.d(TAG, "Found WhatsApp status folder: $path")
                return path
            }
        }
        
        Log.w(TAG, "No WhatsApp status folder found")
        return null
    }
    
    private fun isValidStatusFile(file: File): Boolean {
        if (!file.isFile) return false
        
        val name = file.name.lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".mp4") || 
               name.endsWith(".3gp") || name.endsWith(".mkv") ||
               name.endsWith(".webp") || name.endsWith(".gif")
    }
    
    private fun isVideoFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".mp4") || name.endsWith(".3gp") || 
               name.endsWith(".mkv") || name.endsWith(".avi") ||
               name.endsWith(".mov") || name.endsWith(".wmv")
    }
    
    private fun isImageFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".webp") ||
               name.endsWith(".gif") || name.endsWith(".bmp")
    }
    
    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            true // We'll check permissions at runtime
        } else {
            // Android 12 and below
            true // We'll check permissions at runtime
        }
    }
    
    fun getStatusFolderPath(): String? {
        return getStatusPath()
    }
    
    fun isStatusFolderAccessible(): Boolean {
        val path = getStatusPath()
        if (path != null) {
            val folder = File(path)
            return folder.exists() && folder.isDirectory() && folder.canRead()
        }
        return false
    }
} 
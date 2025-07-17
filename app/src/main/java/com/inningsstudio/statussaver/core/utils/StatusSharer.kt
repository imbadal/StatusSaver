package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Handles sharing WhatsApp statuses
 */
object StatusSharer {
    
    private const val TAG = "StatusSharer"

    suspend fun shareStatus(context: Context, currentPath: String) = withContext(Dispatchers.IO) {
        if (isVideo(currentPath)) {
            shareVideo(path = currentPath, context = context)
        } else {
            shareImage(currentPath, context)
        }
    }

    private fun shareVideo(title: String? = "", path: String, context: Context) {
        // SECURITY CHECK: Ensure we only share files from Status Saver directory
        if (!isFileInStatusSaverDirectory(path)) {
            Log.e(TAG, "SECURITY VIOLATION: Attempted to share file outside Status Saver directory: $path")
            return
        }
        
        val isContentUri = path.startsWith("content://")
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "video/*"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        shareIntent.putExtra(Intent.EXTRA_TITLE, title)
        val uri: Uri = if (isContentUri) {
            Uri.parse(path)
        } else {
            // Use FileProvider for file paths
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                File(path)
            )
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(shareIntent, "Share with"))
    }

    private fun shareImage(currentPath: String, context: Context) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/jpeg"
        
        // SECURITY CHECK: Ensure we only share files from Status Saver directory
        if (!isFileInStatusSaverDirectory(currentPath)) {
            Log.e(TAG, "SECURITY VIOLATION: Attempted to share file outside Status Saver directory: $currentPath")
            return
        }
        
        // Use FileProvider for secure sharing
        val uri: Uri = if (currentPath.startsWith("content://")) {
            Uri.parse(currentPath)
        } else {
            // Use FileProvider for file paths
            FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                File(currentPath)
            )
        }
        
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(share, "Share with"))
    }

    private fun isVideo(path: String): Boolean {
        return (path.substring(path.length - 3) == "mp4")
    }
    
    /**
     * SECURITY CHECK: Validates that a file path is within the Status Saver directory
     * This prevents accidental sharing of files from WhatsApp's original .Statuses folder
     */
    private fun isFileInStatusSaverDirectory(filePath: String): Boolean {
        return try {
            // Check if it's a SAF URI (these are safe as they're managed by the app)
            if (filePath.startsWith("content://")) {
                return true
            }
            
            // For regular file paths, check if they're within Status Saver directory
            val file = File(filePath)
            val statusSaverDir = File(StatusSaver.SAVED_DIRECTORY)
            val favoritesDir = File(StatusSaver.FAVOURITES_DIRECTORY)
            
            // Check if file is within Status Saver directory or its subdirectories
            val isInStatusSaver = file.absolutePath.startsWith(statusSaverDir.absolutePath)
            val isInFavorites = file.absolutePath.startsWith(favoritesDir.absolutePath)
            
            Log.d(TAG, "Security check - File: ${file.absolutePath}")
            Log.d(TAG, "Security check - Status Saver Dir: ${statusSaverDir.absolutePath}")
            Log.d(TAG, "Security check - Favorites Dir: ${favoritesDir.absolutePath}")
            Log.d(TAG, "Security check - Is in Status Saver: $isInStatusSaver")
            Log.d(TAG, "Security check - Is in Favorites: $isInFavorites")
            
            isInStatusSaver || isInFavorites
        } catch (e: Exception) {
            Log.e(TAG, "Error in security check", e)
            false // Fail safe - don't allow sharing if we can't verify
        }
    }
} 
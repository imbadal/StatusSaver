package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles general file operations
 */
object FileOperations {
    
    private const val TAG = "FileOperations"

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            // SECURITY CHECK: Ensure we only delete files from Status Saver directory
            if (!isFileInStatusSaverDirectory(path)) {
                Log.e(TAG, "SECURITY VIOLATION: Attempted to delete file outside Status Saver directory: $path")
                return@withContext false
            }
            
            val file = File(path)
            file.exists() && file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $path", e)
            false
        }
    }

    suspend fun copyFileToInternalStorage(uri: Uri, mContext: Context): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = mContext.contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(mContext, uri) ?: "status_${System.currentTimeMillis()}"
            val outputFile = File(mContext.filesDir, fileName)
            
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to internal storage", e)
            null
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        cursor.getString(displayNameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name from URI", e)
            null
        }
    }
    
    /**
     * SECURITY CHECK: Validates that a file path is within the Status Saver directory
     * This prevents accidental deletion of files from WhatsApp's original .Statuses folder
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
            false // Fail safe - don't allow deletion if we can't verify
        }
    }
} 
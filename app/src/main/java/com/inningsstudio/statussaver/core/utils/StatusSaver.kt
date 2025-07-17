package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles saving and managing saved WhatsApp statuses
 */
object StatusSaver {
    
    private const val TAG = "StatusSaver"
    val savedStatusList = mutableListOf<StatusModel>()

    val SAVED_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "Status Saver" + File.separator + "downloaded"

    val FAVOURITES_DIRECTORY =
        SAVED_DIRECTORY + File.separator + "favourites"

    /**
     * Get all saved statuses (excluding favorites)
     */
    suspend fun getSavedStatus(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<StatusModel>()
        
        val savedDir = File(SAVED_DIRECTORY)
        Log.d(TAG, "Reading saved statuses from: ${savedDir.absolutePath}")
        
        if (savedDir.exists() && savedDir.isDirectory) {
            val files = savedDir.listFiles()
            if (!files.isNullOrEmpty()) {
                files.forEach { file ->
                    // Skip the favourites folder and only include regular files
                    if (file.isFile && file.canRead() && !file.name.equals("favourites", ignoreCase = true)) {
                        val isVideo = file.name.lowercase().endsWith(".mp4")
                        savedFiles.add(StatusModel(
                            id = file.absolutePath.hashCode().toLong(),
                            filePath = file.absolutePath,
                            fileName = file.name,
                            fileSize = file.length(),
                            lastModified = file.lastModified(),
                            isVideo = isVideo
                        ))
                        Log.d(TAG, "Found saved status: ${file.name}")
                    }
                }
            } else {
                Log.d(TAG, "No saved statuses found in directory")
            }
        } else {
            Log.d(TAG, "Saved statuses directory does not exist: ${savedDir.absolutePath}")
        }
        
        savedStatusList.clear()
        savedStatusList.addAll(savedFiles)
        Log.d(TAG, "Total saved statuses loaded: ${savedFiles.size}")
        return@withContext savedFiles
    }

    /**
     * Get all favorite statuses
     */
    suspend fun getFavoriteStatusesFromFolder(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val favoriteFiles = mutableListOf<StatusModel>()
        val favoritesDir = File(FAVOURITES_DIRECTORY)
        
        if (favoritesDir.exists() && favoritesDir.isDirectory) {
            val files = favoritesDir.listFiles()
            if (!files.isNullOrEmpty()) {
                files.forEach { file ->
                    if (file.isFile && file.canRead()) {
                        val isVideo = file.name.lowercase().endsWith(".mp4")
                        favoriteFiles.add(StatusModel(
                            id = file.absolutePath.hashCode().toLong(),
                            filePath = file.absolutePath,
                            fileName = file.name,
                            fileSize = file.length(),
                            lastModified = file.lastModified(),
                            isVideo = isVideo
                        ))
                    }
                }
            }
        }
        
        return@withContext favoriteFiles
    }

    /**
     * Mark a status as favorite by moving it from saved to favorites folder
     */
    suspend fun markAsFavorite(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "=== MARK AS FAVORITE OPERATION ===")
            Log.d(TAG, "Input file path: $filePath")
            
            // SECURITY CHECK: Ensure we only operate on files from Status Saver directory
            if (!isFileInStatusSaverDirectory(filePath)) {
                Log.e(TAG, "SECURITY VIOLATION: Attempted to mark file outside Status Saver directory as favorite: $filePath")
                return@withContext false
            }
            
            val favoritesDir = File(FAVOURITES_DIRECTORY)
            if (!favoritesDir.exists()) {
                favoritesDir.mkdirs()
            }
            
            // Check if it's a SAF URI or file path
            val isContentUri = filePath.startsWith("content://")
            
            if (isContentUri) {
                // Handle SAF URI - copy from status folder to favorites folder
                val uri = Uri.parse(filePath)
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                if (documentFile != null && documentFile.exists()) {
                    val fileName = documentFile.name ?: "favorite_${System.currentTimeMillis()}"
                    val destFile = File(favoritesDir, fileName)
                    
                    if (!destFile.exists()) {
                        // Copy content from SAF URI to favorites folder
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            destFile.outputStream().use { outputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                        
                        Log.d(TAG, "SAF URI copied to favorites successfully")
                        true
                    } else {
                        Log.d(TAG, "File already exists in favorites")
                        true
                    }
                } else {
                    Log.e(TAG, "SAF URI file not found or cannot be accessed")
                    false
                }
            } else {
                // Handle regular file path - move from status folder to favorites folder
                val sourceFile = File(filePath)
                val destFile = File(favoritesDir, sourceFile.name)
                
                Log.d(TAG, "File move operation:")
                Log.d(TAG, "  Source file: ${sourceFile.absolutePath}")
                Log.d(TAG, "  Source exists: ${sourceFile.exists()}")
                Log.d(TAG, "  Source can read: ${sourceFile.canRead()}")
                Log.d(TAG, "  Destination file: ${destFile.absolutePath}")
                Log.d(TAG, "  Destination exists: ${destFile.exists()}")
                
                if (!destFile.exists() && sourceFile.exists()) {
                    // Use copy and delete instead of rename for better reliability
                    val copied = copyFile(sourceFile, destFile)
                    if (copied) {
                        val deleted = sourceFile.delete()
                        Log.d(TAG, "File copied to favorites and original deleted: $deleted")
                        if (deleted) {
                            Log.d(TAG, "✅ File successfully moved to favorites")
                            true
                        } else {
                            // If delete failed, remove the copied file to maintain consistency
                            destFile.delete()
                            Log.e(TAG, "❌ Failed to delete original file, rolled back copy")
                            false
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to copy file to favorites")
                        false
                    }
                } else {
                    Log.d(TAG, "File already exists in favorites or source doesn't exist")
                    Log.d(TAG, "  Dest exists: ${destFile.exists()}")
                    Log.d(TAG, "  Source exists: ${sourceFile.exists()}")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as favorite", e)
            false
        }
    }

    /**
     * Unmark a status as favorite by moving it from favorites back to saved folder
     */
    suspend fun unmarkAsFavorite(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "=== UNMARK AS FAVORITE OPERATION ===")
            Log.d(TAG, "Input file path: $filePath")
            
            // SECURITY CHECK: Ensure we only operate on files from Status Saver directory
            if (!isFileInStatusSaverDirectory(filePath)) {
                Log.e(TAG, "SECURITY VIOLATION: Attempted to unmark file outside Status Saver directory as favorite: $filePath")
                return@withContext false
            }
            
            val favoritesDir = File(FAVOURITES_DIRECTORY)
            val statusDir = File(SAVED_DIRECTORY)
            
            // Check if it's a SAF URI or file path
            val isContentUri = filePath.startsWith("content://")
            
            if (isContentUri) {
                // Handle SAF URI - this shouldn't happen for favorites, but handle gracefully
                Log.d(TAG, "SAF URI unmark as favorite - this should not happen for favorites")
                true
            } else {
                // Handle regular file path - move from favorites folder back to status folder
                val favoriteFile = File(filePath)
                
                if (favoriteFile.exists() && favoriteFile.parentFile?.absolutePath == favoritesDir.absolutePath) {
                    // Ensure status directory exists
                    if (!statusDir.exists()) {
                        statusDir.mkdirs()
                    }
                    
                    // Move the file from favorites folder back to status folder
                    val destFile = File(statusDir, favoriteFile.name)
                    
                    // Use copy and delete instead of rename for better reliability
                    val copied = copyFile(favoriteFile, destFile)
                    if (copied) {
                        val deleted = favoriteFile.delete()
                        Log.d(TAG, "File copied back to status folder and favorite deleted: $deleted")
                        if (deleted) {
                            Log.d(TAG, "✅ File successfully moved back to saved statuses")
                            true
                        } else {
                            // If delete failed, remove the copied file to maintain consistency
                            destFile.delete()
                            Log.e(TAG, "❌ Failed to delete favorite file, rolled back copy")
                            false
                        }
                    } else {
                        Log.e(TAG, "❌ Failed to copy file back to saved statuses")
                        false
                    }
                } else {
                    Log.d(TAG, "Favorite file not found or not in favorites directory")
                    true // Consider it successful if file doesn't exist
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unmarking as favorite", e)
            false
        }
    }

    /**
     * Check if a file is currently in favorites
     */
    fun isFileInFavorites(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            val favoritesDir = File(FAVOURITES_DIRECTORY)
            return file.absolutePath.startsWith(favoritesDir.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if file is in favorites", e)
            false
        }
    }

    /**
     * Get the current location of a file (saved or favorites)
     */
    fun getFileLocation(filePath: String): String {
        return try {
            val file = File(filePath)
            val favoritesDir = File(FAVOURITES_DIRECTORY)
            val savedDir = File(SAVED_DIRECTORY)
            
            when {
                file.absolutePath.startsWith(favoritesDir.absolutePath) -> "favorites"
                file.absolutePath.startsWith(savedDir.absolutePath) -> "saved"
                else -> "unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file location", e)
            "unknown"
        }
    }

    /**
     * Delete a saved status (removes from both saved and favorites if present)
     */
    suspend fun deleteSavedStatus(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "=== DELETE SAVED STATUS OPERATION ===")
            Log.d(TAG, "Input file path: $filePath")
            
            // SECURITY CHECK: Ensure we only delete files from Status Saver directory
            if (!isFileInStatusSaverDirectory(filePath)) {
                Log.e(TAG, "SECURITY VIOLATION: Attempted to delete file outside Status Saver directory: $filePath")
                return@withContext false
            }
            
            // Check if it's a SAF URI or file path
            val isContentUri = filePath.startsWith("content://")
            
            if (isContentUri) {
                // Handle SAF URI deletion
                val uri = Uri.parse(filePath)
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                if (documentFile != null && documentFile.exists()) {
                    val deleted = documentFile.delete()
                    if (deleted) {
                        // Also remove from favorites if it exists there
                        val fileName = documentFile.name
                        if (fileName != null) {
                            val favoritesDir = File(FAVOURITES_DIRECTORY)
                            val favoriteFile = File(favoritesDir, fileName)
                            if (favoriteFile.exists()) {
                                favoriteFile.delete()
                            }
                        }
                    }
                    Log.d(TAG, "SAF URI deletion result: $deleted")
                    deleted
                } else {
                    Log.e(TAG, "SAF URI file not found or cannot be accessed")
                    false
                }
            } else {
                // Handle regular file path deletion
                val file = File(filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted) {
                        // Also remove from the other location if it exists there
                        val fileName = file.name
                        val favoritesDir = File(FAVOURITES_DIRECTORY)
                        val savedDir = File(SAVED_DIRECTORY)
                        
                        // Check if it was in favorites, then also delete from saved
                        if (file.absolutePath.startsWith(favoritesDir.absolutePath)) {
                            val savedFile = File(savedDir, fileName)
                            if (savedFile.exists()) {
                                savedFile.delete()
                            }
                        }
                        // Check if it was in saved, then also delete from favorites
                        else if (file.absolutePath.startsWith(savedDir.absolutePath)) {
                            val favoriteFile = File(favoritesDir, fileName)
                            if (favoriteFile.exists()) {
                                favoriteFile.delete()
                            }
                        }
                    }
                    Log.d(TAG, "File path deletion result: $deleted")
                    deleted
                } else {
                    Log.e(TAG, "File not found: $filePath")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting saved status", e)
            false
        }
    }

    /**
     * Save a status to the saved folder
     */
    suspend fun saveStatusToFolder(context: Context, folderUri: Uri, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "=== STARTING SAVE OPERATION ===")
            Log.d(TAG, "Source file: $filePath")
            Log.d(TAG, "Using DCIM path for saving: $SAVED_DIRECTORY")

            // Handle both file paths and content URIs
            val isContentUri = filePath.startsWith("content://")
            Log.d(TAG, "Is content URI: $isContentUri")

            if (isContentUri) {
                // Handle content URI
                val sourceUri = Uri.parse(filePath)
                Log.d(TAG, "Parsed source URI: $sourceUri")

                // Get file name from content URI
                val fileName = getFileNameFromUri(context, sourceUri)
                Log.d(TAG, "File name from URI: $fileName")

                if (fileName == null) {
                    Log.e(TAG, "Could not get file name from URI")
                    return@withContext false
                }

                // Get file size from content URI
                val fileSize = getFileSizeFromUri(context, sourceUri)
                Log.d(TAG, "File size from URI: $fileSize bytes")

                // Get MIME type from content URI
                val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
                Log.d(TAG, "MIME type from URI: $mimeType")

                // Create the destination directory
                val destinationDir = File(SAVED_DIRECTORY)
                if (!destinationDir.exists()) {
                    val created = destinationDir.mkdirs()
                    if (!created) {
                        Log.e(TAG, "Failed to create destination directory: $SAVED_DIRECTORY")
                        return@withContext false
                    }
                    Log.d(TAG, "Created destination directory: $SAVED_DIRECTORY")
                }

                // Create the destination file
                val destinationFile = File(destinationDir, fileName)
                Log.d(TAG, "Destination file: ${destinationFile.absolutePath}")

                // Copy the file content
                try {
                    context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                        destinationFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0L

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }

                            Log.d(TAG, "File copied successfully: $totalBytes bytes")
                        }
                    } ?: run {
                        Log.e(TAG, "Failed to open input stream for source URI")
                        return@withContext false
                    }
                } catch (e: Exception) {
                    /**
                     * EDGE CASE HANDLING: Uninstall/Reinstall Scenario
                     * 
                     * Problem: When user uninstalls and reinstalls the app on Android 10+,
                     * the app loses access to previously saved files due to scoped storage.
                     * If user tries to save a file with the same name, it fails with EACCES.
                     * 
                     * Solution: Detect this specific error and create a unique filename
                     * to avoid the conflict, ensuring the user's file gets saved.
                     * 
                     * Example:
                     * - User saves "image.jpg" → works fine
                     * - User uninstalls app → file exists but app can't access it
                     * - User reinstalls app → tries to save "image.jpg" again
                     * - Gets EACCES error → creates "image_1703123456789.jpg" instead
                     */
                    if (e.message?.contains("EACCES") == true || e.message?.contains("Permission denied") == true) {
                        Log.w(TAG, "Permission denied - likely uninstall/reinstall scenario, trying with unique filename")
                        
                        // Create unique filename to avoid conflict with inaccessible existing file
                        val baseName = fileName.substringBeforeLast(".")
                        val extension = fileName.substringAfterLast(".", "")
                        val timestamp = System.currentTimeMillis()
                        val uniqueFileName = "${baseName}_${timestamp}.$extension"
                        val uniqueDestinationFile = File(destinationDir, uniqueFileName)
                        
                        Log.d(TAG, "Retrying with unique filename: $uniqueFileName")
                        
                        // Rewrite the file with the unique name
                        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                            uniqueDestinationFile.outputStream().use { outputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytes = 0L

                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                }

                                Log.d(TAG, "File copied successfully with unique name: $totalBytes bytes")
                            }
                        } ?: run {
                            Log.e(TAG, "Failed to open input stream for source URI")
                            return@withContext false
                        }
                    } else {
                        // Re-throw other exceptions (not related to uninstall/reinstall scenario)
                        throw e
                    }
                }

                Log.d(TAG, "=== SAVE OPERATION COMPLETED SUCCESSFULLY ===")
                true

            } else {
                // Handle regular file path
                val sourceFile = File(filePath)
                Log.d(TAG, "=== REGULAR FILE PATH SAVE ===")
                Log.d(TAG, "Source file path: $filePath")
                Log.d(TAG, "Source file exists: ${sourceFile.exists()}")
                Log.d(TAG, "Source file can read: ${sourceFile.canRead()}")
                Log.d(TAG, "Source file size: ${sourceFile.length()} bytes")
                
                if (!sourceFile.exists()) {
                    Log.e(TAG, "Source file does not exist: $filePath")
                    return@withContext false
                }

                if (!sourceFile.canRead()) {
                    Log.e(TAG, "Source file cannot be read: $filePath")
                    return@withContext false
                }

                if (sourceFile.length() == 0L) {
                    Log.e(TAG, "Source file is empty: $filePath")
                    return@withContext false
                }

                // Create the destination directory
                val destinationDir = File(SAVED_DIRECTORY)
                if (!destinationDir.exists()) {
                    val created = destinationDir.mkdirs()
                    if (!created) {
                        Log.e(TAG, "Failed to create destination directory: $SAVED_DIRECTORY")
                        return@withContext false
                    }
                    Log.d(TAG, "Created destination directory: $SAVED_DIRECTORY")
                }

                // Create the destination file
                val destinationFile = File(destinationDir, sourceFile.name)
                Log.d(TAG, "Destination file: ${destinationFile.absolutePath}")

                // Copy the file content
                try {
                    sourceFile.inputStream().use { inputStream ->
                        destinationFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytes = 0L

                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                            }

                            Log.d(TAG, "File copied successfully: $totalBytes bytes")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error copying file: ${e.message}", e)
                    
                    /**
                     * ENHANCED ERROR HANDLING: Multiple failure scenarios
                     * 
                     * 1. EACCES/Permission denied: File access issues
                     * 2. File in use: WhatsApp might be using the file
                     * 3. File corruption: Incomplete downloads
                     * 4. File name conflicts: Duplicate names
                     * 
                     * Solution: Try multiple approaches to save the file
                     */
                    if (e.message?.contains("EACCES") == true || 
                        e.message?.contains("Permission denied") == true ||
                        e.message?.contains("File in use") == true ||
                        e.message?.contains("Access denied") == true) {
                        
                        Log.w(TAG, "File access issue detected, trying with unique filename")
                        
                        // Create unique filename to avoid conflicts
                        val baseName = sourceFile.name.substringBeforeLast(".")
                        val extension = sourceFile.name.substringAfterLast(".", "")
                        val timestamp = System.currentTimeMillis()
                        val uniqueFileName = "${baseName}_${timestamp}.$extension"
                        val uniqueDestinationFile = File(destinationDir, uniqueFileName)
                        
                        Log.d(TAG, "Retrying with unique filename: $uniqueFileName")
                        
                        try {
                            // Try to copy with unique name
                            sourceFile.inputStream().use { inputStream ->
                                uniqueDestinationFile.outputStream().use { outputStream ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    var totalBytes = 0L

                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        outputStream.write(buffer, 0, bytesRead)
                                        totalBytes += bytesRead
                                    }

                                    Log.d(TAG, "File copied successfully with unique name: $totalBytes bytes")
                                }
                            }
                        } catch (retryException: Exception) {
                            Log.e(TAG, "Failed to copy even with unique filename: ${retryException.message}")
                            return@withContext false
                        }
                    } else {
                        // For other exceptions, log and return false
                        Log.e(TAG, "Unhandled exception during file copy: ${e.message}")
                        return@withContext false
                    }
                }

                Log.d(TAG, "=== SAVE OPERATION COMPLETED SUCCESSFULLY ===")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving status to folder", e)
            false
        }
    }

    /**
     * Helper function to copy files with verification
     */
    private fun copyFile(source: File, destination: File): Boolean {
        return try {
            source.inputStream().use { inputStream ->
                destination.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }
                }
            }
            
            // Verify the copy was successful
            val sourceSize = source.length()
            val destSize = destination.length()
            val success = sourceSize == destSize && destSize > 0
            
            Log.d(TAG, "Copy verification - Source size: $sourceSize, Dest size: $destSize, Success: $success")
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file from ${source.absolutePath} to ${destination.absolutePath}", e)
            false
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

    private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        cursor.getLong(sizeIndex)
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size from URI", e)
            0L
        }
    }
    
    /**
     * SECURITY CHECK: Validates that a file path is within the Status Saver directory
     * This prevents accidental deletion of files from WhatsApp's original .Statuses folder
     */
    private fun isFileInStatusSaverDirectory(filePath: String): Boolean {
        return try {
            Log.d(TAG, "=== SECURITY CHECK ===")
            Log.d(TAG, "Checking file path: $filePath")
            
            // Check if it's a SAF URI (these are safe as they're managed by the app)
            if (filePath.startsWith("content://")) {
                Log.d(TAG, "Security check - SAF URI detected, allowing")
                return true
            }
            
            // For regular file paths, check if they're within Status Saver directory
            val file = File(filePath)
            val statusSaverDir = File(SAVED_DIRECTORY)
            val favoritesDir = File(FAVOURITES_DIRECTORY)
            
            // Check if file is within Status Saver directory or its subdirectories
            val isInStatusSaver = file.absolutePath.startsWith(statusSaverDir.absolutePath)
            val isInFavorites = file.absolutePath.startsWith(favoritesDir.absolutePath)
            
            Log.d(TAG, "Security check - File absolute path: ${file.absolutePath}")
            Log.d(TAG, "Security check - Status Saver Dir: ${statusSaverDir.absolutePath}")
            Log.d(TAG, "Security check - Favorites Dir: ${favoritesDir.absolutePath}")
            Log.d(TAG, "Security check - Is in Status Saver: $isInStatusSaver")
            Log.d(TAG, "Security check - Is in Favorites: $isInFavorites")
            Log.d(TAG, "Security check - Final result: ${isInStatusSaver || isInFavorites}")
            
            isInStatusSaver || isInFavorites
        } catch (e: Exception) {
            Log.e(TAG, "Error in security check", e)
            false // Fail safe - don't allow deletion if we can't verify
        }
    }
} 
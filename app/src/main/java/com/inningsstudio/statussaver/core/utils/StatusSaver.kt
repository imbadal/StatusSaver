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

    private val SAVED_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "Status Saver" + File.separator + "statuses"

    private val FAVOURITES_DIRECTORY =
        SAVED_DIRECTORY + File.separator + "favourites"

    suspend fun getSavedStatus(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<StatusModel>()
        val pref = PreferenceUtils(context.applicationContext as android.app.Application)
        val safUriString = pref.getUriFromPreference()
        var usedSAF = false
        if (!safUriString.isNullOrBlank()) {
            try {
                val safUri = Uri.parse(safUriString)
                val documentFile = DocumentFile.fromTreeUri(context, safUri)
                if (documentFile != null && documentFile.exists()) {
                    val savedStatuses = getSavedStatusesFromFolder(context)
                    savedFiles.addAll(savedStatuses)
                    usedSAF = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accessing SAF URI", e)
            }
        }
        
        if (!usedSAF) {
            // Fallback to direct file access
            val savedDir = File(SAVED_DIRECTORY)
            if (savedDir.exists() && savedDir.isDirectory) {
                val files = savedDir.listFiles()
                if (!files.isNullOrEmpty()) {
                    files.forEach { file ->
                        if (file.isFile && file.canRead()) {
                            val isVideo = file.name.lowercase().endsWith(".mp4")
                            savedFiles.add(StatusModel(
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
        }
        
        savedStatusList.clear()
        savedStatusList.addAll(savedFiles)
        return@withContext savedFiles
    }

    suspend fun markAsFavorite(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Attempting to mark as favorite: $filePath")
            
            val favoritesDir = File(FAVOURITES_DIRECTORY)
            if (!favoritesDir.exists()) {
                favoritesDir.mkdirs()
            }
            
            // Check if it's a SAF URI or file path
            val isContentUri = filePath.startsWith("content://")
            
            if (isContentUri) {
                // Handle SAF URI - move from status folder to favorites folder
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
                        
                        // Delete the original file from status folder
                        val deleted = documentFile.delete()
                        Log.d(TAG, "SAF URI moved to favorites successfully, original deleted: $deleted")
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
                
                if (!destFile.exists() && sourceFile.exists()) {
                    // Move the file from status folder to favorites folder
                    val moved = sourceFile.renameTo(destFile)
                    Log.d(TAG, "File moved to favorites successfully: $moved")
                    moved
                } else {
                    Log.d(TAG, "File already exists in favorites or source doesn't exist")
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as favorite", e)
            false
        }
    }

    suspend fun unmarkAsFavorite(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Attempting to unmark as favorite: $filePath")
            
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
                    val moved = favoriteFile.renameTo(destFile)
                    Log.d(TAG, "File moved back to status folder successfully: $moved")
                    moved
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

    suspend fun getSavedStatusesFromFolder(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<StatusModel>()
        val pref = PreferenceUtils(context.applicationContext as android.app.Application)
        val safUriString = pref.getUriFromPreference()
        
        if (!safUriString.isNullOrBlank()) {
            try {
                val safUri = Uri.parse(safUriString)
                val documentFile = DocumentFile.fromTreeUri(context, safUri)
                
                if (documentFile != null && documentFile.exists()) {
                    documentFile.listFiles().forEach { file ->
                        if (file.isFile && file.canRead()) {
                            val isVideo = file.name?.lowercase()?.endsWith(".mp4") ?: false
                            savedFiles.add(StatusModel(
                                id = file.uri.toString().hashCode().toLong(),
                                filePath = file.uri.toString(),
                                fileName = file.name ?: "",
                                fileSize = file.length(),
                                lastModified = file.lastModified(),
                                isVideo = isVideo
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading saved statuses from SAF", e)
            }
        }
        
        return@withContext savedFiles
    }

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

    suspend fun deleteSavedStatus(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "Attempting to delete: $filePath")
            
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
                        // Also remove from favorites if it exists there (in case it was moved there)
                        val favoritesDir = File(FAVOURITES_DIRECTORY)
                        val favoriteFile = File(favoritesDir, file.name)
                        if (favoriteFile.exists()) {
                            favoriteFile.delete()
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
                if (!sourceFile.exists()) {
                    Log.e(TAG, "Source file does not exist: $filePath")
                    return@withContext false
                }

                Log.d(TAG, "Source file exists, size: ${sourceFile.length()} bytes")

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
                        val baseName = sourceFile.name.substringBeforeLast(".")
                        val extension = sourceFile.name.substringAfterLast(".", "")
                        val timestamp = System.currentTimeMillis()
                        val uniqueFileName = "${baseName}_${timestamp}.$extension"
                        val uniqueDestinationFile = File(destinationDir, uniqueFileName)
                        
                        Log.d(TAG, "Retrying with unique filename: $uniqueFileName")
                        
                        // Rewrite the file with the unique name
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
                    } else {
                        // Re-throw other exceptions (not related to uninstall/reinstall scenario)
                        throw e
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
} 
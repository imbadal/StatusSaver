package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import coil.request.ImageRequest
import com.inningsstudio.statussaver.core.utils.Const.MP4
import com.inningsstudio.statussaver.core.utils.Const.NO_MEDIA
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import androidx.core.content.FileProvider
import java.nio.file.Files
import java.nio.file.StandardCopyOption


object FileUtils {

    private const val TAG = "FileUtils"
    val statusList = mutableListOf<StatusModel>()
    val savedStatusList = mutableListOf<StatusModel>()

    private val SAVED_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "Status Saver" + File.separator + "statuses"

    private val FAVOURITES_DIRECTORY =
        SAVED_DIRECTORY + File.separator + "favourites"

    private fun isVideo(path: String): Boolean {
        return (path.substring(path.length - 3) == MP4)
    }

    suspend fun getStatus(context: Context, statusUri: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val files = mutableListOf<StatusModel>()
        
        // Check if we have required permissions
        if (!StorageAccessHelper.hasRequiredPermissions(context)) {
            return@withContext files
        }
        
        // Check if statusUri is empty or invalid
        if (statusUri.isBlank()) {
            // Method 1: Try MediaStore API (Android 10+) - most efficient
            val mediaStoreStatuses = StorageAccessHelper.getStatusesViaMediaStore(context)
            if (mediaStoreStatuses.isNotEmpty()) {
                files.addAll(mediaStoreStatuses)
            } else {
                // Method 2: Try comprehensive path detection
                val detector = StatusPathDetector()
                val availablePaths = detector.getAllPossibleStatusPaths()
                if (availablePaths.isNotEmpty()) {
                    for (path in availablePaths) {
                        val pathStatuses = getStatusFromPath(context, path)
                        if (pathStatuses.isNotEmpty()) {
                            files.addAll(pathStatuses)
                            break // Use the first path that has statuses
                        }
                    }
                }
            }
        } else {
            // Check if the input is a file path or a URI
            val isFilePath = statusUri.startsWith("/") || !statusUri.contains("://")
            
            if (isFilePath) {
                // Handle as file path
                return@withContext getStatusFromPath(context, statusUri)
            } else {
                // Handle as URI - try SAF
                val safStatuses = StorageAccessHelper.getStatusesViaSAF(context, statusUri)
                files.addAll(safStatuses)
            }
        }
        
        // Add minimal padding items for UI
        if (files.isNotEmpty()) {
            files.addAll(listOf(
                StatusModel(
                    id = 0L,
                    filePath = "",
                    fileName = "",
                    fileSize = 0L,
                    lastModified = 0L
                ),
                StatusModel(
                    id = 0L,
                    filePath = "",
                    fileName = "",
                    fileSize = 0L,
                    lastModified = 0L
                )
            ))
        }
        
        statusList.clear()
        statusList.addAll(files)
        return@withContext files
    }
    
    /**
     * Get status from a file path (for direct file system access)
     */
    private suspend fun getStatusFromPath(context: Context, path: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val files = mutableListOf<StatusModel>()
        
        try {
            val directory = File(path)
            
            if (directory.exists() && directory.isDirectory) {
                // List all files including hidden ones
                val allFiles = directory.listFiles { file ->
                    true
                }
                
                if (!allFiles.isNullOrEmpty()) {
                    // Process all files (including hidden ones) for received statuses
                    allFiles.forEach { file ->
                        val filePath = file.absolutePath
                        if (isValidFile(filePath)) {
                            if (isVideo(filePath)) {
                                try {
                                    val mediaMetadataRetriever = MediaMetadataRetriever()
                                    mediaMetadataRetriever.setDataSource(filePath)
                                    val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                                    mediaMetadataRetriever.release()
                                    files.add(StatusModel(
                                        id = filePath.hashCode().toLong(),
                                        filePath = filePath,
                                        fileName = file.name,
                                        fileSize = file.length(),
                                        lastModified = file.lastModified(),
                                        isVideo = true,
                                        thumbnail = thumbnail
                                    ))
                                } catch (e: Exception) {
                                    // Skip problematic video files
                                }
                            } else {
                                val imageRequest = ImageRequest.Builder(context).data(filePath).build()
                                files.add(StatusModel(
                                    id = filePath.hashCode().toLong(),
                                    filePath = filePath,
                                    fileName = file.name,
                                    fileSize = file.length(),
                                    lastModified = file.lastModified(),
                                    imageRequest = imageRequest
                                ))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle errors silently to avoid blocking UI
        }
        
        // Add minimal padding items for UI
        if (files.isNotEmpty()) {
            files.addAll(listOf(
                StatusModel(
                    id = 0L,
                    filePath = "",
                    fileName = "",
                    fileSize = 0L,
                    lastModified = 0L
                ),
                StatusModel(
                    id = 0L,
                    filePath = "",
                    fileName = "",
                    fileSize = 0L,
                    lastModified = 0L
                )
            ))
        }
        
        statusList.clear()
        statusList.addAll(files)
        return@withContext files
    }

    suspend fun getSavedStatus(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<StatusModel>()
        val pref = PreferenceUtils(context.applicationContext as android.app.Application)
        val safUriString = pref.getUriFromPreference()
        var usedSAF = false
        if (!safUriString.isNullOrBlank()) {
            try {
                val safUri = Uri.parse(safUriString)
                val folderDoc = DocumentFile.fromTreeUri(context, safUri)
                if (folderDoc != null) {
                    val statusSaverFolder = folderDoc.findFile("Status Saver")
                    if (statusSaverFolder != null && statusSaverFolder.isDirectory) {
                        val statusesFolder = statusSaverFolder.findFile("statuses")
                        if (statusesFolder != null && statusesFolder.isDirectory) {
                            Log.d(TAG, "Reading saved statuses from SAF/Status Saver/statuses: "+ statusesFolder.uri)
                            for (file in statusesFolder.listFiles()) {
                                if (file.isFile && isValidFile(file.name ?: "")) {
                                    val fileName = file.name ?: ""
                                    val fileSize = file.length()
                                    val lastModified = file.lastModified()
                                    val filePath = file.uri.toString()
                                    val isVideo = fileName.lowercase().endsWith(".mp4") || fileName.lowercase().endsWith(".3gp") || fileName.lowercase().endsWith(".mkv")
                                    if (isVideo) {
                                        // No thumbnail for SAF videos (optional: implement if needed)
                                        savedFiles.add(StatusModel(
                                            id = filePath.hashCode().toLong(),
                                            filePath = filePath,
                                            fileName = fileName,
                                            fileSize = fileSize,
                                            lastModified = lastModified,
                                            isVideo = true
                                        ))
                                    } else {
                                        val imageRequest = ImageRequest.Builder(context).data(file.uri).build()
                                        savedFiles.add(StatusModel(
                                            id = filePath.hashCode().toLong(),
                                            filePath = filePath,
                                            fileName = fileName,
                                            fileSize = fileSize,
                                            lastModified = lastModified,
                                            imageRequest = imageRequest
                                        ))
                                    }
                                }
                            }
                            usedSAF = true
                        } else {
                            Log.w(TAG, "statuses folder not found in SAF location: $safUriString")
                        }
                    } else {
                        Log.w(TAG, "Status Saver folder not found in SAF location: $safUriString")
                    }
                } else {
                    Log.w(TAG, "Could not get DocumentFile from SAF URI: $safUriString")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading saved statuses from SAF", e)
            }
        }
        if (!usedSAF) {
            // Fallback to legacy DCIM path
            Log.d(TAG, "Falling back to legacy DCIM path: $SAVED_DIRECTORY")
        val file = File(SAVED_DIRECTORY)
        file.listFiles()?.forEach { it ->
            val path = it.path
            if (isValidFile(path)) {
                if (isVideo(path)) {
                    try {
                        val mediaMetadataRetriever = MediaMetadataRetriever()
                        mediaMetadataRetriever.setDataSource(path)
                        val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                        mediaMetadataRetriever.release()
                        savedFiles.add(StatusModel(
                            id = path.hashCode().toLong(),
                            filePath = path,
                            fileName = it.name,
                            fileSize = it.length(),
                            lastModified = it.lastModified(),
                            isVideo = true,
                            thumbnail = thumbnail
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing saved video: $path", e)
                    }
                } else {
                    val imageRequest = ImageRequest.Builder(context).data(path).build()
                    savedFiles.add(StatusModel(
                        id = path.hashCode().toLong(),
                        filePath = path,
                        fileName = it.name,
                        fileSize = it.length(),
                        lastModified = it.lastModified(),
                        imageRequest = imageRequest
                    ))
                }
            }
        }
        }
        savedStatusList.clear()
        savedStatusList.addAll(savedFiles)
        return@withContext savedFiles
    }

    /**
     * Mark a status as favorite by moving it to the favourites folder
     */
    suspend fun markAsFavorite(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            val favouritesDir = File(FAVOURITES_DIRECTORY)
            if (!favouritesDir.exists()) favouritesDir.mkdirs()
            val destFile = File(favouritesDir, file.name)
            Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking as favorite", e)
            false
        }
    }

    /**
     * Remove a status from favorites by moving it back to the statuses folder
     */
    suspend fun unmarkAsFavorite(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext false
            val statusesDir = File(SAVED_DIRECTORY)
            if (!statusesDir.exists()) statusesDir.mkdirs()
            val destFile = File(statusesDir, file.name)
            Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error unmarking as favorite", e)
            false
        }
    }

    /**
     * Get all saved statuses (excluding favourites)
     */
    suspend fun getSavedStatusesFromFolder(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "=== READING SAVED STATUSES FROM FOLDER ===")
            Log.d(TAG, "Directory path: $SAVED_DIRECTORY")
            val savedDir = File(SAVED_DIRECTORY)
            
            Log.d(TAG, "Directory exists: ${savedDir.exists()}")
            Log.d(TAG, "Is directory: ${savedDir.isDirectory}")
            Log.d(TAG, "Can read: ${savedDir.canRead()}")
            Log.d(TAG, "Can write: ${savedDir.canWrite()}")
            Log.d(TAG, "Absolute path: ${savedDir.absolutePath}")
            
            if (!savedDir.exists() || !savedDir.isDirectory) {
                Log.w(TAG, "Saved statuses directory does not exist: $SAVED_DIRECTORY")
                return@withContext emptyList()
            }
            
            val statusModels = mutableListOf<StatusModel>()
            
            // CRITICAL DEBUGGING: List ALL files first, no filtering
            Log.d(TAG, "=== STEP 1: LISTING ALL FILES (NO FILTERS) ===")
            val allFiles = savedDir.listFiles()
            Log.d(TAG, "Total files found: ${allFiles?.size ?: 0}")
            
            if (allFiles != null) {
                allFiles.forEach { file ->
                    Log.d(TAG, "File: ${file.name}")
                    Log.d(TAG, "  - isFile: ${file.isFile}")
                    Log.d(TAG, "  - isDirectory: ${file.isDirectory}")
                    Log.d(TAG, "  - isHidden: ${file.isHidden}")
                    Log.d(TAG, "  - canRead: ${file.canRead()}")
                    Log.d(TAG, "  - canWrite: ${file.canWrite()}")
                    Log.d(TAG, "  - size: ${file.length()}")
                    Log.d(TAG, "  - lastModified: ${file.lastModified()}")
                    Log.d(TAG, "  - absolutePath: ${file.absolutePath}")
                    
                    // Test isValidFile for each file
                    val isValid = isValidFile(file.absolutePath)
                    Log.d(TAG, "  - isValidFile: $isValid")
                    Log.d(TAG, "  ---")
                }
            }
            
            // STEP 2: Try to get files with different methods
            Log.d(TAG, "=== STEP 2: TRYING DIFFERENT FILE LISTING METHODS ===")
            
            // Method 1: Direct listFiles()
            var files1 = savedDir.listFiles()
            Log.d(TAG, "Method 1 - Direct listFiles(): ${files1?.size ?: 0} files")
            
            // Method 2: listFiles() with FileFilter
            var files2 = savedDir.listFiles { file ->
                file.isFile
            }
            Log.d(TAG, "Method 2 - listFiles() with isFile filter: ${files2?.size ?: 0} files")
            
            // Method 3: listFiles() with our custom filter
            var files3 = savedDir.listFiles { file ->
                file.isFile && isValidFile(file.absolutePath)
            }
            Log.d(TAG, "Method 3 - listFiles() with isValidFile filter: ${files3?.size ?: 0} files")
            
            // Method 4: Using File.list() and creating File objects
            val fileNames = savedDir.list()
            Log.d(TAG, "Method 4 - File.list() names: ${fileNames?.size ?: 0} names")
            fileNames?.forEach { name ->
                Log.d(TAG, "  Name: $name")
            }
            
            // Use the best result
            var files = files1
            if (files == null || files.isEmpty()) {
                files = files2
                Log.d(TAG, "Using Method 2 results")
            }
            if (files == null || files.isEmpty()) {
                files = files3
                Log.d(TAG, "Using Method 3 results")
            }
            if (files == null || files.isEmpty()) {
                // Create files from names
                files = fileNames?.mapNotNull { name ->
                    val file = File(savedDir, name)
                    if (file.isFile) file else null
                }?.toTypedArray()
                Log.d(TAG, "Using Method 4 results: ${files?.size ?: 0} files")
            }
            
            Log.d(TAG, "Final files array: ${files?.size ?: 0} files")
            
            // STEP 3: Process files
            Log.d(TAG, "=== STEP 3: PROCESSING FILES ===")
            files?.forEach { file ->
                try {
                    val fileName = file.name
                    val fileSize = file.length()
                    val lastModified = file.lastModified()
                    val isVideo = isVideo(file.absolutePath)
                    
                    Log.d(TAG, "Processing file: $fileName")
                    Log.d(TAG, "  - size: $fileSize")
                    Log.d(TAG, "  - isVideo: $isVideo")
                    Log.d(TAG, "  - isValidFile: ${isValidFile(file.absolutePath)}")
                    
                    // Only process if it's a valid file
                    if (isValidFile(file.absolutePath)) {
                        if (isVideo) {
                            try {
                                val mediaMetadataRetriever = MediaMetadataRetriever()
                                mediaMetadataRetriever.setDataSource(file.absolutePath)
                                val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                                mediaMetadataRetriever.release()
                                statusModels.add(StatusModel(
                                    id = file.absolutePath.hashCode().toLong(),
                                    filePath = file.absolutePath,
                                    fileName = fileName,
                                    fileSize = fileSize,
                                    lastModified = lastModified,
                                    isVideo = true,
                                    thumbnail = thumbnail
                                ))
                                Log.d(TAG, "✅ Added video status: $fileName")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error getting thumbnail for video: $fileName", e)
                                statusModels.add(StatusModel(
                                    id = file.absolutePath.hashCode().toLong(),
                                    filePath = file.absolutePath,
                                    fileName = fileName,
                                    fileSize = fileSize,
                                    lastModified = lastModified,
                                    isVideo = true
                                ))
                                Log.d(TAG, "✅ Added video status (no thumbnail): $fileName")
                            }
                        } else {
                            val imageRequest = ImageRequest.Builder(context).data(file.absolutePath).build()
                            statusModels.add(StatusModel(
                                id = file.absolutePath.hashCode().toLong(),
                                filePath = file.absolutePath,
                                fileName = fileName,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                imageRequest = imageRequest
                            ))
                            Log.d(TAG, "✅ Added image status: $fileName")
                        }
                    } else {
                        Log.d(TAG, "❌ Skipping invalid file: $fileName")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing saved file: ${file.name}", e)
                }
            }
            
            val sortedStatusModels = statusModels.sortedByDescending { it.lastModified }
            Log.d(TAG, "=== FINAL RESULT: Retrieved ${sortedStatusModels.size} saved statuses from folder ===")
            sortedStatusModels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting saved statuses from folder", e)
            emptyList()
        }
    }

    /**
     * Get all favorite statuses (from favourites folder)
     */
    suspend fun getFavoriteStatusesFromFolder(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        return@withContext try {
            val favouritesDir = File(FAVOURITES_DIRECTORY)
            if (!favouritesDir.exists() || !favouritesDir.isDirectory) {
                return@withContext emptyList()
            }
            val statusModels = mutableListOf<StatusModel>()
            val files = favouritesDir.listFiles { file ->
                file.isFile && isValidFile(file.absolutePath)
            }
            files?.forEach { file ->
                try {
                    val fileName = file.name
                    val fileSize = file.length()
                    val lastModified = file.lastModified()
                    val isVideo = isVideo(file.absolutePath)
                    if (isVideo) {
                        try {
                            val mediaMetadataRetriever = MediaMetadataRetriever()
                            mediaMetadataRetriever.setDataSource(file.absolutePath)
                            val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                            mediaMetadataRetriever.release()
                            statusModels.add(StatusModel(
                                id = file.absolutePath.hashCode().toLong(),
                                filePath = file.absolutePath,
                                fileName = fileName,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                isVideo = true,
                                thumbnail = thumbnail
                            ))
                        } catch (e: Exception) {
                            statusModels.add(StatusModel(
                                id = file.absolutePath.hashCode().toLong(),
                                filePath = file.absolutePath,
                                fileName = fileName,
                                fileSize = fileSize,
                                lastModified = lastModified,
                                isVideo = true
                            ))
                        }
                    } else {
                        val imageRequest = ImageRequest.Builder(context).data(file.absolutePath).build()
                        statusModels.add(StatusModel(
                            id = file.absolutePath.hashCode().toLong(),
                            filePath = file.absolutePath,
                            fileName = fileName,
                            fileSize = fileSize,
                            lastModified = lastModified,
                            imageRequest = imageRequest
                        ))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing favorite file: ${file.name}", e)
                }
            }
            val sortedStatusModels = statusModels.sortedByDescending { it.lastModified }
            sortedStatusModels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorite statuses from folder", e)
            emptyList()
        }
    }

    suspend fun deleteSavedStatus(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete saved status: $filePath")
            
            var fileDeleted = false
            
            // Check if it's a SAF URI
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                if (documentFile != null && documentFile.exists()) {
                    fileDeleted = documentFile.delete()
                    Log.d(TAG, "SAF file deletion result: $fileDeleted")
                } else {
                    Log.w(TAG, "SAF file not found or cannot be deleted: $filePath")
                }
            } else {
                // Handle as regular file path
                val file = File(filePath)
                if (file.exists()) {
                    fileDeleted = file.delete()
                    Log.d(TAG, "Regular file deletion result: $fileDeleted")
                } else {
                    Log.w(TAG, "File not found: $filePath")
                }
            }
            
            return@withContext fileDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting saved status: $filePath", e)
            return@withContext false
        }
    }

    /**
     * Get file name from content URI
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        cursor.getString(displayNameIndex)
                    } else {
                        // Fallback: try to get from URI path
                        uri.lastPathSegment
                    }
                } else {
                    uri.lastPathSegment
                }
            } ?: uri.lastPathSegment
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name from URI", e)
            uri.lastPathSegment
        }
    }
    
    /**
     * Get file size from content URI
     */
    private fun getFileSizeFromUri(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        cursor.getLong(sizeIndex)
                    } else {
                        -1L
                    }
                } else {
                    -1L
                }
            } ?: -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file size from URI", e)
            -1L
        }
    }

    private fun isValidFile(path: String): Boolean {
        if (path.isEmpty()) {
            Log.d(TAG, "Skipping empty path")
            return false
        }
        
        val extension = path.substringAfterLast(".", "").lowercase()
        val isNoMedia = extension == NO_MEDIA.lowercase()
        
        // Check if it's a media file - expanded list
        val isMediaFile = extension in listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", 
            "mp4", "avi", "mov", "mkv", "3gp", "m4v", "wmv", "flv",
            "heic", "heif", "tiff", "tif"
        )
        
        // Accept all media files, reject only .nomedia files
        val isValid = !isNoMedia && isMediaFile
        
        // Only log for debugging when there are issues
        if (!isValid) {
            Log.d(TAG, "File validation failed: $path")
            Log.d(TAG, "  - Extension: '$extension'")
            Log.d(TAG, "  - IsNoMedia: $isNoMedia")
            Log.d(TAG, "  - IsMediaFile: $isMediaFile")
            Log.d(TAG, "  - Final result: $isValid")
        }
        
        return isValid
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        return@withContext file.delete()
    }

    suspend fun copyFileToInternalStorage(uri: Uri, mContext: Context): String? = withContext(Dispatchers.IO) {
        val returnCursor: Cursor? = mContext.contentResolver.query(
            uri, arrayOf<String>(
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            ), null, null, null
        )

        returnCursor?.let {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            val output: File = if (SAVED_DIRECTORY != "") {
                val dir: File = File(SAVED_DIRECTORY)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                File("$SAVED_DIRECTORY/$name")
            } else {
                File(mContext.filesDir.toString() + "/" + name)
            }
            try {
                val inputStream: InputStream? = mContext.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(output)
                var read = 0
                val bufferSize = 1024
                val buffers = ByteArray(bufferSize)
                while (inputStream?.read(buffers).also { read = it ?: 0 } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream?.close()
                outputStream.close()
            } catch (e: Exception) {
                Log.e("Exception", e.message!!)
            }
            return@withContext output.path
        }
        return@withContext null
    }



    private fun shareVideo(title: String? = "", path: String, context: Context) {
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

    suspend fun shareStatus(context: Context, currentPath: String) = withContext(Dispatchers.IO) {
        if (isVideo(currentPath)) {
            shareVideo(path = currentPath, context = context)
        } else {
            shareImage(currentPath, context)
        }
    }

    private fun shareImage(currentPath: String, context: Context) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/jpeg"
        val bytes = ByteArrayOutputStream()
        val f = File(
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + "status_${System.currentTimeMillis()}.jpg"
        )
        try {
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPath))
        context.startActivity(Intent.createChooser(share, "Share with"))
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

                // Save to database after successful file save
                val isVideo = mimeType.startsWith("video/")
                // val savedToDb = saveStatusToDatabase(context, destinationFile.absolutePath, fileName, fileSize, isVideo) // REMOVED DB CALL
                Log.d(TAG, "Saved to database: (DB call removed)")

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

                // Save to database after successful file save
                val isVideo = sourceFile.name.lowercase().endsWith(".mp4")
                // val savedToDb = saveStatusToDatabase(context, destinationFile.absolutePath, sourceFile.name, sourceFile.length(), isVideo) // REMOVED DB CALL
                Log.d(TAG, "Saved to database: (DB call removed)")

                Log.d(TAG, "=== SAVE OPERATION COMPLETED SUCCESSFULLY ===")
                true
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving status to folder", e)
            false
        }
    }

}
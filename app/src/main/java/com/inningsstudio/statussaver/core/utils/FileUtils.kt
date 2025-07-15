package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import coil.request.ImageRequest
import com.inningsstudio.statussaver.core.constants.Const.MP4
import com.inningsstudio.statussaver.core.constants.Const.NO_MEDIA
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.data.model.SavedStatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import androidx.core.content.FileProvider


object FileUtils {

    private const val TAG = "FileUtils"
    val statusList = mutableListOf<StatusModel>()
    val savedStatusList = mutableListOf<StatusModel>()

    private val SAVED_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "inningsstudio" + File.separator + "Status Saver"

    private fun isVideo(path: String): Boolean {
        return (path.substring(path.length - 3) == MP4)
    }

    suspend fun getStatus(context: Context, statusUri: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val files = mutableListOf<StatusModel>()
        
        Log.d(TAG, "=== STARTING STATUS DETECTION ===")
        Log.d(TAG, "Android version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Check if we have required permissions
        if (!StorageAccessHelper.hasRequiredPermissions(context)) {
            Log.w(TAG, "Missing required permissions for Android ${android.os.Build.VERSION.SDK_INT}")
            return@withContext files
        }
        
        // Check if statusUri is empty or invalid
        if (statusUri.isBlank()) {
            Log.w(TAG, "Status URI is empty, trying multiple detection methods")
            
            // Method 1: Try MediaStore API (Android 10+)
            Log.d(TAG, "Method 1: Trying MediaStore API")
            val mediaStoreStatuses = StorageAccessHelper.getStatusesViaMediaStore(context)
            if (mediaStoreStatuses.isNotEmpty()) {
                Log.d(TAG, "Found ${mediaStoreStatuses.size} statuses via MediaStore")
                files.addAll(mediaStoreStatuses)
            }
            
            // Method 2: Try comprehensive path detection
            Log.d(TAG, "Method 2: Trying comprehensive path detection")
            val detector = StatusPathDetector()
            val availablePaths = detector.getAllPossibleStatusPaths()
            if (availablePaths.isNotEmpty()) {
                Log.d(TAG, "Found ${availablePaths.size} available status paths")
                for (path in availablePaths) {
                    Log.d(TAG, "Using detected path: $path")
                    val pathStatuses = getStatusFromPath(context, path)
                    if (pathStatuses.isNotEmpty()) {
                        Log.d(TAG, "Found ${pathStatuses.size} statuses in path: $path")
                        files.addAll(pathStatuses)
                        break // Use the first path that has statuses
                    }
                }
            } else {
                Log.w(TAG, "No WhatsApp status paths found")
            }
        } else {
            // Check if the input is a file path or a URI
            val isFilePath = statusUri.startsWith("/") || !statusUri.contains("://")
            
            if (isFilePath) {
                // Handle as file path
                Log.d(TAG, "Treating input as file path: $statusUri")
                return@withContext getStatusFromPath(context, statusUri)
            } else {
                // Handle as URI - try SAF
                Log.d(TAG, "Treating input as URI: $statusUri")
                val safStatuses = StorageAccessHelper.getStatusesViaSAF(context, statusUri)
                files.addAll(safStatuses)
            }
        }
        
        Log.d(TAG, "Total statuses found: ${files.size}")
        
        // Add padding items for UI
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
            ),
            StatusModel(
                id = 0L,
                filePath = "",
                fileName = "",
                fileSize = 0L,
                lastModified = 0L
            )
        ))
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
            Log.d(TAG, "Checking directory: ${directory.absolutePath}")
            Log.d(TAG, "Directory exists: ${directory.exists()}")
            Log.d(TAG, "Is directory: ${directory.isDirectory}")
            
            if (directory.exists() && directory.isDirectory) {
                // First, try to list all files including hidden ones
                val allFiles = directory.listFiles { file ->
                    // Accept all files for debugging received statuses
                    true
                }
                Log.d(TAG, "All files (including hidden): ${allFiles?.size ?: 0}")
                
                if (!allFiles.isNullOrEmpty()) {
                    Log.d(TAG, "=== ALL FILES IN STATUS DIRECTORY ===")
                    allFiles.forEach { file ->
                        Log.d(TAG, "File: ${file.name}, isHidden: ${file.isHidden}, isFile: ${file.isFile}, size: ${file.length()}")
                    }
                    Log.d(TAG, "=== END ALL FILES ===")
                    
                    // Process all files (including hidden ones) for received statuses
                    allFiles.forEach { file ->
                        Log.d(TAG, "Processing file: ${file.name}, isHidden: ${file.isHidden}, isFile: ${file.isFile}")
                        val filePath = file.absolutePath
                        if (isValidFile(filePath)) {
                            Log.d(TAG, "Processing valid status file: $filePath")
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
                                    Log.d(TAG, "Added video status: $filePath")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing video status: $filePath", e)
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
                                Log.d(TAG, "Added image status: $filePath")
                            }
                        } else {
                            Log.d(TAG, "Skipping invalid status file: $filePath")
                        }
                    }
                } else {
                    Log.d(TAG, "No files found in status directory (including hidden files)")
                    
                    // Try alternative approach - list files without filter
                    try {
                        val rawFiles = directory.listFiles()
                        Log.d(TAG, "Raw file listing: ${rawFiles?.size ?: 0} files")
                        rawFiles?.forEach { file ->
                            Log.d(TAG, "Raw file: ${file.name}, isHidden: ${file.isHidden}, isFile: ${file.isFile}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error listing raw files", e)
                    }
                }
            } else {
                Log.w(TAG, "Directory does not exist or is not a directory: $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading status from path: $path", e)
        }
        
        Log.d(TAG, "Total received status files found: ${files.size}")
        
        // Add padding items for UI
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
            ),
            StatusModel(
                id = 0L,
                filePath = "",
                fileName = "",
                fileSize = 0L,
                lastModified = 0L
            )
        ))
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
                    val statusWpFolder = folderDoc.findFile("StatusWp")
                    if (statusWpFolder != null && statusWpFolder.isDirectory) {
                        Log.d(TAG, "Reading saved statuses from SAF/StatusWp: ${statusWpFolder.uri}")
                        for (file in statusWpFolder.listFiles()) {
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
                        Log.w(TAG, "StatusWp folder not found in SAF location: $safUriString")
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
     * Get saved statuses with favorite information
     */
    suspend fun getSavedStatusesWithFavorites(context: Context): List<SavedStatusModel> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<SavedStatusModel>()
        val pref = PreferenceUtils(context.applicationContext as android.app.Application)
        val safUriString = pref.getUriFromPreference()
        var usedSAF = false
        
        if (!safUriString.isNullOrBlank()) {
            try {
                val safUri = Uri.parse(safUriString)
                val folderDoc = DocumentFile.fromTreeUri(context, safUri)
                if (folderDoc != null) {
                    val statusWpFolder = folderDoc.findFile("StatusWp")
                    if (statusWpFolder != null && statusWpFolder.isDirectory) {
                        Log.d(TAG, "Reading saved statuses from SAF/StatusWp: ${statusWpFolder.uri}")
                        for (file in statusWpFolder.listFiles()) {
                            if (file.isFile && isValidFile(file.name ?: "")) {
                                val filePath = file.uri.toString()
                                val isFav = pref.isFavorite(filePath)
                                savedFiles.add(SavedStatusModel(
                                    statusUri = filePath,
                                    isFav = isFav,
                                    savedDate = file.lastModified()
                                ))
                            }
                        }
                        usedSAF = true
                    } else {
                        Log.w(TAG, "StatusWp folder not found in SAF location: $safUriString")
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
                    val isFav = pref.isFavorite(path)
                    savedFiles.add(SavedStatusModel(
                        statusUri = path,
                        isFav = isFav,
                        savedDate = it.lastModified()
                    ))
                }
            }
        }
        
        return@withContext savedFiles
    }

    /**
     * Toggle favorite status for a saved status
     */
    suspend fun toggleFavoriteStatus(context: Context, statusUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val isCurrentlyFavorite = pref.isFavorite(statusUri)
            
            if (isCurrentlyFavorite) {
                pref.removeFromFavorites(statusUri)
                Log.d(TAG, "Removed from favorites: $statusUri")
            } else {
                pref.addToFavorites(statusUri)
                Log.d(TAG, "Added to favorites: $statusUri")
            }
            
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling favorite status: $statusUri", e)
            return@withContext false
        }
    }

    suspend fun deleteSavedStatus(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete saved status: $filePath")
            
            // Check if it's a SAF URI
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                if (documentFile != null && documentFile.exists()) {
                    val deleted = documentFile.delete()
                    Log.d(TAG, "SAF file deletion result: $deleted")
                    return@withContext deleted
                } else {
                    Log.w(TAG, "SAF file not found or cannot be deleted: $filePath")
                    return@withContext false
                }
            } else {
                // Handle as regular file path
                val file = File(filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d(TAG, "Regular file deletion result: $deleted")
                    return@withContext deleted
                } else {
                    Log.w(TAG, "File not found: $filePath")
                    return@withContext false
                }
            }
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
        
        Log.d(TAG, "File: $path, Extension: '$extension', IsNoMedia: $isNoMedia, IsMediaFile: $isMediaFile")
        
        // Accept all media files, reject only .nomedia files
        val isValid = !isNoMedia && isMediaFile
        Log.d(TAG, "File validation result: $isValid")
        
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

    /**
     * Copy a file from source to destination
     */
    fun copyFile(sourceFile: File, destinationFile: File): Boolean {
        return try {
            if (!destinationFile.parentFile?.exists()!!) {
                destinationFile.parentFile?.mkdirs()
            }
            
            val inputStream = sourceFile.inputStream()
            val outputStream = destinationFile.outputStream()
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            inputStream.close()
            outputStream.close()
            
            Log.d(TAG, "File copied successfully: ${sourceFile.name} -> ${destinationFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file: ${sourceFile.name}", e)
            false
        }
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

    suspend fun saveStatusToFolder(context: Context, folderUri: Uri, filePath: String): Boolean {
        return try {
            Log.d(TAG, "=== STARTING SAVE OPERATION ===")
            Log.d(TAG, "Source file: $filePath")
            Log.d(TAG, "Destination folder URI: $folderUri")
            
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
                    return false
                }
                
                // Get file size from content URI
                val fileSize = getFileSizeFromUri(context, sourceUri)
                Log.d(TAG, "File size from URI: $fileSize bytes")
                
                // Get MIME type from content URI
                val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
                Log.d(TAG, "MIME type from URI: $mimeType")
                
                // Get the folder document
                val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
                if (folderDoc == null) {
                    Log.e(TAG, "Failed to get folder document from URI: $folderUri")
                    return false
                }
                
                Log.d(TAG, "Folder document obtained: ${folderDoc.name}")
                
                // Create StatusWp subfolder if it doesn't exist
                var statusWpFolder = folderDoc.findFile("StatusWp")
                if (statusWpFolder == null) {
                    Log.d(TAG, "Creating StatusWp folder...")
                    statusWpFolder = folderDoc.createDirectory("StatusWp")
                    if (statusWpFolder == null) {
                        Log.e(TAG, "Failed to create StatusWp folder")
                        return false
                    }
                    Log.d(TAG, "StatusWp folder created successfully")
                } else {
                    Log.d(TAG, "StatusWp folder already exists")
                }
                
                // Create the new file in StatusWp folder
                val newFile = statusWpFolder.createFile(mimeType, fileName)
                if (newFile == null) {
                    Log.e(TAG, "Failed to create new file: $fileName")
                    return false
                }
                
                Log.d(TAG, "New file created: ${newFile.name}")
                
                // Copy the file content from content URI to new file
                context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                    context.contentResolver.openOutputStream(newFile.uri)?.use { outStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            outStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                        
                        Log.d(TAG, "File copied successfully: $totalBytes bytes")
                    } ?: run {
                        Log.e(TAG, "Failed to open output stream for new file")
                        return false
                    }
                } ?: run {
                    Log.e(TAG, "Failed to open input stream for source URI")
                    return false
                }
                
                Log.d(TAG, "=== SAVE OPERATION COMPLETED SUCCESSFULLY ===")
                true
                
            } else {
                // Handle regular file path
                val sourceFile = File(filePath)
                if (!sourceFile.exists()) {
                    Log.e(TAG, "Source file does not exist: $filePath")
                    return false
                }
                
                Log.d(TAG, "Source file exists, size: ${sourceFile.length()} bytes")
                
                // Get the folder document
                val folderDoc = DocumentFile.fromTreeUri(context, folderUri)
                if (folderDoc == null) {
                    Log.e(TAG, "Failed to get folder document from URI: $folderUri")
                    return false
                }
                
                Log.d(TAG, "Folder document obtained: ${folderDoc.name}")
                
                // Create StatusWp subfolder if it doesn't exist
                var statusWpFolder = folderDoc.findFile("StatusWp")
                if (statusWpFolder == null) {
                    Log.d(TAG, "Creating StatusWp folder...")
                    statusWpFolder = folderDoc.createDirectory("StatusWp")
                    if (statusWpFolder == null) {
                        Log.e(TAG, "Failed to create StatusWp folder")
                        return false
                    }
                    Log.d(TAG, "StatusWp folder created successfully")
                } else {
                    Log.d(TAG, "StatusWp folder already exists")
                }
                
                // Determine MIME type
                val mimeType = when {
                    filePath.lowercase().endsWith(".mp4") -> "video/mp4"
                    filePath.lowercase().endsWith(".jpg") || filePath.lowercase().endsWith(".jpeg") -> "image/jpeg"
                    filePath.lowercase().endsWith(".png") -> "image/png"
                    filePath.lowercase().endsWith(".gif") -> "image/gif"
                    filePath.lowercase().endsWith(".webp") -> "image/webp"
                    else -> "application/octet-stream"
                }
                
                Log.d(TAG, "MIME type: $mimeType")
                
                // Create the new file in StatusWp folder
                val newFile = statusWpFolder.createFile(mimeType, sourceFile.name)
                if (newFile == null) {
                    Log.e(TAG, "Failed to create new file: ${sourceFile.name}")
                    return false
                }
                
                Log.d(TAG, "New file created: ${newFile.name}")
                
                // Copy the file content
                context.contentResolver.openOutputStream(newFile.uri)?.use { outStream ->
                    sourceFile.inputStream().use { inStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L
                        
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            outStream.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead
                        }
                        
                        Log.d(TAG, "File copied successfully: $totalBytes bytes")
                    }
                } ?: run {
                    Log.e(TAG, "Failed to open output stream for new file")
                    return false
                }
                
                Log.d(TAG, "=== SAVE OPERATION COMPLETED SUCCESSFULLY ===")
                true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file: $filePath", e)
            Log.e(TAG, "Exception details: ${e.message}")
            e.printStackTrace()
            false
        }
    }

}
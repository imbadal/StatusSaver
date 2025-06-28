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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


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
            
            // Method 2: Try path detection and direct file access
            Log.d(TAG, "Method 2: Trying path detection")
            val bestPath = StatusPathDetector.getBestStatusPath(context)
            if (bestPath != null) {
                Log.d(TAG, "Using detected path: $bestPath")
                val pathStatuses = getStatusFromPath(context, bestPath)
                files.addAll(pathStatuses)
            } else {
                Log.w(TAG, "No WhatsApp status paths found")
            }
            
            // Method 3: Try alternative paths for Android 15
            if (android.os.Build.VERSION.SDK_INT >= 34) { // Android 15
                Log.d(TAG, "Method 3: Trying Android 15 specific paths")
                val android15Paths = StorageAccessHelper.getAndroid15StatusPaths()
                for (path in android15Paths) {
                    val externalStorageDir = android.os.Environment.getExternalStorageDirectory()
                    val fullPath = "$externalStorageDir/$path"
                    Log.d(TAG, "Checking Android 15 path: $fullPath")
                    
                    val pathStatuses = getStatusFromPath(context, fullPath)
                    if (pathStatuses.isNotEmpty()) {
                        Log.d(TAG, "Found statuses in Android 15 path: $fullPath")
                        files.addAll(pathStatuses)
                        break
                    }
                }
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
        files.addAll(listOf(StatusModel(""), StatusModel(""), StatusModel("")))
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
                                    files.add(StatusModel(path = filePath, isVideo = true, thumbnail = thumbnail))
                                    Log.d(TAG, "Added video status: $filePath")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing video status: $filePath", e)
                                }
                            } else {
                                val imageRequest = ImageRequest.Builder(context).data(filePath).build()
                                files.add(StatusModel(path = filePath, imageRequest = imageRequest))
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
        files.addAll(listOf(StatusModel(""), StatusModel(""), StatusModel("")))
        statusList.clear()
        statusList.addAll(files)
        return@withContext files
    }

    suspend fun getSavedStatus(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val savedFiles = mutableListOf<StatusModel>()

        val file = File(SAVED_DIRECTORY)
        file.listFiles()?.forEach { it ->
            val path = it.path
            if (isValidFile(path)) {
                if (isVideo(path)) {
                    try {
                        val mediaMetadataRetriever = MediaMetadataRetriever()
                        mediaMetadataRetriever.setDataSource(path) // Use file path directly
                        val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000) // time in Micros
                        mediaMetadataRetriever.release()
                        savedFiles.add(StatusModel(path = path, isVideo = true, thumbnail = thumbnail))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing saved video: $path", e)
                    }
                } else {
                    val imageRequest = ImageRequest.Builder(context).data(path).build()
                    savedFiles.add(StatusModel(path = path, imageRequest = imageRequest))
                }
            }
        }

        savedStatusList.clear()
        savedStatusList.addAll(savedFiles)
        return@withContext savedFiles
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


    private fun shareVideo(title: String? = "", path: String, context: Context) {
        MediaScannerConnection.scanFile(
            context, arrayOf<String>(path),
            null
        ) { path, uri ->
            val shareIntent = Intent(
                Intent.ACTION_SEND
            )
            shareIntent.type = "video/*"
            shareIntent.putExtra(
                Intent.EXTRA_SUBJECT, title
            )
            shareIntent.putExtra(
                Intent.EXTRA_TITLE, title
            )
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share with"
                )
            )
        }
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

}
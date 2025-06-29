package com.inningsstudio.statussaver.core.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageAccessHelper {
    
    private const val TAG = "StorageAccessHelper"
    
    /**
     * Check if app has required permissions
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) - Check media permissions
            val img = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
            val vid = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
            val aud = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
            
            img == PackageManager.PERMISSION_GRANTED &&
            vid == PackageManager.PERMISSION_GRANTED &&
            aud == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 and below (API < 33) - Check READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get WhatsApp statuses using MediaStore API (Android 10+)
     */
    suspend fun getStatusesViaMediaStore(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<StatusModel>()
        
        try {
            Log.d(TAG, "Attempting to get statuses via MediaStore API")
            
            // Query for images in WhatsApp status directory
            val imageSelection = "${MediaStore.Images.Media.DATA} LIKE '%WhatsApp%Status%'"
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                imageSelection,
                null,
                imageSortOrder
            )?.use { cursor ->
                Log.d(TAG, "Found ${cursor.count} images via MediaStore")
                
                while (cursor.moveToNext()) {
                    val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val path = cursor.getString(pathIndex)
                    
                    if (path.contains("Status") && isValidStatusFile(path)) {
                        Log.d(TAG, "Found status image: $path")
                        val imageRequest = coil.request.ImageRequest.Builder(context).data(path).build()
                        statuses.add(StatusModel(
                            id = path.hashCode().toLong(),
                            filePath = path,
                            fileName = path.substringAfterLast("/", path),
                            fileSize = 0L, // Will be set when reading from file
                            lastModified = 0L, // Will be set when reading from file
                            imageRequest = imageRequest
                        ))
                    }
                }
            }
            
            // Query for videos in WhatsApp status directory
            val videoSelection = "${MediaStore.Video.Media.DATA} LIKE '%WhatsApp%Status%'"
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED
            )
            val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                videoSelection,
                null,
                videoSortOrder
            )?.use { cursor ->
                Log.d(TAG, "Found ${cursor.count} videos via MediaStore")
                
                while (cursor.moveToNext()) {
                    val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val path = cursor.getString(pathIndex)
                    
                    if (path.contains("Status") && isValidStatusFile(path)) {
                        Log.d(TAG, "Found status video: $path")
                        try {
                            val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                            mediaMetadataRetriever.setDataSource(path)
                            val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                            mediaMetadataRetriever.release()
                            statuses.add(StatusModel(
                                id = path.hashCode().toLong(),
                                filePath = path,
                                fileName = path.substringAfterLast("/", path),
                                fileSize = 0L, // Will be set when reading from file
                                lastModified = 0L, // Will be set when reading from file
                                isVideo = true,
                                thumbnail = thumbnail
                            ))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing video: $path", e)
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore", e)
        }
        
        Log.d(TAG, "Total statuses found via MediaStore: ${statuses.size}")
        return@withContext statuses
    }
    
    /**
     * Get WhatsApp statuses using Storage Access Framework
     */
    suspend fun getStatusesViaSAF(context: Context, statusUri: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<StatusModel>()
        
        try {
            Log.d(TAG, "Attempting to get statuses via SAF with URI: $statusUri")
            
            val documentFile = DocumentFile.fromTreeUri(context, Uri.parse(statusUri))
            if (documentFile != null && documentFile.exists()) {
                Log.d(TAG, "Document file exists and is accessible")
                
                documentFile.listFiles().forEach { file ->
                    Log.d(TAG, "Found file via SAF: ${file.name}")
                    val filePath = file.uri.toString()
                    
                    if (isValidStatusFile(filePath)) {
                        if (isVideoFile(filePath)) {
                            try {
                                val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                                mediaMetadataRetriever.setDataSource(context, file.uri)
                                val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                                mediaMetadataRetriever.release()
                                statuses.add(StatusModel(
                                    id = filePath.hashCode().toLong(),
                                    filePath = filePath,
                                    fileName = file.name ?: filePath.substringAfterLast("/", filePath),
                                    fileSize = file.length(),
                                    lastModified = file.lastModified(),
                                    isVideo = true,
                                    thumbnail = thumbnail
                                ))
                                Log.d(TAG, "Added video status via SAF: ${file.name}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing video via SAF: ${file.name}", e)
                            }
                        } else {
                            val imageRequest = coil.request.ImageRequest.Builder(context).data(filePath).build()
                            statuses.add(StatusModel(
                                id = filePath.hashCode().toLong(),
                                filePath = filePath,
                                fileName = file.name ?: filePath.substringAfterLast("/", filePath),
                                fileSize = file.length(),
                                lastModified = file.lastModified(),
                                imageRequest = imageRequest
                            ))
                            Log.d(TAG, "Added image status via SAF: ${file.name}")
                        }
                    }
                }
            } else {
                Log.w(TAG, "Document file is null or doesn't exist")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing files via SAF", e)
        }
        
        Log.d(TAG, "Total statuses found via SAF: ${statuses.size}")
        return@withContext statuses
    }
    
    /**
     * Check if file is a valid status file
     */
    private fun isValidStatusFile(path: String): Boolean {
        if (path.isEmpty()) return false
        
        val extension = path.substringAfterLast(".", "").lowercase()
        val isMediaFile = extension in listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", 
            "mp4", "avi", "mov", "mkv", "3gp", "m4v", "wmv", "flv",
            "heic", "heif", "tiff", "tif"
        )
        
        // Check if it's not a .nomedia file
        val isNoMedia = extension == "nomedia"
        
        return isMediaFile && !isNoMedia
    }
    
    /**
     * Check if file is a video
     */
    private fun isVideoFile(path: String): Boolean {
        val extension = path.substringAfterLast(".", "").lowercase()
        return extension in listOf("mp4", "avi", "mov", "mkv", "3gp", "m4v", "wmv", "flv")
    }
} 
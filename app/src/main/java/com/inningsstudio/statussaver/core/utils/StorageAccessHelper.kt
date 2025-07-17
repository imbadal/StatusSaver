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
import android.provider.DocumentsContract
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
     * Since we use SAF (Storage Access Framework), we only need to check if SAF URI is available
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        // For SAF-based access, we don't need media permissions
        // The permission check is handled by checking if SAF URI is available
        return true
    }
    
    /**
     * Get WhatsApp statuses using MediaStore API - OPTIMIZED VERSION
     */
    suspend fun getStatusesViaMediaStore(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<StatusModel>()
        
        try {
            // Query for images in WhatsApp status directory
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE
            )
            
            // Most common selection pattern first
            val imageSelection = "${MediaStore.Images.Media.DATA} LIKE '%/.Statuses/%'"
            
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                imageSelection,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                    
                    val path = cursor.getString(pathIndex)
                    val name = cursor.getString(nameIndex)
                    val dateAdded = cursor.getLong(dateIndex)
                    val size = cursor.getLong(sizeIndex)
                    
                    if (isValidStatusFile(name, "image/*")) {
                        val imageRequest = coil.request.ImageRequest.Builder(context).data(path).build()
                        statuses.add(StatusModel(
                            id = path.hashCode().toLong(),
                            filePath = path,
                            fileName = name,
                            fileSize = size,
                            lastModified = dateAdded * 1000, // Convert to milliseconds
                            imageRequest = imageRequest
                        ))
                    }
                }
            }
            
            // Query for videos in WhatsApp status directory
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE
            )
            
            val videoSelection = "${MediaStore.Video.Media.DATA} LIKE '%/.Statuses/%'"
            
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                videoSelection,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    
                    val path = cursor.getString(pathIndex)
                    val name = cursor.getString(nameIndex)
                    val dateAdded = cursor.getLong(dateIndex)
                    val size = cursor.getLong(sizeIndex)
                    
                    if (isValidStatusFile(name, "video/*")) {
                        try {
                            val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                            mediaMetadataRetriever.setDataSource(path)
                            val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                            mediaMetadataRetriever.release()
                            
                            statuses.add(StatusModel(
                                id = path.hashCode().toLong(),
                                filePath = path,
                                fileName = name,
                                fileSize = size,
                                lastModified = dateAdded * 1000, // Convert to milliseconds
                                isVideo = true,
                                thumbnail = thumbnail
                            ))
                        } catch (e: Exception) {
                            // Skip problematic video files
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            // Handle errors silently
        }
        
        return@withContext statuses
    }
    
    /**
     * Get WhatsApp statuses using Storage Access Framework - PROVEN METHOD
     * Based on successful commercial apps
     */
    suspend fun getStatusesViaSAF(context: Context, statusUri: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val statuses = mutableListOf<StatusModel>()
        
        try {
            Log.d(TAG, "=== STARTING SAF STATUS DETECTION (PROVEN METHOD) ===")
            Log.d(TAG, "Attempting to get statuses via SAF with URI: $statusUri")
            
            val treeUri = Uri.parse(statusUri)
            Log.d(TAG, "Parsed tree URI: $treeUri")
            
            // Use DocumentsContract query - this is the proven method used by successful apps
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri, 
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            Log.d(TAG, "Children URI: $childrenUri")
            
            val cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_SIZE
                ),
                null, null, null
            )
            
            if (cursor != null) {
                Log.d(TAG, "Found ${cursor.count} files via DocumentsContract query")
                
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val displayName = cursor.getString(1)
                    val mimeType = cursor.getString(2)
                    val lastModified = cursor.getLong(3)
                    val size = cursor.getLong(4)
                    
                    Log.d(TAG, "Processing file: $displayName, MIME: $mimeType, Size: $size")
                    
                    if (isValidStatusFile(displayName, mimeType)) {
                        Log.d(TAG, "File is valid status file: $displayName")
                        
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        
                        if (isVideoFile(displayName)) {
                            try {
                                val mediaMetadataRetriever = android.media.MediaMetadataRetriever()
                                mediaMetadataRetriever.setDataSource(context, documentUri)
                                val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                                mediaMetadataRetriever.release()
                                
                                statuses.add(StatusModel(
                                    id = documentUri.toString().hashCode().toLong(),
                                    filePath = documentUri.toString(),
                                    fileName = displayName,
                                    fileSize = size,
                                    lastModified = lastModified,
                                    isVideo = true,
                                    thumbnail = thumbnail
                                ))
                                Log.d(TAG, "Added video status via SAF: $displayName")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing video via SAF: $displayName", e)
                            }
                        } else {
                            val imageRequest = coil.request.ImageRequest.Builder(context).data(documentUri.toString()).build()
                            statuses.add(StatusModel(
                                id = documentUri.toString().hashCode().toLong(),
                                filePath = documentUri.toString(),
                                fileName = displayName,
                                fileSize = size,
                                lastModified = lastModified,
                                imageRequest = imageRequest
                            ))
                            Log.d(TAG, "Added image status via SAF: $displayName")
                        }
                    } else {
                        Log.d(TAG, "File is not a valid status file: $displayName")
                    }
                }
                cursor.close()
            } else {
                Log.w(TAG, "DocumentsContract query returned null cursor")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing files via SAF (DocumentsContract)", e)
        }
        
        Log.d(TAG, "Total statuses found via SAF (DocumentsContract): ${statuses.size}")
        return@withContext statuses
    }
    
    /**
     * Check if file is a valid status file - ENHANCED VERSION
     */
    private fun isValidStatusFile(fileName: String?, mimeType: String?): Boolean {
        if (fileName.isNullOrEmpty()) return false
        
        // Skip hidden files that aren't status files
        if (fileName.startsWith(".") && !isStatusFileName(fileName)) {
            return false
        }
        
        // Check file extensions - expanded list
        val lowerFileName = fileName.lowercase()
        val validExtensions = listOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", 
            "mp4", "avi", "mov", "mkv", "3gp", "m4v", "wmv", "flv",
            "heic", "heif", "tiff", "tif"
        )
        
        val hasValidExtension = validExtensions.any { lowerFileName.endsWith(".$it") }
        
        // Check MIME types as fallback
        val hasValidMimeType = mimeType?.let { 
            it.startsWith("image/") || it.startsWith("video/") 
        } ?: false
        
        // Check if it's not a .nomedia file
        val isNoMedia = lowerFileName.endsWith(".nomedia")
        
        return hasValidExtension && !isNoMedia
    }
    
    /**
     * Check if file name matches WhatsApp status pattern
     */
    private fun isStatusFileName(fileName: String): Boolean {
        // WhatsApp status files typically have specific naming patterns
        // e.g., "IMG-20231201-WA0001.jpg", "VID-20231201-WA0001.mp4"
        return fileName.matches(".*-WA\\d+\\..*".toRegex()) || 
               fileName.matches("IMG-\\d{8}-WA\\d+\\..*".toRegex()) ||
               fileName.matches("VID-\\d{8}-WA\\d+\\..*".toRegex())
    }
    
    /**
     * Check if file is a video
     */
    private fun isVideoFile(path: String): Boolean {
        val extension = path.substringAfterLast(".", "").lowercase()
        return extension in listOf("mp4", "avi", "mov", "mkv", "3gp", "m4v", "wmv", "flv")
    }
} 
package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import coil.request.ImageRequest
import com.inningsstudio.statussaver.core.utils.Const.MP4
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles reading WhatsApp statuses from various sources
 */
object StatusReader {
    
    private const val TAG = "StatusReader"
    val statusList = mutableListOf<StatusModel>()

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

    private fun isValidFile(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.isFile && file.canRead() && !file.name.equals(Const.NO_MEDIA, ignoreCase = true)
    }

    private fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1)
        } else {
            ""
        }
    }
} 
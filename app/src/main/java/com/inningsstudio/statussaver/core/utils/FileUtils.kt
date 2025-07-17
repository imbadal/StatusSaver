package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.net.Uri
import com.inningsstudio.statussaver.data.model.StatusModel

/**
 * Facade class that coordinates between specialized utility classes
 * This maintains backward compatibility while delegating to focused classes
 */
object FileUtils {
    
    // Delegate to StatusReader
    suspend fun getStatus(context: Context, statusUri: String): List<StatusModel> {
        return StatusReader.getStatus(context, statusUri)
    }
    
    // Delegate to StatusSaver
    suspend fun getSavedStatus(context: Context): List<StatusModel> {
        return StatusSaver.getSavedStatus(context)
    }
    
    suspend fun markAsFavorite(context: Context, filePath: String): Boolean {
        return StatusSaver.markAsFavorite(context, filePath)
    }
    
    suspend fun unmarkAsFavorite(context: Context, filePath: String): Boolean {
        return StatusSaver.unmarkAsFavorite(context, filePath)
    }
    
    suspend fun getSavedStatusesFromFolder(context: Context): List<StatusModel> {
        return StatusSaver.getSavedStatusesFromFolder(context)
    }
    
    suspend fun getFavoriteStatusesFromFolder(context: Context): List<StatusModel> {
        return StatusSaver.getFavoriteStatusesFromFolder(context)
    }
    
    suspend fun deleteSavedStatus(context: Context, filePath: String): Boolean {
        return StatusSaver.deleteSavedStatus(context, filePath)
    }
    
    suspend fun saveStatusToFolder(context: Context, folderUri: Uri, filePath: String): Boolean {
        return StatusSaver.saveStatusToFolder(context, folderUri, filePath)
    }
    
    // Delegate to StatusSharer
    suspend fun shareStatus(context: Context, currentPath: String) {
        StatusSharer.shareStatus(context, currentPath)
    }
    
    // Delegate to FileOperations
    suspend fun deleteFile(path: String): Boolean {
        return FileOperations.deleteFile(path)
    }
    
    suspend fun copyFileToInternalStorage(uri: Uri, mContext: Context): String? {
        return FileOperations.copyFileToInternalStorage(uri, mContext)
    }
    
    // Expose lists for backward compatibility
    val statusList: MutableList<StatusModel> = StatusReader.statusList
    val savedStatusList: MutableList<StatusModel> = StatusSaver.savedStatusList
}
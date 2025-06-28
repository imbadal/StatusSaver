package com.inningsstudio.statussaver.data.datasource

import android.content.Context
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.core.utils.FileUtils

/**
 * Local data source for status operations
 * Handles file system operations and local storage
 */
class StatusLocalDataSource(
    private val context: Context
) {
    suspend fun getStatuses(statusUri: String): List<StatusModel> {
        return FileUtils.getStatus(context, statusUri)
    }
    
    suspend fun getSavedStatuses(): List<StatusModel> {
        return FileUtils.getSavedStatus(context)
    }
    
    suspend fun saveStatus(status: StatusModel): Boolean {
        // Implementation for saving status
        return true
    }
    
    suspend fun deleteStatus(path: String): Boolean {
        return FileUtils.deleteFile(path)
    }
    
    suspend fun shareStatus(path: String): Boolean {
        FileUtils.shareStatus(context, path)
        return true
    }
} 
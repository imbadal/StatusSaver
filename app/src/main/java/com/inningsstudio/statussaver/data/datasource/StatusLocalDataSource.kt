package com.inningsstudio.statussaver.data.datasource

import android.content.Context
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.WhatsAppStatusReader

/**
 * Local data source for status operations
 * Handles file system operations and local storage
 */
class StatusLocalDataSource(
    private val context: Context
) {
    private val statusReader = WhatsAppStatusReader()
    
    suspend fun getStatuses(statusUri: String): List<StatusModel> {
        // Use direct file access with WhatsAppStatusReader
        return statusReader.readAllStatuses()
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
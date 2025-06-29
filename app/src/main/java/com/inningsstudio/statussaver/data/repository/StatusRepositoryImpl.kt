package com.inningsstudio.statussaver.data.repository

import android.content.Context
import com.inningsstudio.statussaver.data.datasource.StatusLocalDataSource
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.domain.entity.StatusEntity
import com.inningsstudio.statussaver.domain.repository.StatusRepository
import com.inningsstudio.statussaver.core.utils.WhatsAppStatusReader

/**
 * Repository implementation for status operations
 * Coordinates between different data sources and converts between data models and domain entities
 */
class StatusRepositoryImpl(
    private val localDataSource: StatusLocalDataSource,
    private val context: Context
) : StatusRepository {
    
    private val statusReader = WhatsAppStatusReader()
    
    override suspend fun getStatuses(statusUri: String): List<StatusEntity> {
        // Use direct file access instead of URI
        val statusModels = statusReader.readAllStatuses()
        return statusModels.map { it.toEntity() }
    }
    
    override suspend fun getSavedStatuses(): List<StatusEntity> {
        val statusModels = localDataSource.getSavedStatuses()
        return statusModels.map { it.toEntity() }
    }
    
    override suspend fun saveStatus(status: StatusEntity): Boolean {
        val statusModel = StatusModel.fromEntity(status)
        return localDataSource.saveStatus(statusModel)
    }
    
    override suspend fun deleteStatus(path: String): Boolean {
        return localDataSource.deleteStatus(path)
    }
    
    override suspend fun shareStatus(path: String): Boolean {
        return localDataSource.shareStatus(path)
    }
    
    override suspend fun detectStatusPaths(): List<String> {
        val statusPath = statusReader.getStatusFolderPath()
        return if (statusPath != null) listOf(statusPath) else emptyList()
    }
    
    override suspend fun isWhatsAppInstalled(): Boolean {
        return statusReader.isStatusFolderAccessible()
    }
} 
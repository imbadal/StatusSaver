package com.inningsstudio.statussaver.data.repository

import com.inningsstudio.statussaver.data.dao.SavedStatusDao
import com.inningsstudio.statussaver.data.model.SavedStatusEntity
import kotlinx.coroutines.flow.Flow

class SavedStatusRepository(
    private val savedStatusDao: SavedStatusDao
) {
    
    fun getAllSavedStatuses(): Flow<List<SavedStatusEntity>> {
        return savedStatusDao.getAllSavedStatuses()
    }
    
    fun getFavoriteStatuses(): Flow<List<SavedStatusEntity>> {
        return savedStatusDao.getFavoriteStatuses()
    }
    
    fun getNonFavoriteStatuses(): Flow<List<SavedStatusEntity>> {
        return savedStatusDao.getNonFavoriteStatuses()
    }
    
    suspend fun getSavedStatusByUri(statusUri: String): SavedStatusEntity? {
        return savedStatusDao.getSavedStatusByUri(statusUri)
    }
    
    suspend fun isFavorite(statusUri: String): Boolean {
        return savedStatusDao.isFavorite(statusUri)
    }
    
    suspend fun insertSavedStatus(savedStatus: SavedStatusEntity) {
        savedStatusDao.insertSavedStatus(savedStatus)
    }
    
    suspend fun updateSavedStatus(savedStatus: SavedStatusEntity) {
        savedStatusDao.updateSavedStatus(savedStatus)
    }
    
    suspend fun toggleFavoriteStatus(statusUri: String) {
        val currentStatus = savedStatusDao.getSavedStatusByUri(statusUri)
        currentStatus?.let {
            savedStatusDao.updateFavoriteStatus(statusUri, !it.isFavorite)
        }
    }
    
    suspend fun setFavoriteStatus(statusUri: String, isFavorite: Boolean) {
        savedStatusDao.updateFavoriteStatus(statusUri, isFavorite)
    }
    
    suspend fun deleteSavedStatus(savedStatus: SavedStatusEntity) {
        savedStatusDao.deleteSavedStatus(savedStatus)
    }
    
    suspend fun deleteSavedStatusByUri(statusUri: String) {
        savedStatusDao.deleteSavedStatusByUri(statusUri)
    }
    
    suspend fun deleteAllSavedStatuses() {
        savedStatusDao.deleteAllSavedStatuses()
    }
    
    suspend fun getSavedStatusesCount(): Int {
        return savedStatusDao.getSavedStatusesCount()
    }
    
    suspend fun getFavoriteStatusesCount(): Int {
        return savedStatusDao.getFavoriteStatusesCount()
    }
} 
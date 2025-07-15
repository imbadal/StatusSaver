package com.inningsstudio.statussaver.data.dao

import androidx.room.*
import com.inningsstudio.statussaver.data.model.SavedStatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedStatusDao {
    
    @Query("SELECT * FROM saved_statuses ORDER BY savedDate DESC")
    fun getAllSavedStatuses(): Flow<List<SavedStatusEntity>>
    
    @Query("SELECT * FROM saved_statuses ORDER BY savedDate DESC")
    suspend fun getAllSavedStatusesSync(): List<SavedStatusEntity>
    
    @Query("SELECT * FROM saved_statuses WHERE isFavorite = 1 ORDER BY savedDate DESC")
    fun getFavoriteStatuses(): Flow<List<SavedStatusEntity>>
    
    @Query("SELECT * FROM saved_statuses WHERE isFavorite = 0 ORDER BY savedDate DESC")
    fun getNonFavoriteStatuses(): Flow<List<SavedStatusEntity>>
    
    @Query("SELECT * FROM saved_statuses WHERE statusUri = :statusUri")
    suspend fun getSavedStatusByUri(statusUri: String): SavedStatusEntity?
    
    @Query("SELECT EXISTS(SELECT 1 FROM saved_statuses WHERE statusUri = :statusUri AND isFavorite = 1)")
    suspend fun isFavorite(statusUri: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedStatus(savedStatus: SavedStatusEntity)
    
    @Update
    suspend fun updateSavedStatus(savedStatus: SavedStatusEntity)
    
    @Query("UPDATE saved_statuses SET isFavorite = :isFavorite WHERE statusUri = :statusUri")
    suspend fun updateFavoriteStatus(statusUri: String, isFavorite: Boolean)
    
    @Delete
    suspend fun deleteSavedStatus(savedStatus: SavedStatusEntity)
    
    @Query("DELETE FROM saved_statuses WHERE statusUri = :statusUri")
    suspend fun deleteSavedStatusByUri(statusUri: String)
    
    @Query("DELETE FROM saved_statuses")
    suspend fun deleteAllSavedStatuses()
    
    @Query("SELECT COUNT(*) FROM saved_statuses")
    suspend fun getSavedStatusesCount(): Int
    
    @Query("SELECT COUNT(*) FROM saved_statuses WHERE isFavorite = 1")
    suspend fun getFavoriteStatusesCount(): Int
} 
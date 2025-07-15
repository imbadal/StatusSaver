package com.inningsstudio.statussaver.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved status with favorite information
 */
@Entity(tableName = "saved_statuses")
data class SavedStatusEntity(
    @PrimaryKey
    val statusUri: String,
    val fileName: String,
    val isFavorite: Boolean = false,
    val savedDate: Long = System.currentTimeMillis(),
    val fileSize: Long = 0L,
    val isVideo: Boolean = false
) 
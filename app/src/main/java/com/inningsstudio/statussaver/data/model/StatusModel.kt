package com.inningsstudio.statussaver.data.model

import coil.request.ImageRequest

/**
 * Data model representing a WhatsApp status
 * This is the data layer representation that can be converted to/from domain entities
 */
data class StatusModel(
    val id: Long,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val isSelected: Boolean = false,
    val isVideo: Boolean = false,
    val thumbnail: android.graphics.Bitmap? = null,
    val imageRequest: ImageRequest? = null
) {
    fun toEntity(): com.inningsstudio.statussaver.domain.entity.StatusEntity {
        return com.inningsstudio.statussaver.domain.entity.StatusEntity(
            path = filePath,
            isVideo = isVideo,
            thumbnail = thumbnail,
            imageRequest = imageRequest
        )
    }
    
    companion object {
        fun fromEntity(entity: com.inningsstudio.statussaver.domain.entity.StatusEntity): StatusModel {
            return StatusModel(
                id = entity.path.hashCode().toLong(),
                filePath = entity.path,
                fileName = entity.path.substringAfterLast("/", entity.path),
                fileSize = 0L, // Will be set when reading from file
                lastModified = 0L, // Will be set when reading from file
                isVideo = entity.isVideo,
                thumbnail = entity.thumbnail,
                imageRequest = entity.imageRequest
            )
        }
    }
} 
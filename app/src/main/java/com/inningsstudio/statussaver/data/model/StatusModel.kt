package com.inningsstudio.statussaver.data.model

import coil.request.ImageRequest

/**
 * Data model representing a WhatsApp status
 * This is the data layer representation that can be converted to/from domain entities
 */
data class StatusModel(
    val path: String,
    val isVideo: Boolean = false,
    val thumbnail: android.graphics.Bitmap? = null,
    val imageRequest: ImageRequest? = null
) {
    fun toEntity(): com.inningsstudio.statussaver.domain.entity.StatusEntity {
        return com.inningsstudio.statussaver.domain.entity.StatusEntity(
            path = path,
            isVideo = isVideo,
            thumbnail = thumbnail,
            imageRequest = imageRequest
        )
    }
    
    companion object {
        fun fromEntity(entity: com.inningsstudio.statussaver.domain.entity.StatusEntity): StatusModel {
            return StatusModel(
                path = entity.path,
                isVideo = entity.isVideo,
                thumbnail = entity.thumbnail,
                imageRequest = entity.imageRequest
            )
        }
    }
} 
package com.inningsstudio.statussaver.domain.entity

import coil.request.ImageRequest

/**
 * Domain entity representing a WhatsApp status
 * This is the core business model that doesn't depend on any framework
 */
data class StatusEntity(
    val path: String,
    val isVideo: Boolean = false,
    val thumbnail: android.graphics.Bitmap? = null,
    val imageRequest: ImageRequest? = null
) 
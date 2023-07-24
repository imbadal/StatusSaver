package com.inningsstudio.statussaver

import android.graphics.Bitmap
import coil.request.ImageRequest

data class StatusModel(
    val path: String,
    val isVideo: Boolean = false,
    val imageRequest: ImageRequest? = null,
    val thumbnail: Bitmap? = null
)

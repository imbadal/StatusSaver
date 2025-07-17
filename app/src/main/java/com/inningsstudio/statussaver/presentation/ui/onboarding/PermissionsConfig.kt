package com.inningsstudio.statussaver.presentation.ui.onboarding

import android.os.Build

object PermissionsConfig {

    // Since minSdk is 30 (Android 11), we need to handle both modern and legacy permissions
    val readImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val readVideoPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_VIDEO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            readImagePermission,
            readVideoPermission
        )
    } else {
        arrayOf(readImagePermission) // READ_EXTERNAL_STORAGE covers both images and videos
    }

}
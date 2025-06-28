package com.inningsstudio.statussaver.presentation.ui.onboarding

import android.os.Build

object PermissionsConfig {

    val readImagePermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionsToRequest = arrayOf(
        readImagePermission
    )


}
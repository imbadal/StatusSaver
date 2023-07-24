package com.inningsstudio.statussaver

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun PermissionDialog(
    permissionTextProvider: PermissionTextProvider,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(text = "Permission Required")
    }, text = {
        Text(permissionTextProvider.getDescription(isPermanentlyDeclined))
    }, confirmButton = {
        Button(
            onClick = if (isPermanentlyDeclined) onGoToAppSettingClick else onOkClick
        ) {
            val conformText = if (isPermanentlyDeclined) "Grant Permission" else "Okay"
            Text(conformText)
        }
    }, dismissButton = {
        Button(
            onClick = onDismiss
        ) {
            Text("cancel")
        }
    }, modifier = modifier)
}


interface PermissionTextProvider {
    fun getDescription(isPermanentlyDeclined: Boolean): String
}

class StoragePermissionTextProvider : PermissionTextProvider {
    override fun getDescription(isPermanentlyDeclined: Boolean): String {
        return if (isPermanentlyDeclined) "It seems like you permanently declined the STORAGE PERMISSION. You can to to the app settings to grand it."
        else "This app need access to you storage so that you can see all the status."
    }
}
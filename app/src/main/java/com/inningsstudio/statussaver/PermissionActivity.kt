package com.inningsstudio.statussaver

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.storage.StorageManager
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.inningsstudio.statussaver.Const.STATUS_URI


class PermissionActivity : ComponentActivity() {

    private val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
    private lateinit var preferenceUtils: PreferenceUtils

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)

        preferenceUtils = PreferenceUtils(application)
        val uriTree = preferenceUtils.getUriFromPreference()
        if (uriTree.isNullOrBlank()) {
            getFolderPermission()
        } else {
            fetchImages(uriTree)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getFolderPermission() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        // Optionally, set the initial URI if you want to guide the user to a specific folder
        // val initialUri = ... // You can construct your initialUri if needed
        // intent.putExtra("android.provider.extra.INITIAL_URI", initialUri)
        intent.putExtra("android.content.extra.SHOW_ADVANCE", true)
        startActivityForResult(intent, 1212)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            val treeUri = data?.data
            treeUri?.let {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val actualPath = treeUri.toString()
                preferenceUtils.setUriToPreference(actualPath)
                fetchImages(actualPath)
            }
        }
    }

    private fun fetchImages(statusUri: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(STATUS_URI, statusUri)
        startActivity(intent)
        finish()
    }
}
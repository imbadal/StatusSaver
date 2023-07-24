package com.inningsstudio.statussaver

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object FileUtils {

    fun getFilesFromUri(context: Context, statusUri: String): List<String> {
        val files = mutableListOf<String>()
        val fileDoc = DocumentFile.fromTreeUri(context, Uri.parse(statusUri))

        fileDoc?.listFiles()?.forEach { file ->
            files.add(file.uri.toString())
        }
        return files
    }

}
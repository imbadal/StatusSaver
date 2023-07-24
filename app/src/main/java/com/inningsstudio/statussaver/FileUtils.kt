package com.inningsstudio.statussaver

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import coil.request.ImageRequest
import com.inningsstudio.statussaver.Const.MP4
import com.inningsstudio.statussaver.Const.NO_MEDIA

object FileUtils {

    private fun isVideo(path: String): Boolean {
        return (path.substring(path.length - 3) == MP4)
    }

    fun getStatus(context: Context, statusUri: String): List<StatusModel> {
        val files = mutableListOf<StatusModel>()
        val fileDoc = DocumentFile.fromTreeUri(context, Uri.parse(statusUri))

        fileDoc?.listFiles()?.forEach { file ->
            val path = file.uri.toString()
            if (isValidFile(path)) {
                if (isVideo(path)) {
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, Uri.parse(path))
                    val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000) // time in Micros
                    mediaMetadataRetriever.release()
                    files.add(StatusModel(path = path, isVideo = true, thumbnail = thumbnail))
                } else {
                    val imageRequest = ImageRequest.Builder(context).data(path).build()
                    files.add(StatusModel(path = path, imageRequest = imageRequest))
                }
            }
        }
        files.addAll(listOf(StatusModel(""), StatusModel(""), StatusModel("")))
        return files
    }

    private fun isValidFile(path: String): Boolean {
        return path.substringAfterLast(".") != NO_MEDIA || path.isEmpty()
    }

}
package com.inningsstudio.statussaver

import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import coil.request.ImageRequest
import com.inningsstudio.statussaver.Const.MP4
import com.inningsstudio.statussaver.Const.NO_MEDIA
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object FileUtils {

    val statusList = mutableListOf<StatusModel>()

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
        statusList.clear()
        statusList.addAll(files)
        return files
    }

    private fun isValidFile(path: String): Boolean {
        return path.substringAfterLast(".") != NO_MEDIA || path.isEmpty()
    }

    fun copyFileToInternalStorage(
        uri: Uri,
        mContext: Context
    ): String? {
        val newDirName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "inningsstudio" + File.separator + "Status Saver"
        val returnCursor: Cursor? = mContext.contentResolver.query(
            uri, arrayOf<String>(
                OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            ), null, null, null
        )

        returnCursor?.let {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
            returnCursor.moveToFirst()
            val name = returnCursor.getString(nameIndex)
            val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
            val output: File
            if (newDirName != "") {
                val dir: File = File(newDirName)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                output = File("$newDirName/$name")
            } else {
                output = File(mContext.filesDir.toString() + "/" + name)
            }
            try {
                val inputStream: InputStream? = mContext.contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(output)
                var read = 0
                val bufferSize = 1024
                val buffers = ByteArray(bufferSize)
                while (inputStream?.read(buffers).also { read = it ?: 0 } != -1) {
                    outputStream.write(buffers, 0, read)
                }
                inputStream?.close()
                outputStream.close()
            } catch (e: Exception) {
                Log.e("Exception", e.message!!)
            }
            return output.path
        }
        return null
    }

}
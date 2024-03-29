package com.inningsstudio.statussaver

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import coil.request.ImageRequest
import com.inningsstudio.statussaver.Const.MP4
import com.inningsstudio.statussaver.Const.NO_MEDIA
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


object FileUtils {

    val statusList = mutableListOf<StatusModel>()
    val savedStatusList = mutableListOf<StatusModel>()

    private val SAVED_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "inningsstudio" + File.separator + "Status Saver"

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

    fun getSavedStatus(context: Context): List<StatusModel> {
        val savedFiles = mutableListOf<StatusModel>()

        val file = File(SAVED_DIRECTORY)
        file.listFiles()?.forEach { it ->
            val path = it.path
            if (isValidFile(path)) {
                if (isVideo(path)) {
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(context, Uri.parse(path))
                    val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000) // time in Micros
                    mediaMetadataRetriever.release()
                    savedFiles.add(StatusModel(path = path, isVideo = true, thumbnail = thumbnail))
                } else {
                    val imageRequest = ImageRequest.Builder(context).data(path).build()
                    savedFiles.add(StatusModel(path = path, imageRequest = imageRequest))
                }
            }
        }

        savedStatusList.clear()
        savedStatusList.addAll(savedFiles)
        return savedFiles
    }

    private fun isValidFile(path: String): Boolean {
        return path.substringAfterLast(".") != NO_MEDIA || path.isEmpty()
    }

    fun deleteFile(
        path: String
    ): Boolean {
        val file = File(path)
        return file.delete()
    }

    fun copyFileToInternalStorage(
        uri: Uri, mContext: Context
    ): String? {
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
            val output: File = if (SAVED_DIRECTORY != "") {
                val dir: File = File(SAVED_DIRECTORY)
                if (!dir.exists()) {
                    dir.mkdir()
                }
                File("$SAVED_DIRECTORY/$name")
            } else {
                File(mContext.filesDir.toString() + "/" + name)
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


    private fun shareVideo(title: String? = "", path: String, context: Context) {
        MediaScannerConnection.scanFile(
            context, arrayOf<String>(path),
            null
        ) { path, uri ->
            val shareIntent = Intent(
                Intent.ACTION_SEND
            )
            shareIntent.type = "video/*"
            shareIntent.putExtra(
                Intent.EXTRA_SUBJECT, title
            )
            shareIntent.putExtra(
                Intent.EXTRA_TITLE, title
            )
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share with"
                )
            )
        }
    }

    fun shareStatus(context: Context, currentPath: String) {
        if (isVideo(currentPath)) {
            shareVideo(path = currentPath, context = context)
        } else {
            shareImage(currentPath, context)
        }
    }

    private fun shareImage(currentPath: String, context: Context) {
        val share = Intent(Intent.ACTION_SEND)
        share.type = "image/jpeg"
        val bytes = ByteArrayOutputStream()
        val f = File(
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + "status_${System.currentTimeMillis()}.jpg"
        )
        try {
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse(currentPath))
        context.startActivity(Intent.createChooser(share, "Share with"))
    }

}
package com.inningsstudio.statussaver.core.utils

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
import com.inningsstudio.statussaver.core.constants.Const.MP4
import com.inningsstudio.statussaver.core.constants.Const.NO_MEDIA
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


object FileUtils {

    private const val TAG = "FileUtils"
    val statusList = mutableListOf<StatusModel>()
    val savedStatusList = mutableListOf<StatusModel>()

    private val SAVED_DIRECTORY =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            .toString() + File.separator + "inningsstudio" + File.separator + "Status Saver"

    private fun isVideo(path: String): Boolean {
        return (path.substring(path.length - 3) == MP4)
    }

    suspend fun getStatus(context: Context, statusUri: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val files = mutableListOf<StatusModel>()
        
        // Check if statusUri is empty or invalid
        if (statusUri.isBlank()) {
            Log.w(TAG, "Status URI is empty, trying to detect WhatsApp status paths")
            
            // Try to detect WhatsApp status paths automatically
            val bestPath = StatusPathDetector.getBestStatusPath(context)
            if (bestPath != null) {
                Log.d(TAG, "Using detected path: $bestPath")
                return@withContext getStatusFromPath(context, bestPath)
            } else {
                Log.w(TAG, "No WhatsApp status paths found")
                return@withContext files // Return empty list
            }
        }
        
        // Try to use the provided URI
        try {
            val fileDoc = DocumentFile.fromTreeUri(context, Uri.parse(statusUri))
            if (fileDoc != null && fileDoc.exists()) {
                fileDoc.listFiles()?.forEach { file ->
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
            } else {
                Log.w(TAG, "Invalid or non-existent URI: $statusUri")
                // Fallback to path detection
                val bestPath = StatusPathDetector.getBestStatusPath(context)
                if (bestPath != null) {
                    return@withContext getStatusFromPath(context, bestPath)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing status URI: $statusUri", e)
            // Fallback to path detection
            val bestPath = StatusPathDetector.getBestStatusPath(context)
            if (bestPath != null) {
                return@withContext getStatusFromPath(context, bestPath)
            }
        }
        
        // Add padding items for UI
        files.addAll(listOf(StatusModel(""), StatusModel(""), StatusModel("")))
        statusList.clear()
        statusList.addAll(files)
        return@withContext files
    }
    
    /**
     * Get status from a file path (for direct file system access)
     */
    private suspend fun getStatusFromPath(context: Context, path: String): List<StatusModel> = withContext(Dispatchers.IO) {
        val files = mutableListOf<StatusModel>()
        
        try {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) {
                val fileList = directory.listFiles()
                if (fileList != null) {
                    fileList.forEach { file ->
                        val filePath = file.absolutePath
                        if (isValidFile(filePath)) {
                            if (isVideo(filePath)) {
                                val mediaMetadataRetriever = MediaMetadataRetriever()
                                mediaMetadataRetriever.setDataSource(filePath)
                                val thumbnail = mediaMetadataRetriever.getFrameAtTime(1000000)
                                mediaMetadataRetriever.release()
                                files.add(StatusModel(path = filePath, isVideo = true, thumbnail = thumbnail))
                            } else {
                                val imageRequest = ImageRequest.Builder(context).data(filePath).build()
                                files.add(StatusModel(path = filePath, imageRequest = imageRequest))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading status from path: $path", e)
        }
        
        // Add padding items for UI
        files.addAll(listOf(StatusModel(""), StatusModel(""), StatusModel("")))
        statusList.clear()
        statusList.addAll(files)
        return@withContext files
    }

    suspend fun getSavedStatus(context: Context): List<StatusModel> = withContext(Dispatchers.IO) {
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
        return@withContext savedFiles
    }

    private fun isValidFile(path: String): Boolean {
        return path.substringAfterLast(".") != NO_MEDIA || path.isEmpty()
    }

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        return@withContext file.delete()
    }

    suspend fun copyFileToInternalStorage(uri: Uri, mContext: Context): String? = withContext(Dispatchers.IO) {
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
            return@withContext output.path
        }
        return@withContext null
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

    suspend fun shareStatus(context: Context, currentPath: String) = withContext(Dispatchers.IO) {
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
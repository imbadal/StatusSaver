package com.inningsstudio.statussaver.core.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handles general file operations
 */
object FileOperations {
    
    private const val TAG = "FileOperations"

    suspend fun deleteFile(path: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(path)
            file.exists() && file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting file: $path", e)
            false
        }
    }

    suspend fun copyFileToInternalStorage(uri: Uri, mContext: Context): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val inputStream = mContext.contentResolver.openInputStream(uri)
            val fileName = getFileNameFromUri(mContext, uri) ?: "status_${System.currentTimeMillis()}"
            val outputFile = File(mContext.filesDir, fileName)
            
            inputStream?.use { input ->
                outputFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                }
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to internal storage", e)
            null
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        cursor.getString(displayNameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file name from URI", e)
            null
        }
    }
} 
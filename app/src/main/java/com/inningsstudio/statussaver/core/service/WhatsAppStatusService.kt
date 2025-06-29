package com.inningsstudio.statussaver.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.inningsstudio.statussaver.R
import com.inningsstudio.statussaver.core.utils.FileUtils
import java.io.File

class WhatsAppStatusService : Service() {
    
    companion object {
        private const val TAG = "WhatsAppStatusService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "status_saver_channel"
        
        private const val WHATSAPP_STATUS_PATH = "/WhatsApp/Media/.Statuses/"
        private const val WHATSAPP_STATUS_PATH_SCOPED = "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
        private const val WHATSAPP_BUSINESS_STATUS_PATH = "/WhatsApp Business/Media/.Statuses/"
        private const val WHATSAPP_BUSINESS_STATUS_PATH_SCOPED = "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/"
    }
    
    private var statusObserver: FileObserver? = null
    private var currentStatusPath: String? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        
        // Check permissions and start monitoring
        if (hasRequiredPermissions()) {
            startStatusMonitoring()
        } else {
            Log.w(TAG, "Required permissions not granted")
            stopSelf()
        }
        
        return START_STICKY
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 and below
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun startStatusMonitoring() {
        val statusPath = getStatusPath()
        if (statusPath != null) {
            currentStatusPath = statusPath
            startFileObserver(statusPath)
            Log.d(TAG, "Started monitoring: $statusPath")
        } else {
            Log.w(TAG, "No WhatsApp status folder found")
        }
    }
    
    private fun getStatusPath(): String? {
        val externalStorage = android.os.Environment.getExternalStorageDirectory().absolutePath
        
        // Check all possible paths
        val possiblePaths = listOf(
            externalStorage + WHATSAPP_STATUS_PATH_SCOPED,
            externalStorage + WHATSAPP_STATUS_PATH,
            externalStorage + WHATSAPP_BUSINESS_STATUS_PATH_SCOPED,
            externalStorage + WHATSAPP_BUSINESS_STATUS_PATH
        )
        
        for (path in possiblePaths) {
            val folder = File(path)
            if (folder.exists() && folder.isDirectory()) {
                Log.d(TAG, "Found WhatsApp status folder: $path")
                return path
            }
        }
        
        return null
    }
    
    private fun startFileObserver(path: String) {
        statusObserver = object : FileObserver(path, FileObserver.CREATE) {
            override fun onEvent(event: Int, filename: String?) {
                if (filename != null && event == FileObserver.CREATE) {
                    Log.d(TAG, "New status detected: $filename")
                    handleNewStatus(path, filename)
                }
            }
        }
        statusObserver?.startWatching()
    }
    
    private fun handleNewStatus(folderPath: String, filename: String) {
        val statusFile = File(folderPath, filename)
        if (statusFile.exists() && isValidStatusFile(statusFile)) {
            // Save status to app directory
            saveStatusToAppDirectory(statusFile)
            
            // Show notification
            showStatusNotification(filename)
        }
    }
    
    private fun isValidStatusFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".mp4") || 
               name.endsWith(".3gp") || name.endsWith(".mkv")
    }
    
    private fun saveStatusToAppDirectory(statusFile: File) {
        try {
            val appDir = File(getExternalFilesDir(null), "StatusSaver")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            
            val destinationFile = File(appDir, statusFile.name)
            FileUtils.copyFile(statusFile, destinationFile)
            
            Log.d(TAG, "Status saved: ${destinationFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving status: ${e.message}")
        }
    }
    
    private fun showStatusNotification(filename: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("New WhatsApp Status")
            .setContentText("Saved: $filename")
            .setSmallIcon(R.drawable.baseline_download_24)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Status Saver",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new WhatsApp statuses"
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        statusObserver?.stopWatching()
        Log.d(TAG, "Service destroyed")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
} 
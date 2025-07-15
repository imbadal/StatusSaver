package com.inningsstudio.statussaver.core.utils

import android.os.Environment
import android.util.Log
import java.io.File

class StatusPathDetector {
    
    private val availableWhatsAppTypes = mutableListOf<String>()
    
    companion object {
        private const val TAG = "StatusPathDetector"
    }
    
    /**
     * Detect all possible WhatsApp status folders based on the decompiled app logic
     * This is the exact implementation from the suggestion.txt file
     */
    fun detectWhatsAppStatusFolders(): List<String> {
        Log.d(TAG, "=== DETECTING WHATSAPP STATUS FOLDERS ===")
        availableWhatsAppTypes.clear()
        
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        Log.d(TAG, "Base storage path: $basePath")
        
        // Check 1: Android/media/com.whatsapp/WhatsApp/Media/.Statuses/ (Android 13+ scoped storage)
        val file1 = File("$basePath/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 1: ${file1.absolutePath}")
        Log.d(TAG, "  - Exists: ${file1.exists()}")
        Log.d(TAG, "  - Is directory: ${file1.isDirectory}")
        Log.d(TAG, "  - Can read: ${file1.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file1.isHidden}")
        
        if (file1.exists() && file1.isDirectory && file1.canRead()) {
            val files = file1.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Whatsapp")
                Log.d(TAG, "✅ FOUND: Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file1, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 2: WhatsApp/Media/.Statuses/ (legacy path)
        val file2 = File("$basePath/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 2: ${file2.absolutePath}")
        Log.d(TAG, "  - Exists: ${file2.exists()}")
        Log.d(TAG, "  - Is directory: ${file2.isDirectory}")
        Log.d(TAG, "  - Can read: ${file2.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file2.isHidden}")
        
        if (file2.exists() && file2.isDirectory && file2.canRead()) {
            val files = file2.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("WhatsApp")
                Log.d(TAG, "✅ FOUND: WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file2, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 3: Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/
        val file3 = File("$basePath/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/")
        Log.d(TAG, "Checking path 3: ${file3.absolutePath}")
        Log.d(TAG, "  - Exists: ${file3.exists()}")
        Log.d(TAG, "  - Is directory: ${file3.isDirectory}")
        Log.d(TAG, "  - Can read: ${file3.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file3.isHidden}")
        
        if (file3.exists() && file3.isDirectory && file3.canRead()) {
            val files = file3.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
                availableWhatsAppTypes.add("wa Business")
                Log.d(TAG, "✅ FOUND: Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/")
                logFirstFewFiles(file3, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 4: WhatsApp Business/Media/.Statuses/
        val file4 = File("$basePath/WhatsApp Business/Media/.Statuses/")
        Log.d(TAG, "Checking path 4: ${file4.absolutePath}")
        Log.d(TAG, "  - Exists: ${file4.exists()}")
        Log.d(TAG, "  - Is directory: ${file4.isDirectory}")
        Log.d(TAG, "  - Can read: ${file4.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file4.isHidden}")
        
        if (file4.exists() && file4.isDirectory && file4.canRead()) {
            val files = file4.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("WA Business")
                Log.d(TAG, "✅ FOUND: WhatsApp Business/Media/.Statuses/")
                logFirstFewFiles(file4, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 5: parallel_lite/0/WhatsApp/Media/.Statuses/
        val file5 = File("$basePath/parallel_lite/0/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 5: ${file5.absolutePath}")
        Log.d(TAG, "  - Exists: ${file5.exists()}")
        Log.d(TAG, "  - Is directory: ${file5.isDirectory}")
        Log.d(TAG, "  - Can read: ${file5.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file5.isHidden}")
        
        if (file5.exists() && file5.isDirectory && file5.canRead()) {
            val files = file5.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Parellel Lite")
                Log.d(TAG, "✅ FOUND: parallel_lite/0/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file5, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 6: parallel_intl/0/WhatsApp/Media/.Statuses/
        val file6 = File("$basePath/parallel_intl/0/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 6: ${file6.absolutePath}")
        Log.d(TAG, "  - Exists: ${file6.exists()}")
        Log.d(TAG, "  - Is directory: ${file6.isDirectory}")
        Log.d(TAG, "  - Can read: ${file6.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file6.isHidden}")
        
        if (file6.exists() && file6.isDirectory && file6.canRead()) {
            val files = file6.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Parellel lite")
                Log.d(TAG, "✅ FOUND: parallel_intl/0/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file6, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 7: GBWhatsApp/Media/.Statuses/
        val file7 = File("$basePath/GBWhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 7: ${file7.absolutePath}")
        Log.d(TAG, "  - Exists: ${file7.exists()}")
        Log.d(TAG, "  - Is directory: ${file7.isDirectory}")
        Log.d(TAG, "  - Can read: ${file7.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file7.isHidden}")
        
        if (file7.exists() && file7.isDirectory && file7.canRead()) {
            val files = file7.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("GB WhatsApp")
                Log.d(TAG, "✅ FOUND: GBWhatsApp/Media/.Statuses/")
                logFirstFewFiles(file7, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 8: DualApp/WhatsApp/Media/.Statuses/
        val file8 = File("$basePath/DualApp/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 8: ${file8.absolutePath}")
        Log.d(TAG, "  - Exists: ${file8.exists()}")
        Log.d(TAG, "  - Is directory: ${file8.isDirectory}")
        Log.d(TAG, "  - Can read: ${file8.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file8.isHidden}")
        
        if (file8.exists() && file8.isDirectory && file8.canRead()) {
            val files = file8.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Dual Whatsapp")
                Log.d(TAG, "✅ FOUND: DualApp/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file8, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 9: /storage/emulated/999/WhatsApp/Media/.Statuses/
        val file9 = File("/storage/emulated/999/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 9: ${file9.absolutePath}")
        Log.d(TAG, "  - Exists: ${file9.exists()}")
        Log.d(TAG, "  - Is directory: ${file9.isDirectory}")
        Log.d(TAG, "  - Can read: ${file9.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file9.isHidden}")
        
        if (file9.exists() && file9.isDirectory && file9.canRead()) {
            val files = file9.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Dual WhatsApp")
                Log.d(TAG, "✅ FOUND: /storage/emulated/999/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file9, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 10: /storage/ace-999/WhatsApp/Media/.Statuses/
        val file10 = File("/storage/ace-999/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 10: ${file10.absolutePath}")
        Log.d(TAG, "  - Exists: ${file10.exists()}")
        Log.d(TAG, "  - Is directory: ${file10.isDirectory}")
        Log.d(TAG, "  - Can read: ${file10.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file10.isHidden}")
        
        if (file10.exists() && file10.isDirectory && file10.canRead()) {
            val files = file10.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Dual whatsApp")
                Log.d(TAG, "✅ FOUND: /storage/ace-999/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file10, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 11: /storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/
        val file11 = File("/storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 11: ${file11.absolutePath}")
        Log.d(TAG, "  - Exists: ${file11.exists()}")
        Log.d(TAG, "  - Is directory: ${file11.isDirectory}")
        Log.d(TAG, "  - Can read: ${file11.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file11.isHidden}")
        
        if (file11.exists() && file11.isDirectory && file11.canRead()) {
            val files = file11.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("Dual whatsapp")
                Log.d(TAG, "✅ FOUND: /storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file11, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // Check 12: DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/
        val file12 = File("$basePath/DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        Log.d(TAG, "Checking path 12: ${file12.absolutePath}")
        Log.d(TAG, "  - Exists: ${file12.exists()}")
        Log.d(TAG, "  - Is directory: ${file12.isDirectory}")
        Log.d(TAG, "  - Can read: ${file12.canRead()}")
        Log.d(TAG, "  - Is hidden: ${file12.isHidden}")
        
        if (file12.exists() && file12.isDirectory && file12.canRead()) {
            val files = file12.list()
            Log.d(TAG, "  - Files count: ${files?.size ?: 0}")
            if (files != null && files.isNotEmpty()) {
            availableWhatsAppTypes.add("DuaL Whatsapp")
                Log.d(TAG, "✅ FOUND: DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
                logFirstFewFiles(file12, files)
            } else {
                Log.d(TAG, "❌ Folder exists but is empty")
            }
        } else {
            Log.d(TAG, "❌ Path not accessible")
        }
        
        // CRITICAL FALLBACK: Always add default option (from decompiled app)
        if (availableWhatsAppTypes.isEmpty()) {
            availableWhatsAppTypes.add("WhatsApp")
            Log.d(TAG, "⚠️ No paths found, using default fallback")
        }
        
        Log.d(TAG, "=== DETECTION SUMMARY ===")
        Log.d(TAG, "Found WhatsApp types: $availableWhatsAppTypes")
        Log.d(TAG, "Total valid paths: ${availableWhatsAppTypes.size}")
        
        return availableWhatsAppTypes
    }
    
    private fun logFirstFewFiles(folder: File, files: Array<String>) {
        Log.d(TAG, "  - First few files in ${folder.name}:")
        files.take(5).forEach { fileName ->
            val file = File(folder, fileName)
            Log.d(TAG, "    * $fileName (hidden: ${file.isHidden}, size: ${file.length()})")
        }
        if (files.size > 5) {
            Log.d(TAG, "    ... and ${files.size - 5} more files")
        }
    }
    
    /**
     * Get the path for a specific WhatsApp type
     * This is the exact implementation from the suggestion.txt file (Method F)
     */
    fun getPathForType(selectedType: String): String {
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        
        return when (selectedType) {
            "WhatsApp" -> "$basePath/WhatsApp/Media/.Statuses/"
            "Whatsapp" -> "$basePath/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
            "WA Business" -> "$basePath/WhatsApp Business/Media/.Statuses/"
            "wa Business" -> "$basePath/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/"
            "GB WhatsApp" -> "$basePath/GBWhatsApp/Media/.Statuses/"
            "Parellel Lite" -> "$basePath/parallel_lite/0/WhatsApp/Media/.Statuses/"
            "Parellel lite" -> "$basePath/parallel_intl/0/WhatsApp/Media/.Statuses/"
            "Dual Whatsapp" -> "$basePath/DualApp/WhatsApp/Media/.Statuses/"
            "Dual WhatsApp" -> "/storage/emulated/999/WhatsApp/Media/.Statuses/"
            "Dual whatsApp" -> "/storage/ace-999/WhatsApp/Media/.Statuses/"
            "Dual whatsapp" -> "/storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
            "DuaL Whatsapp" -> "$basePath/DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/"
            else -> "$basePath/WhatsApp/Media/.Statuses/" // Default fallback
        }
    }
    
    /**
     * Get all available WhatsApp types
     */
    fun getAvailableTypes(): List<String> {
        if (availableWhatsAppTypes.isEmpty()) {
            detectWhatsAppStatusFolders()
        }
        return availableWhatsAppTypes
    }
    
    /**
     * Get all possible status paths that exist and have files
     */
    fun getAllPossibleStatusPaths(): List<String> {
        detectWhatsAppStatusFolders()
        return availableWhatsAppTypes.map { getPathForType(it) }
    }
    
    /**
     * Get the best available status path (first one found)
     */
    fun getBestAvailablePath(): String? {
        detectWhatsAppStatusFolders()
        return if (availableWhatsAppTypes.isNotEmpty()) {
            getPathForType(availableWhatsAppTypes.first())
        } else {
            null
        }
    }
    
    /**
     * Debug function to log all WhatsApp paths
     */
    fun debugWhatsAppPaths() {
        Log.d(TAG, "=== DEBUGGING WHATSAPP PATHS ===")
        detectWhatsAppStatusFolders()
    }
}
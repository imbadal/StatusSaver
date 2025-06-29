package com.inningsstudio.statussaver.core.utils

import android.os.Environment
import android.util.Log
import java.io.File

class StatusPathDetector {
    
    private val availableWhatsAppTypes = mutableListOf<String>()
    
    /**
     * Detect all possible WhatsApp status folders based on the decompiled app logic
     * This is the exact implementation from the suggestion.txt file
     */
    fun detectWhatsAppStatusFolders(): List<String> {
        availableWhatsAppTypes.clear()
        
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        
        // Check 1: Android/media/com.whatsapp/WhatsApp/Media/.Statuses/
        val file1 = File("$basePath/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        if (file1.exists() && file1.list() != null && file1.list()!!.isNotEmpty()) {
            availableWhatsAppTypes.add("Whatsapp")
            Log.d("StatusPathDetector", "Found: Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        }
        
        // Check 2: WhatsApp/Media/.Statuses/
        val file2 = File("$basePath/WhatsApp/Media/.Statuses/")
        if (file2.exists() && file2.list() != null && file2.list()!!.isNotEmpty()) {
            availableWhatsAppTypes.add("WhatsApp")
            Log.d("StatusPathDetector", "Found: WhatsApp/Media/.Statuses/")
        }
        
        // Check 3: Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/
        val file3 = File("$basePath/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/")
        if (file3.exists()) {
            val list4 = file3.list()
            if (list4 != null && list4.isNotEmpty()) {
                availableWhatsAppTypes.add("wa Business")
                Log.d("StatusPathDetector", "Found: Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/")
            }
        }
        
        // Check 4: WhatsApp Business/Media/.Statuses/
        val file4 = File("$basePath/WhatsApp Business/Media/.Statuses/")
        if (file4.exists() && file4.list() != null && file4.list()!!.isNotEmpty()) {
            availableWhatsAppTypes.add("WA Business")
            Log.d("StatusPathDetector", "Found: WhatsApp Business/Media/.Statuses/")
        }
        
        // Check 5: parallel_lite/0/WhatsApp/Media/.Statuses/
        if (File("$basePath/parallel_lite/0/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("Parellel Lite")
            Log.d("StatusPathDetector", "Found: parallel_lite/0/WhatsApp/Media/.Statuses/")
        }
        
        // Check 6: parallel_intl/0/WhatsApp/Media/.Statuses/
        if (File("$basePath/parallel_intl/0/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("Parellel lite")
            Log.d("StatusPathDetector", "Found: parallel_intl/0/WhatsApp/Media/.Statuses/")
        }
        
        // Check 7: GBWhatsApp/Media/.Statuses/
        if (File("$basePath/GBWhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("GB WhatsApp")
            Log.d("StatusPathDetector", "Found: GBWhatsApp/Media/.Statuses/")
        }
        
        // Check 8: DualApp/WhatsApp/Media/.Statuses/
        if (File("$basePath/DualApp/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("Dual Whatsapp")
            Log.d("StatusPathDetector", "Found: DualApp/WhatsApp/Media/.Statuses/")
        }
        
        // Check 9: /storage/emulated/999/WhatsApp/Media/.Statuses/
        if (File("/storage/emulated/999/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("Dual WhatsApp")
            Log.d("StatusPathDetector", "Found: /storage/emulated/999/WhatsApp/Media/.Statuses/")
        }
        
        // Check 10: /storage/ace-999/WhatsApp/Media/.Statuses/
        if (File("/storage/ace-999/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("Dual whatsApp")
            Log.d("StatusPathDetector", "Found: /storage/ace-999/WhatsApp/Media/.Statuses/")
        }
        
        // Check 11: /storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/
        if (File("/storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("Dual whatsapp")
            Log.d("StatusPathDetector", "Found: /storage/ace-999/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        }
        
        // Check 12: DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/
        if (File("$basePath/DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/").exists()) {
            availableWhatsAppTypes.add("DuaL Whatsapp")
            Log.d("StatusPathDetector", "Found: DualApp/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/")
        }
        
        // CRITICAL FALLBACK: Always add default option (from decompiled app)
        if (availableWhatsAppTypes.isEmpty()) {
            availableWhatsAppTypes.add("WhatsApp")
            Log.d("StatusPathDetector", "No paths found, using default fallback")
        }
        
        Log.d("StatusPathDetector", "Found WhatsApp types: $availableWhatsAppTypes")
        return availableWhatsAppTypes
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
     * Debug method to log all possible paths
     */
    fun debugWhatsAppPaths() {
        val basePath = Environment.getExternalStorageDirectory().absolutePath
        Log.d("StatusPathDetector", "Base path: $basePath")
        
        val possiblePaths = listOf(
            "/WhatsApp/Media/.Statuses/",
            "/Android/media/com.whatsapp/WhatsApp/Media/.Statuses/",
            "/WhatsApp Business/Media/.Statuses/",
            "/Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses/",
            "/GBWhatsApp/Media/.Statuses/",
            "/parallel_lite/0/WhatsApp/Media/.Statuses/",
            "/DualApp/WhatsApp/Media/.Statuses/"
        )
        
        possiblePaths.forEach { path ->
            val folder = File(basePath + path)
            Log.d("StatusPathDetector", "Checking: ${folder.absolutePath}")
            Log.d("StatusPathDetector", "  - Exists: ${folder.exists()}")
            Log.d("StatusPathDetector", "  - Is Directory: ${folder.isDirectory()}")
            
            if (folder.exists() && folder.isDirectory()) {
                val files = folder.listFiles()
                Log.d("StatusPathDetector", "  - Files count: ${files?.size ?: "null"}")
                
                if (files != null && files.isNotEmpty()) {
                    Log.d("StatusPathDetector", "  - First few files:")
                    for (i in 0 until minOf(3, files.size)) {
                        Log.d("StatusPathDetector", "    * ${files[i].name}")
                    }
                }
            }
            
            val isValid = folder.exists() && folder.isDirectory() && 
                         folder.listFiles() != null && folder.listFiles()!!.isNotEmpty()
            Log.d("StatusPathDetector", "  - Valid for status reading: $isValid")
        }
    }
} 
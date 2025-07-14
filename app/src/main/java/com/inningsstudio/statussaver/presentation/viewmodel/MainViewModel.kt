package com.inningsstudio.statussaver.presentation.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.domain.usecase.DetectStatusPathsUseCase
import com.inningsstudio.statussaver.domain.usecase.GetStatusesUseCase
import com.inningsstudio.statussaver.presentation.state.StatusUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    private val getStatusesUseCase: GetStatusesUseCase,
    private val detectStatusPathsUseCase: DetectStatusPathsUseCase,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<StatusUiState>(StatusUiState.Loading)
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()
    
    private val statusPathDetector = StatusPathDetector()
    private val preferenceUtils = PreferenceUtils(context.applicationContext as android.app.Application)
    private var currentStatusPath: String? = null
    
    init {
        Log.d("MainViewModel", "=== INITIALIZING MAIN VIEW MODEL ===")
        Log.d("MainViewModel", "Android Version: ${Build.VERSION.SDK_INT}")
        Log.d("MainViewModel", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        
        // Check permissions first
        checkPermissionsAndLoadStatuses()
    }
    
    private fun checkPermissionsAndLoadStatuses() {
        viewModelScope.launch {
            Log.d("MainViewModel", "=== CHECKING PERMISSIONS AND LOADING STATUSES ===")
            
            // Step 1: Check if we have required permissions
            val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
            Log.d("MainViewModel", "Has required permissions: $hasPermissions")
            
            if (!hasPermissions) {
                Log.w("MainViewModel", "❌ PERMISSIONS NOT GRANTED - Cannot proceed")
                _uiState.value = StatusUiState.Error("Media permissions are required. Please grant permissions in app settings.")
                return@launch
            }
            
            Log.d("MainViewModel", "✅ PERMISSIONS GRANTED - Proceeding with status detection")
            
            // Step 2: Check for SAF URI from onboarding
            val safUri = preferenceUtils.getUriFromPreference()
            Log.d("MainViewModel", "SAF URI from preferences: $safUri")
            
            // Step 3: Detect all possible WhatsApp status paths
            val allPossiblePaths = statusPathDetector.getAllPossibleStatusPaths()
            Log.d("MainViewModel", "All possible WhatsApp status paths:")
            allPossiblePaths.forEachIndexed { index, path ->
                Log.d("MainViewModel", "  Path $index: $path")
            }
            
            // Step 4: Check each path for existence and accessibility
            Log.d("MainViewModel", "=== CHECKING PATH ACCESSIBILITY ===")
            var foundValidPath = false
            
            for (path in allPossiblePaths) {
                val folder = File(path)
                Log.d("MainViewModel", "Checking path: $path")
                Log.d("MainViewModel", "  - Exists: ${folder.exists()}")
                Log.d("MainViewModel", "  - Is directory: ${folder.isDirectory}")
                Log.d("MainViewModel", "  - Can read: ${folder.canRead()}")
                Log.d("MainViewModel", "  - Is hidden: ${folder.isHidden}")
                Log.d("MainViewModel", "  - Absolute path: ${folder.absolutePath}")
                
                if (folder.exists() && folder.isDirectory && folder.canRead()) {
                    Log.d("MainViewModel", "✅ VALID PATH FOUND: $path")
                    foundValidPath = true
                    currentStatusPath = path
                    
                    // Check for hidden files in the folder
                    checkHiddenFilesInFolder(folder)
                    break
                } else {
                    Log.d("MainViewModel", "❌ Path not accessible: $path")
                }
            }
            
            if (!foundValidPath) {
                Log.w("MainViewModel", "❌ NO VALID WHATSAPP STATUS PATHS FOUND")
                _uiState.value = StatusUiState.Error("No WhatsApp status folder found. Please make sure WhatsApp is installed and has status files.")
                return@launch
            }
            
            // Step 5: Load statuses from the valid path
            Log.d("MainViewModel", "=== LOADING STATUSES FROM VALID PATH ===")
            loadStatusesFromPath(currentStatusPath!!)
        }
    }
    
    private fun checkHiddenFilesInFolder(folder: File) {
        Log.d("MainViewModel", "=== CHECKING HIDDEN FILES IN FOLDER ===")
        Log.d("MainViewModel", "Folder: ${folder.absolutePath}")
        
        try {
            // List all files including hidden ones
            val allFiles = folder.listFiles { file ->
                // Accept all files for debugging
                true
            }
            
            Log.d("MainViewModel", "Total files found (including hidden): ${allFiles?.size ?: 0}")
            
            if (allFiles != null) {
                allFiles.forEach { file ->
                    Log.d("MainViewModel", "  File: ${file.name}")
                    Log.d("MainViewModel", "    - Is hidden: ${file.isHidden}")
                    Log.d("MainViewModel", "    - Size: ${file.length()} bytes")
                    Log.d("MainViewModel", "    - Can read: ${file.canRead()}")
                    Log.d("MainViewModel", "    - Is file: ${file.isFile}")
                    Log.d("MainViewModel", "    - Extension: ${getFileExtension(file.name)}")
                }
                
                // Count by type
                val images = allFiles.filter { isImageFile(it) }
                val videos = allFiles.filter { isVideoFile(it) }
                val hiddenFiles = allFiles.filter { it.isHidden }
                
                Log.d("MainViewModel", "File summary:")
                Log.d("MainViewModel", "  - Total files: ${allFiles.size}")
                Log.d("MainViewModel", "  - Image files: ${images.size}")
                Log.d("MainViewModel", "  - Video files: ${videos.size}")
                Log.d("MainViewModel", "  - Hidden files: ${hiddenFiles.size}")
            } else {
                Log.w("MainViewModel", "❌ Could not list files in folder")
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "❌ Error checking files in folder", e)
        }
    }
    
    private fun loadStatusesFromPath(path: String) {
        Log.d("MainViewModel", "=== LOADING STATUSES FROM PATH ===")
        Log.d("MainViewModel", "Path: $path")
        
        try {
            val statuses = readStatusesFromPath(path)
            Log.d("MainViewModel", "Statuses loaded: ${statuses.size}")
            
            if (statuses.isNotEmpty()) {
                Log.d("MainViewModel", "✅ SUCCESS: Loaded ${statuses.size} statuses")
                _uiState.value = StatusUiState.Success(statuses)
            } else {
                Log.w("MainViewModel", "❌ No statuses found in path")
                _uiState.value = StatusUiState.Error("No statuses found. Please make sure you have viewed some WhatsApp statuses.")
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "❌ Error loading statuses", e)
            _uiState.value = StatusUiState.Error("Error loading statuses: ${e.message}")
        }
    }
    
    private fun readStatusesFromPath(path: String): List<StatusModel> {
        val statuses = mutableListOf<StatusModel>()
        val statusFolder = File(path)
        
        Log.d("MainViewModel", "Reading statuses from path: $path")
        Log.d("MainViewModel", "Folder exists: ${statusFolder.exists()}")
        Log.d("MainViewModel", "Is directory: ${statusFolder.isDirectory}")
        Log.d("MainViewModel", "Can read: ${statusFolder.canRead()}")
        
        if (statusFolder.exists() && statusFolder.isDirectory) {
            // List all files including hidden ones
            val files = statusFolder.listFiles { file ->
                // Accept all files for debugging
                true
            }
            
            Log.d("MainViewModel", "Total files found (including hidden): ${files?.size ?: 0}")
            
            files?.forEach { file ->
                Log.d("MainViewModel", "Processing file: ${file.name}")
                Log.d("MainViewModel", "  - Is hidden: ${file.isHidden}")
                Log.d("MainViewModel", "  - Size: ${file.length()}")
                Log.d("MainViewModel", "  - Extension: ${getFileExtension(file.name)}")
                
                if (isValidStatusFile(file)) {
                    Log.d("MainViewModel", "✅ Valid status file: ${file.name}")
                    val statusModel = StatusModel(
                        id = file.absolutePath.hashCode().toLong(),
                        filePath = file.absolutePath,
                        fileName = file.name,
                        fileSize = file.length(),
                        lastModified = file.lastModified(),
                        isSelected = false,
                        isVideo = isVideoFile(file)
                    )
                    statuses.add(statusModel)
                } else {
                    Log.d("MainViewModel", "❌ Not a valid status file: ${file.name}")
                }
            }
        }
        
        return statuses
    }
    
    private fun isValidStatusFile(file: File): Boolean {
        if (!file.isFile || !file.canRead()) return false
        
        val name = file.name.lowercase()
        val extension = getFileExtension(name)
        
        // Check for valid media extensions
        val validExtensions = listOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "3gp", "mkv")
        
        return validExtensions.contains(extension) && file.length() > 0
    }
    
    private fun isImageFile(file: File): Boolean {
        val extension = getFileExtension(file.name.lowercase())
        return listOf("jpg", "jpeg", "png", "webp", "gif").contains(extension)
    }
    
    private fun isVideoFile(file: File): Boolean {
        val extension = getFileExtension(file.name.lowercase())
        return listOf("mp4", "3gp", "mkv", "avi", "mov").contains(extension)
    }
    
    private fun getFileExtension(fileName: String): String {
        return if (fileName.contains(".")) {
            fileName.substringAfterLast(".", "")
        } else {
            ""
        }
    }
    
    fun loadStatuses() {
        Log.d("MainViewModel", "=== MANUAL STATUS LOADING TRIGGERED ===")
        checkPermissionsAndLoadStatuses()
    }
    
    fun refreshStatuses() {
        Log.d("MainViewModel", "=== REFRESH STATUSES TRIGGERED ===")
        checkPermissionsAndLoadStatuses()
    }
    
    fun getAvailableWhatsAppTypes(): List<String> {
        return statusPathDetector.getAvailableTypes()
    }
    
    fun getCurrentStatusPath(): String? {
        return currentStatusPath
    }
    
    fun debugPaths() {
        Log.d("MainViewModel", "=== DEBUGGING PATHS ===")
        val allPaths = statusPathDetector.getAllPossibleStatusPaths()
        Log.d("MainViewModel", "All possible paths: $allPaths")
        
        allPaths.forEach { path ->
            val folder = File(path)
            Log.d("MainViewModel", "Path: $path")
            Log.d("MainViewModel", "  - Exists: ${folder.exists()}")
            Log.d("MainViewModel", "  - Is directory: ${folder.isDirectory}")
            Log.d("MainViewModel", "  - Can read: ${folder.canRead()}")
            Log.d("MainViewModel", "  - Is hidden: ${folder.isHidden}")
        }
    }
    
    fun debugStatusReading() {
        viewModelScope.launch {
            Log.d("MainViewModel", "=== DEBUGGING STATUS READING ===")
            
            // Check permissions
            val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
            Log.d("MainViewModel", "Has required permissions: $hasPermissions")
            
            // Check SAF URI
            val safUri = preferenceUtils.getUriFromPreference()
            Log.d("MainViewModel", "SAF URI from preferences: $safUri")
            
            // Check all possible paths
            val allPaths = statusPathDetector.getAllPossibleStatusPaths()
            Log.d("MainViewModel", "All possible paths: $allPaths")
            
            // Check each path individually
            allPaths.forEach { path ->
                val folder = File(path)
                Log.d("MainViewModel", "Path: $path")
                Log.d("MainViewModel", "  - Exists: ${folder.exists()}")
                Log.d("MainViewModel", "  - Is directory: ${folder.isDirectory}")
                Log.d("MainViewModel", "  - Can read: ${folder.canRead()}")
                Log.d("MainViewModel", "  - Is hidden: ${folder.isHidden}")
                
                if (folder.exists() && folder.isDirectory) {
                    val files = folder.listFiles { file -> true }
                    Log.d("MainViewModel", "  - Total files: ${files?.size ?: 0}")
                    files?.forEach { file ->
                        Log.d("MainViewModel", "    - ${file.name} (hidden: ${file.isHidden}, size: ${file.length()})")
                    }
                }
            }
            
            Log.d("MainViewModel", "=== END DEBUGGING ===")
        }
    }
}
package com.inningsstudio.statussaver.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.domain.entity.StatusEntity
import com.inningsstudio.statussaver.domain.repository.StatusRepository
import com.inningsstudio.statussaver.domain.usecase.DetectStatusPathsUseCase
import com.inningsstudio.statussaver.domain.usecase.GetStatusesUseCase
import com.inningsstudio.statussaver.presentation.state.StatusUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Main ViewModel for the status listing screen
 * Handles business logic and UI state management
 */
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
        loadStatuses()
    }
    
    fun loadStatuses() {
        viewModelScope.launch {
            try {
                _uiState.value = StatusUiState.Loading
                Log.d("MainViewModel", "=== STARTING STATUS LOADING ===")
                
                // First, try to use SAF URI from onboarding
                val safUri = preferenceUtils.getUriFromPreference()
                Log.d("MainViewModel", "SAF URI from preferences: $safUri")
                
                if (!safUri.isNullOrBlank()) {
                    Log.d("MainViewModel", "Using SAF URI: $safUri")
                    val statuses = FileUtils.getStatus(context, safUri)
                    Log.d("MainViewModel", "Statuses loaded via SAF: ${statuses.size}")
                    
                    if (statuses.isNotEmpty()) {
                        _uiState.value = StatusUiState.Success(statuses)
                        Log.d("MainViewModel", "Successfully loaded ${statuses.size} statuses via SAF")
                        return@launch
                    } else {
                        Log.w("MainViewModel", "No statuses found via SAF, trying fallback")
                    }
                } else {
                    Log.w("MainViewModel", "No SAF URI found in preferences")
                }
                
                // Fallback to direct file access if SAF fails or is not available
                Log.d("MainViewModel", "Using fallback: direct file access")
                val availablePaths = statusPathDetector.getAllPossibleStatusPaths()
                Log.d("MainViewModel", "Available paths: $availablePaths")
                
                if (availablePaths.isNotEmpty()) {
                    // Use the first available path
                    currentStatusPath = availablePaths.first()
                    Log.d("MainViewModel", "Using status path: $currentStatusPath")
                    
                    // Read statuses from this path
                    val statuses = readStatusesFromPath(currentStatusPath!!)
                    Log.d("MainViewModel", "Statuses loaded via direct access: ${statuses.size}")
                    
                    if (statuses.isNotEmpty()) {
                        _uiState.value = StatusUiState.Success(statuses)
                        Log.d("MainViewModel", "Successfully loaded ${statuses.size} statuses via direct access")
                    } else {
                        _uiState.value = StatusUiState.Empty("No statuses found in the detected folder")
                        Log.d("MainViewModel", "No statuses found in path: $currentStatusPath")
                    }
                } else {
                    _uiState.value = StatusUiState.Error("No WhatsApp status folders found")
                    Log.e("MainViewModel", "No WhatsApp status folders found")
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading statuses", e)
                _uiState.value = StatusUiState.Error("Failed to load statuses: ${e.message}")
            }
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
            
            if (files != null) {
                // Log all files for debugging
                files.forEach { file ->
                    Log.d("MainViewModel", "File: ${file.name}, isHidden: ${file.isHidden}, isFile: ${file.isFile}, size: ${file.length()}")
                }
                
                for (file in files) {
                    if (isValidStatusFile(file)) {
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
                        Log.d("MainViewModel", "✅ Added status: ${file.name}")
                    } else {
                        Log.d("MainViewModel", "❌ Skipping invalid file: ${file.name}")
                    }
                }
            } else {
                Log.w("MainViewModel", "Failed to list files in status directory")
            }
        } else {
            Log.w("MainViewModel", "Status folder does not exist or is not accessible")
        }
        
        Log.d("MainViewModel", "Total valid statuses found: ${statuses.size}")
        return statuses.sortedByDescending { it.lastModified }
    }
    
    private fun isValidStatusFile(file: File): Boolean {
        if (!file.isFile) {
            Log.d("MainViewModel", "Skipping non-file: ${file.name}")
            return false
        }
        
        val name = file.name.lowercase()
        
        // Skip .nomedia files
        if (name == ".nomedia") {
            Log.d("MainViewModel", "Skipping .nomedia file")
            return false
        }
        
        // Check for valid media file extensions
        val isValidExtension = name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".mp4") || 
               name.endsWith(".3gp") || name.endsWith(".mkv") ||
               name.endsWith(".webp") || name.endsWith(".gif") ||
               name.endsWith(".bmp") || name.endsWith(".heic") ||
               name.endsWith(".heif") || name.endsWith(".tiff") ||
               name.endsWith(".tif") || name.endsWith(".avi") ||
               name.endsWith(".mov") || name.endsWith(".wmv") ||
               name.endsWith(".flv") || name.endsWith(".m4v")
        
        if (!isValidExtension) {
            Log.d("MainViewModel", "Skipping file with invalid extension: ${file.name}")
            return false
        }
        
        // Check if file has content (size > 0)
        if (file.length() == 0L) {
            Log.d("MainViewModel", "Skipping empty file: ${file.name}")
            return false
        }
        
        Log.d("MainViewModel", "✅ Valid status file: ${file.name} (size: ${file.length()})")
        return true
    }
    
    private fun isVideoFile(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".mp4") || name.endsWith(".3gp") || 
               name.endsWith(".mkv") || name.endsWith(".avi") ||
               name.endsWith(".mov") || name.endsWith(".wmv") ||
               name.endsWith(".flv") || name.endsWith(".m4v")
    }
    
    fun refreshStatuses() {
        loadStatuses()
    }
    
    fun getAvailableWhatsAppTypes(): List<String> {
        return statusPathDetector.getAvailableTypes()
    }
    
    fun getCurrentStatusPath(): String? {
        return currentStatusPath
    }
    
    fun debugPaths() {
        statusPathDetector.debugWhatsAppPaths()
    }
    
    fun debugStatusReading() {
        viewModelScope.launch {
            Log.d("MainViewModel", "=== DEBUGGING STATUS READING ===")
            
            // Check permissions
            val hasPermissions = com.inningsstudio.statussaver.core.utils.StorageAccessHelper.hasRequiredPermissions(context)
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
                
                if (folder.exists() && folder.isDirectory) {
                    val files = folder.listFiles { true }
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
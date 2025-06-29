package com.inningsstudio.statussaver.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
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
    private var currentStatusPath: String? = null
    
    init {
        loadStatuses()
    }
    
    fun loadStatuses() {
        viewModelScope.launch {
            try {
                _uiState.value = StatusUiState.Loading
                
                // Use the new StatusPathDetector to find WhatsApp status paths
                val availablePaths = statusPathDetector.getAllPossibleStatusPaths()
                
                if (availablePaths.isNotEmpty()) {
                    // Use the first available path
                    currentStatusPath = availablePaths.first()
                    Log.d("MainViewModel", "Using status path: $currentStatusPath")
                    
                    // Read statuses from this path
                    val statuses = readStatusesFromPath(currentStatusPath!!)
                    
                    if (statuses.isNotEmpty()) {
                        _uiState.value = StatusUiState.Success(statuses)
                        Log.d("MainViewModel", "Loaded ${statuses.size} statuses")
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
        
        if (statusFolder.exists() && statusFolder.isDirectory) {
            val files = statusFolder.listFiles()
            if (files != null) {
                for (file in files) {
                    if (isValidStatusFile(file)) {
                        val statusModel = StatusModel(
                            id = file.absolutePath.hashCode().toLong(),
                            filePath = file.absolutePath,
                            fileName = file.name,
                            fileSize = file.length(),
                            lastModified = file.lastModified(),
                            isSelected = false
                        )
                        statuses.add(statusModel)
                    }
                }
            }
        }
        
        return statuses.sortedByDescending { it.lastModified }
    }
    
    private fun isValidStatusFile(file: File): Boolean {
        if (!file.isFile) return false
        
        val name = file.name.lowercase()
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || 
               name.endsWith(".png") || name.endsWith(".mp4") || 
               name.endsWith(".3gp") || name.endsWith(".mkv")
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
} 
package com.inningsstudio.statussaver.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.domain.usecase.DetectStatusPathsUseCase
import com.inningsstudio.statussaver.domain.usecase.GetStatusesUseCase
import com.inningsstudio.statussaver.presentation.state.StatusUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        loadStatuses()
    }
    
    fun loadStatuses() {
        viewModelScope.launch {
            try {
                _uiState.value = StatusUiState.Loading
                
                val statusPaths = detectStatusPathsUseCase()
                if (statusPaths.isEmpty()) {
                    _uiState.value = StatusUiState.NoWhatsAppInstalled
                    return@launch
                }
                
                val statuses = getStatusesUseCase(statusPaths.first())
                if (statuses.isEmpty()) {
                    _uiState.value = StatusUiState.Empty
                } else {
                    _uiState.value = StatusUiState.Success(statuses)
                }
            } catch (e: Exception) {
                _uiState.value = StatusUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun refreshStatuses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadStatuses()
            _isRefreshing.value = false
        }
    }
    
    fun debugStatusPaths() {
        viewModelScope.launch {
            try {
                // Check permissions first
                StatusPathDetector.checkPermissions(context)
                
                // Check if there are any WhatsApp statuses
                StatusPathDetector.checkWhatsAppStatuses(context)
                
                // Check specifically for received statuses from contacts
                StatusPathDetector.checkReceivedStatuses(context)
                
                // This will trigger the debug logging in StatusPathDetector
                val statusPaths = detectStatusPathsUseCase()
                println("Debug: Found ${statusPaths.size} status paths")
                statusPaths.forEach { path ->
                    println("Debug: Status path: $path")
                }
            } catch (e: Exception) {
                println("Debug: Error detecting status paths: ${e.message}")
            }
        }
    }
} 
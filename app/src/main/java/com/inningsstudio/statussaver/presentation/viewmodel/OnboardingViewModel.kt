package com.inningsstudio.statussaver.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.inningsstudio.statussaver.presentation.state.OnboardingUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the onboarding screen
 * Handles onboarding state and navigation logic
 */
class OnboardingViewModel() : ViewModel() {
    
    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Step1(false))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()
    
    fun setStepCompleted(step: Int, isCompleted: Boolean) {
        when (step) {
            0 -> _uiState.value = OnboardingUiState.Step1(isCompleted)
            1 -> _uiState.value = OnboardingUiState.Step2(isCompleted)
            2 -> _uiState.value = OnboardingUiState.Step3(isCompleted)
        }
    }
    
    fun setCurrentStep(step: Int) {
        _currentStep.value = step
    }
    
    fun canProceedToNextStep(): Boolean {
        return when (_currentStep.value) {
            0 -> (_uiState.value as? OnboardingUiState.Step1)?.isCompleted == true
            1 -> (_uiState.value as? OnboardingUiState.Step2)?.isCompleted == true
            2 -> (_uiState.value as? OnboardingUiState.Step3)?.isCompleted == true
            else -> false
        }
    }
} 
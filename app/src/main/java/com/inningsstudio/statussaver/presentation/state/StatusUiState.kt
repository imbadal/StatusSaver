package com.inningsstudio.statussaver.presentation.state

import com.inningsstudio.statussaver.data.model.StatusModel

/**
 * UI state for status listing screen
 */
sealed class StatusUiState {
    object Loading : StatusUiState()
    data class Success(val statuses: List<StatusModel>) : StatusUiState()
    data class Empty(val message: String) : StatusUiState()
    data class Error(val message: String) : StatusUiState()
}

/**
 * UI state for onboarding screen
 */
sealed class OnboardingUiState {
    object Loading : OnboardingUiState()
    data class Step1(val isCompleted: Boolean) : OnboardingUiState()
    data class Step2(val isCompleted: Boolean) : OnboardingUiState()
    data class Step3(val isCompleted: Boolean) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
} 
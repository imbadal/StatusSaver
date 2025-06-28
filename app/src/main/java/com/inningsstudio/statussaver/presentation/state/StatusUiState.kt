package com.inningsstudio.statussaver.presentation.state

import com.inningsstudio.statussaver.domain.entity.StatusEntity

/**
 * UI state for status listing screen
 */
sealed class StatusUiState {
    object Loading : StatusUiState()
    data class Success(val statuses: List<StatusEntity>) : StatusUiState()
    data class Error(val message: String) : StatusUiState()
    object Empty : StatusUiState()
    object NoWhatsAppInstalled : StatusUiState()
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
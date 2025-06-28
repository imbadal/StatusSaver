package com.inningsstudio.statussaver.domain.usecase

import com.inningsstudio.statussaver.domain.repository.StatusRepository

/**
 * Use case for detecting WhatsApp status paths
 * Implements business logic for path detection
 */
class DetectStatusPathsUseCase(
    private val statusRepository: StatusRepository
) {
    suspend operator fun invoke(): List<String> {
        return statusRepository.detectStatusPaths()
    }
} 
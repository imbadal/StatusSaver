package com.inningsstudio.statussaver.domain.usecase

import com.inningsstudio.statussaver.domain.entity.StatusEntity
import com.inningsstudio.statussaver.domain.repository.StatusRepository

/**
 * Use case for getting saved statuses
 * Implements business logic for retrieving saved statuses
 */
class GetSavedStatusesUseCase(
    private val statusRepository: StatusRepository
) {
    suspend operator fun invoke(): List<StatusEntity> {
        return statusRepository.getSavedStatuses()
    }
} 
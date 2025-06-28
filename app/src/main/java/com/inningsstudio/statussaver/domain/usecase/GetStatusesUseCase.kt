package com.inningsstudio.statussaver.domain.usecase

import com.inningsstudio.statussaver.domain.entity.StatusEntity
import com.inningsstudio.statussaver.domain.repository.StatusRepository

/**
 * Use case for getting WhatsApp statuses
 * Implements business logic for retrieving statuses
 */
class GetStatusesUseCase(
    private val statusRepository: StatusRepository
) {
    suspend operator fun invoke(statusUri: String): List<StatusEntity> {
        return statusRepository.getStatuses(statusUri)
    }
} 
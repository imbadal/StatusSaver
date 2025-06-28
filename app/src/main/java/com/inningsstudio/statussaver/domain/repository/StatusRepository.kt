package com.inningsstudio.statussaver.domain.repository

import com.inningsstudio.statussaver.domain.entity.StatusEntity

/**
 * Domain repository interface for status operations
 * Defines the contract for status-related data operations
 */
interface StatusRepository {
    suspend fun getStatuses(statusUri: String): List<StatusEntity>
    suspend fun getSavedStatuses(): List<StatusEntity>
    suspend fun saveStatus(status: StatusEntity): Boolean
    suspend fun deleteStatus(path: String): Boolean
    suspend fun shareStatus(path: String): Boolean
    suspend fun detectStatusPaths(): List<String>
    suspend fun isWhatsAppInstalled(): Boolean
} 
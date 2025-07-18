package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StatusSaver
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

data class StatusGalleryState(
    val statusList: List<StatusModel> = emptyList(),
    val savedStatusList: List<StatusModel> = emptyList(),
    val favoriteList: List<StatusModel> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingSaved: Boolean = false,
    val errorMessage: String? = null,
    val showStatusView: Boolean = false,
    val selectedStatusIndex: Int = 0,
    val currentTab: Int = 0, // 0 = Statuses, 1 = Saved
    val lastSavedStatusesHash: Int = 0,
    val lastStatusesHash: Int = 0,
    val savedStatusesLoaded: Boolean = false,
    val statusesLoaded: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val statusToDelete: StatusModel? = null,
    val statusFilterTab: Int = 0, // 0 = All, 1 = Image, 2 = Video
    val displayStatusList: List<StatusModel> = emptyList(),
    val savedFilterTab: Int = 0, // 0 = Saved, 1 = Favourites
    val showSettingsBottomSheet: Boolean = false,
    val gridColumns: Int = 3,
    val sortOrder: Int = 0 // 0 = Latest first, 1 = Oldest first
)

// Helper functions for filtering and sorting
fun filterStatuses(statuses: List<StatusModel>, filterTab: Int): List<StatusModel> {
    return when (filterTab) {
        0 -> statuses // All
        1 -> statuses.filter { !it.isVideo } // Image only
        2 -> statuses.filter { it.isVideo } // Video only
        else -> statuses
    }
}

fun sortStatuses(statuses: List<StatusModel>, sortOrder: Int): List<StatusModel> {
    return when (sortOrder) {
        0 -> statuses.sortedByDescending { it.lastModified } // Latest first
        1 -> statuses.sortedBy { it.lastModified } // Oldest first
        else -> statuses.sortedByDescending { it.lastModified } // Default: Latest first
    }
}

// Helper functions for calculating hashes
fun calculateSavedStatusesHash(statuses: List<StatusModel>): Int {
    return statuses.hashCode()
}

fun calculateStatusesHash(statuses: List<StatusModel>): Int {
    return statuses.hashCode()
}

// Helper function for video thumbnail generation
suspend fun getVideoThumbnailIO(context: Context, path: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val retriever = MediaMetadataRetriever()
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(path))
            } else {
                retriever.setDataSource(path)
            }
            val bitmap = retriever.frameAtTime
            retriever.release()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

// Data loading functions
suspend fun loadSavedStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    Log.d("StatusGalleryActivity", "=== STARTING SAVED STATUS LOADING ===")
    onStateUpdate(currentState.copy(isLoadingSaved = true))

    try {
        // Get saved statuses from DCIM folder (excluding favorites)
        val savedStatuses = FileUtils.getSavedStatusesFromFolder(context)
        // Get favorite statuses from favourites folder
        val favorites = FileUtils.getFavoriteStatusesFromFolder(context)
        Log.d("StatusGalleryActivity", "Found ${savedStatuses.size} saved statuses from folder")
        Log.d("StatusGalleryActivity", "Found ${favorites.size} favorite statuses from folder")
        
        // Calculate hash of new statuses
        val newHash = calculateSavedStatusesHash(savedStatuses + favorites)
        
        // Only update state if there are actual changes
        if (newHash != currentState.lastSavedStatusesHash) {
            Log.d("StatusGalleryActivity", "Saved statuses changed, updating UI")
            onStateUpdate(currentState.copy(
                savedStatusList = savedStatuses,
                favoriteList = favorites,
                lastSavedStatusesHash = newHash,
                isLoadingSaved = false
            ))
        } else {
            Log.d("StatusGalleryActivity", "No changes in saved statuses, skipping UI update")
            onStateUpdate(currentState.copy(isLoadingSaved = false))
        }
        
        Log.d("StatusGalleryActivity", "✅ Saved status loading completed successfully")
    } catch (e: Exception) {
        Log.e("StatusGalleryActivity", "❌ Error loading saved statuses", e)
        onStateUpdate(currentState.copy(isLoadingSaved = false))
    }
}

suspend fun loadStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    Log.d("StatusGalleryActivity", "=== STARTING STATUS LOADING ===")
    onStateUpdate(currentState.copy(isLoading = true, errorMessage = null))

    val pref = PreferenceUtils(context.applicationContext as android.app.Application)
    val safUri = pref.getUriFromPreference()

    Log.d("StatusGalleryActivity", "Loading statuses with SAF URI: $safUri")

    try {
        // Only check if SAF URI is present
        if (safUri.isNullOrBlank()) {
            Log.e("StatusGalleryActivity", "❌ NO SAF URI - Cannot load statuses")
            onStateUpdate(currentState.copy(
                errorMessage = "Please grant access to the WhatsApp .Statuses folder in onboarding/settings.",
                isLoading = false
            ))
            return
        }

        Log.d("StatusGalleryActivity", "Calling FileUtils.getStatus()...")
        val statuses = FileUtils.getStatus(context, safUri ?: "")
        Log.d(
            "StatusGalleryActivity",
            "FileUtils.getStatus() returned ${statuses.size} statuses"
        )

        val filteredStatuses = statuses.filter { it.filePath.isNotEmpty() }
            .sortedByDescending { it.lastModified } // Sort by date, latest first
        Log.d("StatusGalleryActivity", "Filtered to ${filteredStatuses.size} valid statuses")
        
        // Calculate hash of new statuses
        val newHash = calculateStatusesHash(filteredStatuses)
        
        // Only update state if there are actual changes
        if (newHash != currentState.lastStatusesHash) {
            Log.d("StatusGalleryActivity", "Statuses changed, updating UI")
            onStateUpdate(currentState.copy(
                statusList = filteredStatuses,
                lastStatusesHash = newHash,
                statusesLoaded = true,
                isLoading = false
            ))
        } else {
            Log.d("StatusGalleryActivity", "No changes in statuses, skipping UI update")
            onStateUpdate(currentState.copy(
                statusesLoaded = true,
                isLoading = false
            ))
        }
        Log.d("StatusGalleryActivity", "✅ Status loading completed successfully")

    } catch (e: Exception) {
        Log.e("StatusGalleryActivity", "❌ Error loading statuses", e)
        onStateUpdate(currentState.copy(
            errorMessage = e.message,
            isLoading = false
        ))
    }
}

// Force refresh functions
suspend fun forceRefreshStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    val newState = currentState.copy(statusesLoaded = false)
    loadStatuses(context, newState, onStateUpdate)
}

suspend fun forceRefreshSavedStatuses(
    context: Context,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    val newState = currentState.copy(savedStatusesLoaded = false)
    loadSavedStatuses(context, newState, onStateUpdate)
}

// Favorite management functions
suspend fun markAsFavorite(
    context: Context,
    status: StatusModel,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    try {
        Log.d("StatusGalleryActivity", "Marking as favorite: ${status.filePath}")
        
        // Optimistic UI update - immediately move the item from saved to favorites
        val updatedStatus = status.copy(
            filePath = status.filePath.replace(StatusSaver.SAVED_DIRECTORY, StatusSaver.FAVOURITES_DIRECTORY)
        )
        
        // Update the lists optimistically
        val updatedSavedList = currentState.savedStatusList.filter { it.filePath != status.filePath }
        val updatedFavoriteList = currentState.favoriteList + updatedStatus
        
        onStateUpdate(currentState.copy(
            savedStatusList = updatedSavedList,
            favoriteList = updatedFavoriteList
        ))
        
        // Perform the actual file operation
        val success = FileUtils.markAsFavorite(context, status.filePath)
        if (success) {
            Log.d("StatusGalleryActivity", "✅ Status marked as favorite successfully")
            // No need to refresh - optimistic update already handled it
        } else {
            Log.e("StatusGalleryActivity", "❌ Failed to mark as favorite")
            // Revert optimistic update on failure
            onStateUpdate(currentState.copy(
                savedStatusList = currentState.savedStatusList,
                favoriteList = currentState.favoriteList
            ))
        }
    } catch (e: Exception) {
        Log.e("StatusGalleryActivity", "❌ Error marking as favorite", e)
        // Revert optimistic update on error
        onStateUpdate(currentState.copy(
            savedStatusList = currentState.savedStatusList,
            favoriteList = currentState.favoriteList
        ))
    }
}

suspend fun unmarkAsFavorite(
    context: Context,
    status: StatusModel,
    currentState: StatusGalleryState,
    onStateUpdate: (StatusGalleryState) -> Unit
) {
    try {
        Log.d("StatusGalleryActivity", "Unmarking as favorite: ${status.filePath}")
        
        // Optimistic UI update - immediately move the item from favorites to saved
        val updatedStatus = status.copy(
            filePath = status.filePath.replace(StatusSaver.FAVOURITES_DIRECTORY, StatusSaver.SAVED_DIRECTORY)
        )
        
        // Update the lists optimistically
        val updatedSavedList = currentState.savedStatusList + updatedStatus
        val updatedFavoriteList = currentState.favoriteList.filter { it.filePath != status.filePath }
        
        onStateUpdate(currentState.copy(
            savedStatusList = updatedSavedList,
            favoriteList = updatedFavoriteList
        ))
        
        // Perform the actual file operation
        val success = FileUtils.unmarkAsFavorite(context, status.filePath)
        if (success) {
            Log.d("StatusGalleryActivity", "✅ Status unmarked as favorite successfully")
            // No need to refresh - optimistic update already handled it
        } else {
            Log.e("StatusGalleryActivity", "❌ Failed to unmark as favorite")
            // Revert optimistic update on failure
            onStateUpdate(currentState.copy(
                savedStatusList = currentState.savedStatusList,
                favoriteList = currentState.favoriteList
            ))
        }
    } catch (e: Exception) {
        Log.e("StatusGalleryActivity", "❌ Error unmarking as favorite", e)
        // Revert optimistic update on error
        onStateUpdate(currentState.copy(
            savedStatusList = currentState.savedStatusList,
            favoriteList = currentState.favoriteList
        ))
    }
} 
package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StatusSaver
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StandaloneStatusGallery(context: Context) {
    // State management
    var state by remember { mutableStateOf(StatusGalleryState()) }
    val coroutineScope = rememberCoroutineScope()
    
    // Pager state for swipeable tabs
    val pagerState = rememberPagerState(initialPage = state.currentTab)

    // Sync pager state with currentTab
    LaunchedEffect(pagerState.currentPage) {
        state = state.copy(currentTab = pagerState.currentPage)
    }
    // Sync currentTab with pager state
    LaunchedEffect(state.currentTab) {
        if (pagerState.currentPage != state.currentTab) {
            pagerState.animateScrollToPage(state.currentTab)
        }
    }

    // Update display list when source list, filter, or sort order changes
    LaunchedEffect(state.statusList, state.statusFilterTab, state.sortOrder) {
        val filteredStatuses = filterStatuses(state.statusList, state.statusFilterTab)
        val sortedStatuses = sortStatuses(filteredStatuses, state.sortOrder)
        state = state.copy(displayStatusList = sortedStatuses)
    }

    // UI setup
    val primaryGreen = Color(0xFF25D366)
    val systemUiController = rememberSystemUiController()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set status bar color for gallery
    SideEffect {
        systemUiController.setStatusBarColor(primaryGreen, darkIcons = false)
    }

    // Initial load
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            loadStatuses(context, state) { newState -> state = newState }
        }
        coroutineScope.launch {
            loadSavedStatuses(context, state) { newState -> state = newState }
        }
    }
    
    // Load statuses when switching to Statuses tab
    LaunchedEffect(state.currentTab) {
        if (state.currentTab == 0 && !state.statusesLoaded) {
            coroutineScope.launch {
                loadStatuses(context, state) { newState -> state = newState }
            }
            state = state.copy(statusesLoaded = true)
        } else if (state.currentTab == 1 && !state.savedStatusesLoaded) {
            coroutineScope.launch {
                loadSavedStatuses(context, state) { newState -> state = newState }
            }
            state = state.copy(savedStatusesLoaded = true)
        }
    }

    // Setup lifecycle observer
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                super.onStart(owner)
                // App came to foreground, check for new statuses
                Log.d("StatusGalleryActivity", "App came to foreground, checking for new statuses")
                
                // Check for new statuses based on current tab
                if (state.currentTab == 0) {
                    // Statuses tab - check for new WhatsApp statuses
                    coroutineScope.launch {
                        val pref = PreferenceUtils(context.applicationContext as android.app.Application)
                        val safUri = pref.getUriFromPreference()
                        
                        if (!safUri.isNullOrBlank()) {
                            try {
                                val newStatuses = FileUtils.getStatus(context, safUri)
                                    .filter { it.filePath.isNotEmpty() }
                                    .sortedByDescending { it.lastModified }
                                
                                // Calculate hash of new statuses
                                val newHash = calculateStatusesHash(newStatuses)
                                
                                // Only update if there are actual changes
                                if (newHash != state.lastStatusesHash) {
                                    Log.d("StatusGalleryActivity", "New statuses detected, updating UI")
                                    state = state.copy(
                                        statusList = newStatuses,
                                        lastStatusesHash = newHash
                                    )
                                } else {
                                    Log.d("StatusGalleryActivity", "No new statuses found")
                                }
                            } catch (e: Exception) {
                                Log.e("StatusGalleryActivity", "Error checking for new statuses", e)
                            }
                        }
                    }
                } else {
                    // Saved tab - check for new saved statuses
                    coroutineScope.launch {
                        try {
                            val newSavedStatuses = FileUtils.getSavedStatusesFromFolder(context)
                            val newFavorites = FileUtils.getFavoriteStatusesFromFolder(context)
                            
                            // Calculate hash of new saved statuses
                            val newHash = calculateSavedStatusesHash(newSavedStatuses + newFavorites)
                            
                            // Only update if there are actual changes
                            if (newHash != state.lastSavedStatusesHash) {
                                Log.d("StatusGalleryActivity", "New saved statuses detected, updating UI")
                                state = state.copy(
                                    savedStatusList = newSavedStatuses,
                                    favoriteList = newFavorites,
                                    lastSavedStatusesHash = newHash
                                )
                            } else {
                                Log.d("StatusGalleryActivity", "No new saved statuses found")
                            }
                        } catch (e: Exception) {
                            Log.e("StatusGalleryActivity", "Error checking for new saved statuses", e)
                        }
                    }
                }
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodic background check for new statuses while app is in foreground
    LaunchedEffect(state.currentTab) {
        while (true) {
            delay(15000) // Check every 15 seconds to be less aggressive
            
            // Only check if app is in foreground (current tab is active)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // Check for new statuses based on current tab
                if (state.currentTab == 0) {
                    // Statuses tab - check for new WhatsApp statuses
                    val pref = PreferenceUtils(context.applicationContext as android.app.Application)
                    val safUri = pref.getUriFromPreference()
                    
                    if (!safUri.isNullOrBlank()) {
                        try {
                            val newStatuses = FileUtils.getStatus(context, safUri)
                                .filter { it.filePath.isNotEmpty() }
                                .sortedByDescending { it.lastModified }
                            
                            // Calculate hash of new statuses
                            val newHash = calculateStatusesHash(newStatuses)
                            
                            // Only update if there are actual changes
                            if (newHash != state.lastStatusesHash) {
                                Log.d("StatusGalleryActivity", "New statuses detected during background check, updating UI")
                                state = state.copy(
                                    statusList = newStatuses,
                                    lastStatusesHash = newHash
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("StatusGalleryActivity", "Error during background status check", e)
                        }
                    }
                } else {
                    // Saved tab - check for new saved statuses
                    try {
                        val newSavedStatuses = FileUtils.getSavedStatusesFromFolder(context)
                        val newFavorites = FileUtils.getFavoriteStatusesFromFolder(context)
                        
                        // Calculate hash of new saved statuses
                        val newHash = calculateSavedStatusesHash(newSavedStatuses + newFavorites)
                        
                        // Only update if there are actual changes
                        if (newHash != state.lastSavedStatusesHash) {
                            Log.d("StatusGalleryActivity", "New saved statuses detected during background check, updating UI")
                            state = state.copy(
                                savedStatusList = newSavedStatuses,
                                favoriteList = newFavorites,
                                lastSavedStatusesHash = newHash
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("StatusGalleryActivity", "Error during background saved status check", e)
                    }
                }
            }
        }
    }

    if (state.showStatusView) {
        StatusView(
            statusList = if (state.currentTab == 0) state.displayStatusList else {
                // For saved tab, use the filtered list based on current filter
                when (state.savedFilterTab) {
                    0 -> state.savedStatusList.sortedByDescending { it.lastModified }
                    1 -> state.favoriteList.sortedByDescending { it.lastModified }
                    else -> state.savedStatusList.sortedByDescending { it.lastModified }
                }
            },
            initialIndex = state.selectedStatusIndex,
            isFromSavedStatuses = state.currentTab == 1,
            onBackPressed = { state = state.copy(showStatusView = false) },
            onStatusSaved = {
                // Trigger refresh of saved statuses when a status is saved
                if (state.currentTab == 1) {
                    coroutineScope.launch {
                        loadSavedStatuses(context, state) { newState -> state = newState }
                    }
                } else {
                    // Reset the loaded flag so next time Saved tab is opened, it will refresh
                    state = state.copy(savedStatusesLoaded = false)
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                StatusGalleryToolbar(
                    isLoading = state.isLoading,
                    currentTab = state.currentTab,
                    onTabChange = { newTab -> state = state.copy(currentTab = newTab) },
                    onRefresh = {
                        coroutineScope.launch {
                            if (state.currentTab == 0) {
                                forceRefreshStatuses(context, state) { newState -> state = newState }
                            } else {
                                forceRefreshSavedStatuses(context, state) { newState -> state = newState }
                            }
                        }
                    }
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    pageCount = 2,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> { // Statuses tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Filter tabs for Statuses
                                if (!state.isLoading && state.errorMessage == null && state.statusList.isNotEmpty()) {
                                    StatusFilterTabs(
                                        currentFilter = state.statusFilterTab,
                                        onFilterChanged = { newFilter -> 
                                            state = state.copy(statusFilterTab = newFilter)
                                        },
                                        showSettingsButton = true,
                                        onSettingsClick = { 
                                            state = state.copy(showSettingsBottomSheet = true)
                                        }
                                    )
                                }
                                
                                // Content area
                                StatusGridContent(
                                    isLoading = state.isLoading,
                                    errorMessage = state.errorMessage,
                                    statusList = state.statusList,
                                    displayStatusList = state.displayStatusList,
                                    statusFilterTab = state.statusFilterTab,
                                    gridColumns = state.gridColumns,
                                    thumbCache = remember { mutableMapOf<String, Bitmap?>() },
                                    context = context,
                                    getVideoThumbnailIO = { ctx, path -> getVideoThumbnailIO(ctx, path) },
                                    onStatusClick = { index ->
                                        state = state.copy(
                                            selectedStatusIndex = index,
                                            showStatusView = true
                                        )
                                    },
                                    onRefresh = {
                                        coroutineScope.launch {
                                            forceRefreshStatuses(context, state) { newState -> state = newState }
                                        }
                                    },
                                    onDelete = { status ->
                                        state = state.copy(
                                            statusToDelete = status,
                                            showDeleteConfirmation = true
                                        )
                                    },
                                    onFavoriteToggle = { status ->
                                        coroutineScope.launch {
                                            if (StatusSaver.isFileInFavorites(status.filePath)) {
                                                unmarkAsFavorite(context, status, state) { newState -> state = newState }
                                            } else {
                                                markAsFavorite(context, status, state) { newState -> state = newState }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        1 -> { // Saved tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Filter tabs for Saved Statuses
                                if (!state.isLoadingSaved && (state.savedStatusList.isNotEmpty() || state.favoriteList.isNotEmpty())) {
                                    SavedStatusFilterTabs(
                                        currentFilter = state.savedFilterTab,
                                        onFilterChanged = { newFilter -> 
                                            state = state.copy(savedFilterTab = newFilter)
                                        }
                                    )
                                }
                                
                                // Content area
                                val displaySavedList = when (state.savedFilterTab) {
                                    0 -> state.savedStatusList.sortedByDescending { it.lastModified }
                                    1 -> state.favoriteList.sortedByDescending { it.lastModified }
                                    else -> state.savedStatusList.sortedByDescending { it.lastModified }
                                }
                                
                                StatusGridContent(
                                    isLoading = state.isLoadingSaved,
                                    errorMessage = null,
                                    statusList = state.savedStatusList,
                                    displayStatusList = displaySavedList,
                                    statusFilterTab = 0,
                                    gridColumns = state.gridColumns,
                                    thumbCache = remember { mutableMapOf<String, Bitmap?>() },
                                    context = context,
                                    getVideoThumbnailIO = { ctx, path -> getVideoThumbnailIO(ctx, path) },
                                    onStatusClick = { index ->
                                        state = state.copy(
                                            selectedStatusIndex = index,
                                            showStatusView = true
                                        )
                                    },
                                    onRefresh = {
                                        coroutineScope.launch {
                                            forceRefreshSavedStatuses(context, state) { newState -> state = newState }
                                        }
                                    },
                                    onDelete = { status ->
                                        state = state.copy(
                                            statusToDelete = status,
                                            showDeleteConfirmation = true
                                        )
                                    },
                                    onFavoriteToggle = { status ->
                                        coroutineScope.launch {
                                            if (StatusSaver.isFileInFavorites(status.filePath)) {
                                                unmarkAsFavorite(context, status, state) { newState -> state = newState }
                                            } else {
                                                markAsFavorite(context, status, state) { newState -> state = newState }
                                            }
                                        }
                                    },
                                    isSavedTab = true,
                                    savedFilterTab = state.savedFilterTab
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    StatusDeleteDialog(
        showDeleteConfirmation = state.showDeleteConfirmation,
        statusToDelete = state.statusToDelete,
        onDismiss = { 
            state = state.copy(
                showDeleteConfirmation = false,
                statusToDelete = null
            )
        },
        onDelete = { status ->
            coroutineScope.launch {
                try {
                    // Optimistic UI update - immediately remove the item from both lists
                    val isFavorite = StatusSaver.isFileInFavorites(status.filePath)
                    if (isFavorite) {
                        state = state.copy(
                            favoriteList = state.favoriteList.filter { it.filePath != status.filePath }
                        )
                    } else {
                        state = state.copy(
                            savedStatusList = state.savedStatusList.filter { it.filePath != status.filePath }
                        )
                    }
                    
                    // Perform the actual file operation
                    val success = FileUtils.deleteSavedStatus(context, status.filePath)
                    if (!success) {
                        // Revert optimistic update on failure
                        if (isFavorite) {
                            state = state.copy(
                                favoriteList = state.favoriteList + status
                            )
                        } else {
                            state = state.copy(
                                savedStatusList = state.savedStatusList + status
                            )
                        }
                        Log.e("StatusGalleryActivity", "Failed to delete saved status")
                    }
                } catch (e: Exception) {
                    // Revert optimistic update on error
                    val isFavorite = StatusSaver.isFileInFavorites(status.filePath)
                    if (isFavorite) {
                        state = state.copy(
                            favoriteList = state.favoriteList + status
                        )
                    } else {
                        state = state.copy(
                            savedStatusList = state.savedStatusList + status
                        )
                    }
                    Log.e("StatusGalleryActivity", "Error deleting saved status", e)
                }
            }
        }
    )
    
    // Settings Bottom Sheet
    StatusSettingsBottomSheet(
        showSettingsBottomSheet = state.showSettingsBottomSheet,
        onDismiss = { state = state.copy(showSettingsBottomSheet = false) },
        gridColumns = state.gridColumns,
        onGridColumnsChange = { newColumns -> state = state.copy(gridColumns = newColumns) },
        sortOrder = state.sortOrder,
        onSortOrderChange = { newOrder -> state = state.copy(sortOrder = newOrder) }
    )
} 
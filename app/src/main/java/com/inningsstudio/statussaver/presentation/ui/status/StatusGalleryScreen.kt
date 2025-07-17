package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.facebook.shimmer.ShimmerFrameLayout
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.core.utils.StatusSaver
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import android.app.Activity
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.inningsstudio.statussaver.R

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StandaloneStatusGallery(context: Context) {
    var statusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var savedStatusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var favoriteList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingSaved by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStatusView by remember { mutableStateOf(false) }
    var selectedStatusIndex by remember { mutableStateOf(0) }
    var currentTab by remember { mutableStateOf(0) } // 0 = Statuses, 1 = Saved
    var lastSavedStatusesHash by remember { mutableStateOf(0) }
    var lastStatusesHash by remember { mutableStateOf(0) }
    var savedStatusesLoaded by remember { mutableStateOf(false) }
    var statusesLoaded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var statusToDelete by remember { mutableStateOf<StatusModel?>(null) }
    
    // Filter tabs for Statuses
    var statusFilterTab by remember { mutableStateOf(0) } // 0 = All, 1 = Image, 2 = Video
    var displayStatusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    
    // Filter tabs for Saved Statuses
    var savedFilterTab by remember { mutableStateOf(0) } // 0 = Saved, 1 = Favourites
    
    // Settings state
    var showSettingsBottomSheet by remember { mutableStateOf(false) }
    var gridColumns by remember { mutableStateOf(3) } // Default: 3 columns
    var sortOrder by remember { mutableStateOf(0) } // 0 = Latest first, 1 = Oldest first
    
    val coroutineScope = rememberCoroutineScope()

    // Pager state for swipeable tabs
    val pagerState = rememberPagerState(initialPage = currentTab)

    // Sync pager state with currentTab
    LaunchedEffect(pagerState.currentPage) {
        currentTab = pagerState.currentPage
    }
    // Sync currentTab with pager state
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab) {
            pagerState.animateScrollToPage(currentTab)
        }
    }

    // Simple in-memory thumbnail cache
    val thumbCache = remember { mutableMapOf<String, Bitmap?>() }

    // Function to calculate hash of saved statuses for change detection
    fun calculateSavedStatusesHash(statuses: List<StatusModel>): Int {
        return statuses.hashCode()
    }

    // Function to calculate hash of statuses for change detection
    fun calculateStatusesHash(statuses: List<StatusModel>): Int {
        return statuses.hashCode()
    }

    // Function to filter statuses based on selected tab
    fun filterStatuses(statuses: List<StatusModel>, filterTab: Int): List<StatusModel> {
        return when (filterTab) {
            0 -> statuses // All
            1 -> statuses.filter { !it.isVideo } // Image only
            2 -> statuses.filter { it.isVideo } // Video only
            else -> statuses
        }
    }

    // Function to sort statuses based on sort order
    fun sortStatuses(statuses: List<StatusModel>, sortOrder: Int): List<StatusModel> {
        return when (sortOrder) {
            0 -> statuses.sortedByDescending { it.lastModified } // Latest first
            1 -> statuses.sortedBy { it.lastModified } // Oldest first
            else -> statuses.sortedByDescending { it.lastModified } // Default: Latest first
        }
    }

    // Update display list when source list, filter, or sort order changes
    LaunchedEffect(statusList, statusFilterTab, sortOrder) {
        val filteredStatuses = filterStatuses(statusList, statusFilterTab)
        displayStatusList = sortStatuses(filteredStatuses, sortOrder)
    }

    fun loadSavedStatuses() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== STARTING SAVED STATUS LOADING ===")
            isLoadingSaved = true

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
                if (newHash != lastSavedStatusesHash) {
                    Log.d("StatusGalleryActivity", "Saved statuses changed, updating UI")
                    savedStatusList = savedStatuses
                    favoriteList = favorites
                    lastSavedStatusesHash = newHash
                } else {
                    Log.d("StatusGalleryActivity", "No changes in saved statuses, skipping UI update")
                }
                
                isLoadingSaved = false
                Log.d("StatusGalleryActivity", "✅ Saved status loading completed successfully")
            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error loading saved statuses", e)
                isLoadingSaved = false
            }
        }
    }

    fun loadStatuses() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== STARTING STATUS LOADING ===")
            isLoading = true
            errorMessage = null

            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val safUri = pref.getUriFromPreference()

            Log.d("StatusGalleryActivity", "Loading statuses with SAF URI: $safUri")

            try {
                // Only check if SAF URI is present
                if (safUri.isNullOrBlank()) {
                    Log.e("StatusGalleryActivity", "❌ NO SAF URI - Cannot load statuses")
                    errorMessage =
                        "Please grant access to the WhatsApp .Statuses folder in onboarding/settings."
                    isLoading = false
                    return@launch
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
                if (newHash != lastStatusesHash) {
                    Log.d("StatusGalleryActivity", "Statuses changed, updating UI")
                    statusList = filteredStatuses
                    lastStatusesHash = newHash
                } else {
                    Log.d("StatusGalleryActivity", "No changes in statuses, skipping UI update")
                }
                statusesLoaded = true
                isLoading = false
                Log.d("StatusGalleryActivity", "✅ Status loading completed successfully")

            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error loading statuses", e)
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    // Function to force refresh saved statuses (for manual refresh)
    fun forceRefreshSavedStatuses() {
        savedStatusesLoaded = false
        loadSavedStatuses()
    }

    // Function to force refresh statuses (for manual refresh)
    fun forceRefreshStatuses() {
        statusesLoaded = false
        loadStatuses()
    }

    // Function to mark as favorite
    fun markAsFavorite(status: StatusModel) {
        coroutineScope.launch {
            try {
                Log.d("StatusGalleryActivity", "Marking as favorite: ${status.filePath}")
                val success = FileUtils.markAsFavorite(context, status.filePath)
                if (success) {
                    Log.d("StatusGalleryActivity", "✅ Status marked as favorite successfully")
                    // Refresh the lists to get the updated file paths from disk
                    loadSavedStatuses()
                } else {
                    Log.e("StatusGalleryActivity", "❌ Failed to mark as favorite")
                }
            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error marking as favorite", e)
            }
        }
    }

    // Function to unmark as favorite
    fun unmarkAsFavorite(status: StatusModel) {
        coroutineScope.launch {
            try {
                Log.d("StatusGalleryActivity", "Unmarking as favorite: ${status.filePath}")
                val success = FileUtils.unmarkAsFavorite(context, status.filePath)
                if (success) {
                    Log.d("StatusGalleryActivity", "✅ Status unmarked as favorite successfully")
                    // Refresh the lists to get the updated file paths from disk
                    loadSavedStatuses()
                } else {
                    Log.e("StatusGalleryActivity", "❌ Failed to unmark as favorite")
                }
            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error unmarking as favorite", e)
            }
        }
    }

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

    val primaryGreen = Color(0xFF25D366)
    val darkGreen = Color(0xFF128C7E)
    val systemUiController = rememberSystemUiController()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set status bar color for gallery
    SideEffect {
        systemUiController.setStatusBarColor(primaryGreen, darkIcons = false)
    }

    // Live update when app comes to foreground
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                super.onStart(owner)
                // App came to foreground, check for new statuses
                Log.d("StatusGalleryActivity", "App came to foreground, checking for new statuses")
                
                // Check for new statuses based on current tab
                if (currentTab == 0) {
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
                                if (newHash != lastStatusesHash) {
                                    Log.d("StatusGalleryActivity", "New statuses detected, updating UI")
                                    statusList = newStatuses
                                    lastStatusesHash = newHash
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
                            if (newHash != lastSavedStatusesHash) {
                                Log.d("StatusGalleryActivity", "New saved statuses detected, updating UI")
                                savedStatusList = newSavedStatuses
                                favoriteList = newFavorites
                                lastSavedStatusesHash = newHash
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

    LaunchedEffect(Unit) {
        // Initial load
        loadStatuses()
        loadSavedStatuses()
    }
    
    // Periodic background check for new statuses while app is in foreground
    LaunchedEffect(currentTab) {
        while (true) {
            delay(15000) // Check every 15 seconds to be less aggressive
            
            // Only check if app is in foreground (current tab is active)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                // Check for new statuses based on current tab
                if (currentTab == 0) {
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
                            if (newHash != lastStatusesHash) {
                                Log.d("StatusGalleryActivity", "New statuses detected during background check, updating UI")
                                statusList = newStatuses
                                lastStatusesHash = newHash
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
                        if (newHash != lastSavedStatusesHash) {
                            Log.d("StatusGalleryActivity", "New saved statuses detected during background check, updating UI")
                            savedStatusList = newSavedStatuses
                            favoriteList = newFavorites
                            lastSavedStatusesHash = newHash
                        }
                    } catch (e: Exception) {
                        Log.e("StatusGalleryActivity", "Error during background saved status check", e)
                    }
                }
            }
        }
    }

    // Load statuses when switching to Statuses tab
    LaunchedEffect(currentTab) {
        if (currentTab == 0 && !statusesLoaded) {
            loadStatuses()
            statusesLoaded = true
        } else if (currentTab == 1 && !savedStatusesLoaded) {
            loadSavedStatuses()
            savedStatusesLoaded = true
        }
    }

    if (showStatusView) {
        StatusView(
            statusList = if (currentTab == 0) displayStatusList else savedStatusList,
            initialIndex = selectedStatusIndex,
            isFromSavedStatuses = currentTab == 1,
            onBackPressed = { showStatusView = false },
            onStatusSaved = {
                // Trigger refresh of saved statuses when a status is saved
                if (currentTab == 1) {
                    loadSavedStatuses()
                } else {
                    // Reset the loaded flag so next time Saved tab is opened, it will refresh
                    savedStatusesLoaded = false
                }
            }
        )
    } else {
        Scaffold(
            topBar = {
                Column {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    // Professional toolbar with integrated tabs
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(primaryGreen, darkGreen)
                                )
                            )
                    ) {
                        Column {
                            // Top toolbar section with title and refresh
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status Saver",
                                    color = Color.White,
                                    style = TextStyle(fontSize = 18.sp),
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { 
                                            if (currentTab == 0) {
                                                forceRefreshStatuses()
                                            } else {
                                                forceRefreshSavedStatuses()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Refresh",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Integrated tabs section
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 0.dp)
                            ) {
                                // Statuses Tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clickable { currentTab = 0 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Statuses",
                                            color = if (currentTab == 0) Color.White else Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        // Traditional tab indicator at bottom
                                        if (currentTab == 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.8f)
                                                    .height(2.dp)
                                                    .background(Color.White, RoundedCornerShape(1.dp))
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                    }
                                }
                                
                                // Saved Tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clickable { currentTab = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Saved",
                                            color = if (currentTab == 1) Color.White else Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp,
                                            fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                        // Traditional tab indicator at bottom
                                        if (currentTab == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.8f)
                                                    .height(2.dp)
                                                    .background(Color.White, RoundedCornerShape(1.dp))
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
                                if (!isLoading && errorMessage == null && statusList.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Filter tabs
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            // All tab
                                            Box(
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clickable { statusFilterTab = 0 }
                                                    .background(
                                                        if (statusFilterTab == 0) Color(0xFFE8F5E8) else Color.Transparent,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (statusFilterTab == 0) primaryGreen else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "All",
                                                    color = if (statusFilterTab == 0) primaryGreen else Color(0xFF757575),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (statusFilterTab == 0) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Image tab
                                            Box(
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clickable { statusFilterTab = 1 }
                                                    .background(
                                                        if (statusFilterTab == 1) Color(0xFFE8F5E8) else Color.Transparent,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (statusFilterTab == 1) primaryGreen else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Image",
                                                    color = if (statusFilterTab == 1) primaryGreen else Color(0xFF757575),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (statusFilterTab == 1) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Video tab
                                            Box(
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clickable { statusFilterTab = 2 }
                                                    .background(
                                                        if (statusFilterTab == 2) Color(0xFFE8F5E8) else Color.Transparent,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (statusFilterTab == 2) primaryGreen else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Video",
                                                    color = if (statusFilterTab == 2) primaryGreen else Color(0xFF757575),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (statusFilterTab == 2) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                        
                                        // Control icon (settings)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clickable { showSettingsBottomSheet = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.outline_dataset_24),
                                                contentDescription = "Display Controls",
                                                tint = Color(0xFF9E9E9E),
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Content area
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                ) {
                            when {
                                isLoading -> {
                                    // Show shimmer grid
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(36) { // Show 36 shimmer items to fill entire screen height
                                            ShimmerCard()
                                        }
                                    }
                                }

                                errorMessage != null -> {
                                    Log.d("StatusGalleryActivity", "Showing error state: $errorMessage")
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = "Error",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "Something went wrong",
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                errorMessage ?: "Unknown error",
                                                color = Color.Gray,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = { forceRefreshStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Try Again", color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }

                                        displayStatusList.isEmpty() -> {
                                    Log.d("StatusGalleryActivity", "Showing empty state - no statuses found")
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Home,
                                                contentDescription = "No Statuses",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                        "No Status Available",
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                        when (statusFilterTab) {
                                                            0 -> "Make sure you have granted folder permission and have WhatsApp statuses"
                                                            1 -> "No image statuses available"
                                                            2 -> "No video statuses available"
                                                            else -> "No statuses available"
                                                        },
                                                color = Color.Gray,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = { forceRefreshStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Refresh", color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Log.d(
                                        "StatusGalleryActivity",
                                                "Showing status grid with ${displayStatusList.size} statuses (filter: ${statusFilterTab})"
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(gridColumns),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                                itemsIndexed(displayStatusList) { index, status ->
                                                    com.inningsstudio.statussaver.presentation.ui.status.ModernStatusCard(
                                                status = status,
                                                context = context,
                                                thumbCache = thumbCache,
                                                getVideoThumbnailIO = { ctx, path -> getVideoThumbnailIO(ctx, path) },
                                                onClick = {
                                                    selectedStatusIndex = index
                                                    showStatusView = true
                                                }
                                            )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Saved tab
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Filter tabs for Saved Statuses
                                if (!isLoadingSaved && (savedStatusList.isNotEmpty() || favoriteList.isNotEmpty())) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Filter tabs
                                        Row(
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            // Saved tab
                                            Box(
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clickable { savedFilterTab = 0 }
                                                    .background(
                                                        if (savedFilterTab == 0) Color(0xFFE8F5E8) else Color.Transparent,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (savedFilterTab == 0) primaryGreen else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Saved",
                                                    color = if (savedFilterTab == 0) primaryGreen else Color(0xFF757575),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (savedFilterTab == 0) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.width(12.dp))
                                            
                                            // Favourites tab
                                            Box(
                                                modifier = Modifier
                                                    .height(36.dp)
                                                    .clickable { savedFilterTab = 1 }
                                                    .background(
                                                        if (savedFilterTab == 1) Color(0xFFE8F5E8) else Color.Transparent,
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (savedFilterTab == 1) primaryGreen else Color(0xFFE0E0E0),
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Favourites",
                                                    color = if (savedFilterTab == 1) primaryGreen else Color(0xFF757575),
                                                    fontSize = 13.sp,
                                                    fontWeight = if (savedFilterTab == 1) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Content area
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White)
                                ) {
                                    // Calculate display list for saved statuses
                                    val displaySavedList = when (savedFilterTab) {
                                        0 -> savedStatusList // Saved only
                                        1 -> favoriteList // Favourites only
                                        else -> savedStatusList
                                    }
                                    
                            when {
                                isLoadingSaved -> {
                                    // Show shimmer grid for saved statuses
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(gridColumns),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(36) { // Show 36 shimmer items to fill entire screen height
                                            ShimmerCard()
                                        }
                                    }
                                }

                                        displaySavedList.isEmpty() -> {
                                    Log.d("StatusGalleryActivity", "Showing empty state - no saved statuses found")
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                contentDescription = "No Saved Statuses",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                        "No Status Available",
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                        when (savedFilterTab) {
                                                            0 -> "No saved statuses available"
                                                            1 -> "No favourite statuses available"
                                                            else -> "No saved statuses available"
                                                        },
                                                color = Color.Gray,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = { forceRefreshSavedStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Refresh", color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Log.d(
                                        "StatusGalleryActivity",
                                        "Showing saved status grid with ${savedStatusList.size} saved and ${favoriteList.size} favorites"
                                    )
                                    
                                    LazyVerticalGrid(
                                                columns = GridCells.Fixed(gridColumns),
                                        modifier = Modifier.fillMaxSize(),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                                itemsIndexed(displaySavedList) { index, status ->
                                                    val isFavorite = StatusSaver.isFileInFavorites(status.filePath)
                                                    com.inningsstudio.statussaver.presentation.ui.status.SavedStatusCardWithActions(
                                                    status = status,
                                                        isFavorite = isFavorite,
                                                    context = context,
                                                    thumbCache = thumbCache,
                                                    getVideoThumbnailIO = { ctx, path -> getVideoThumbnailIO(ctx, path) },
                                                    onDelete = {
                                                        statusToDelete = status
                                                        showDeleteConfirmation = true
                                                    },
                                                    onFavoriteToggle = {
                                                            if (isFavorite) {
                                                        unmarkAsFavorite(status)
                                                            } else {
                                                                markAsFavorite(status)
                                                            }
                                                    },
                                                    onClick = {
                                                            selectedStatusIndex = index
                                                            showStatusView = true
                                                    }
                                                )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation && statusToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmation = false
                statusToDelete = null
            },
            title = {
                Text(
                    text = "Delete Saved Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this saved status? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Delete the saved status
                        statusToDelete?.let { status ->
                            coroutineScope.launch {
                                try {
                                    val success = FileUtils.deleteSavedStatus(context, status.filePath)
                                    if (success) {
                                        // Refresh the lists to get the updated state from disk
                                        loadSavedStatuses()
                                    }
                                } catch (e: Exception) {
                                    Log.e("StatusGalleryActivity", "Error deleting saved status", e)
                                }
                            }
                        }
                        showDeleteConfirmation = false
                        statusToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmation = false
                        statusToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Gray
        )
    }
    
    // Settings Bottom Sheet
    if (showSettingsBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState()
        
        ModalBottomSheet(
            onDismissRequest = { showSettingsBottomSheet = false },
            sheetState = bottomSheetState,
            containerColor = Color.White,
            dragHandle = {
                    Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                Box(
                    modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.Gray, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            Column(
                        modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Header
                Text(
                    text = "Display Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                        )
                
                // Grid Layout Section
                Text(
                    text = "Grid Layout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2 Columns option
            Box(
                modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { gridColumns = 2 }
                            .background(
                                if (gridColumns == 2) primaryGreen else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (gridColumns == 2) primaryGreen else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 2x2 Grid Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                                Box(
                        modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                        )
                                )
                                Box(
                        modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                    
                    // 3 Columns option
                    Box(
        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { gridColumns = 3 }
                            .background(
                                if (gridColumns == 3) primaryGreen else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (gridColumns == 3) primaryGreen else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 3x2 Grid Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                    Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                Box(
                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
            Box(
                modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                    )
                                )
            }
        }
    }
                    
                    // 4 Columns option
                    Box(
        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { gridColumns = 4 }
                            .background(
                                if (gridColumns == 4) primaryGreen else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (gridColumns == 4) primaryGreen else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 4x2 Grid Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                    Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                Box(
                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                    Box(
                        modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                        )
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
    Box(
        modifier = Modifier
                                        .size(5.dp)
            .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                )
            )
                                Box(
            modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                    )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sort Order Section
                Text(
                    text = "Sort Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Latest first option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrder = 0 }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == 0,
                            onClick = { sortOrder = 0 },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = primaryGreen,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Latest First",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = if (sortOrder == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    
                    // Oldest first option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sortOrder = 1 }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == 1,
                            onClick = { sortOrder = 1 },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = primaryGreen,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Oldest First",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = if (sortOrder == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
} 
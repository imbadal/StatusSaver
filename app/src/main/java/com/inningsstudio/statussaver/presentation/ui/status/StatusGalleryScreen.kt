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
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.app.Activity
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StandaloneStatusGallery(context: Context) {
    var statusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var savedStatusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingSaved by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStatusView by remember { mutableStateOf(false) }
    var selectedStatusIndex by remember { mutableStateOf(0) }
    var currentTab by remember { mutableStateOf(0) } // 0 = Statuses, 1 = Saved
    var savedStatusesRefreshTrigger by remember { mutableStateOf(0) } // Trigger for refreshing saved statuses
    var lastSavedStatusesHash by remember { mutableStateOf(0) } // Hash to detect changes
    var savedStatusesLoaded by remember { mutableStateOf(false) } // Track if saved statuses have been loaded
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

    fun loadSavedStatuses() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== STARTING SAVED STATUS LOADING ===")
            isLoadingSaved = true

            try {
                val savedStatuses = FileUtils.getSavedStatus(context)
                Log.d("StatusGalleryActivity", "Found ${savedStatuses.size} saved statuses")
                
                // Calculate hash of new statuses
                val newHash = calculateSavedStatusesHash(savedStatuses)
                
                // Only update state if there are actual changes
                if (newHash != lastSavedStatusesHash) {
                    Log.d("StatusGalleryActivity", "Saved statuses changed, updating UI")
                    savedStatusList = savedStatuses
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

    // Function to force refresh saved statuses (for manual refresh)
    fun forceRefreshSavedStatuses() {
        savedStatusesLoaded = false
        loadSavedStatuses()
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

    fun loadStatuses() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== STARTING STATUS LOADING ===")
            isLoading = true
            errorMessage = null

            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val safUri = pref.getUriFromPreference()

            Log.d("StatusGalleryActivity", "Loading statuses with SAF URI: $safUri")

            try {
                // First, let's check if we have permissions
                val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
                Log.d("StatusGalleryActivity", "Has required permissions: $hasPermissions")

                if (!hasPermissions) {
                    Log.e("StatusGalleryActivity", "❌ NO PERMISSIONS - Cannot load statuses")
                    errorMessage =
                        "Media permissions are required. Please grant permissions in app settings."
                    isLoading = false
                    return@launch
                }

                // Let's also check what paths are available
                val detector = StatusPathDetector()
                val allPaths = detector.getAllPossibleStatusPaths()
                Log.d("StatusGalleryActivity", "All possible paths: $allPaths")

                // Check each path manually
                allPaths.forEach { path ->
                    val folder = File(path)
                    Log.d("StatusGalleryActivity", "Checking path: $path")
                    Log.d("StatusGalleryActivity", "  - Exists: ${folder.exists()}")
                    Log.d("StatusGalleryActivity", "  - Is directory: ${folder.isDirectory}")
                    Log.d("StatusGalleryActivity", "  - Can read: ${folder.canRead()}")
                    Log.d("StatusGalleryActivity", "  - Is hidden: ${folder.isHidden}")

                    if (folder.exists() && folder.isDirectory && folder.canRead()) {
                        val files = folder.listFiles { _ -> true }
                        Log.d("StatusGalleryActivity", "  - Total files: ${files?.size ?: 0}")
                        files?.take(5)?.forEach { file ->
                            Log.d(
                                "StatusGalleryActivity",
                                "    - ${file.name} (hidden: ${file.isHidden}, size: ${file.length()})"
                            )
                        }
                    }
                }

                Log.d("StatusGalleryActivity", "Calling FileUtils.getStatus()...")
                val statuses = FileUtils.getStatus(context, safUri ?: "")
                Log.d(
                    "StatusGalleryActivity",
                    "FileUtils.getStatus() returned ${statuses.size} statuses"
                )

                // Log all statuses for debugging
                statuses.forEachIndexed { index, status ->
                    Log.d(
                        "StatusGalleryActivity",
                        "Status $index: ${status.fileName} (${status.filePath})"
                    )
                    Log.d("StatusGalleryActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("StatusGalleryActivity", "  - Is video: ${status.isVideo}")
                    Log.d("StatusGalleryActivity", "  - Last modified: ${status.lastModified}")
                    Log.d(
                        "StatusGalleryActivity",
                        "  - File path empty: ${status.filePath.isEmpty()}"
                    )
                }

                statusList = statuses.filter { it.filePath.isNotEmpty() }
                Log.d("StatusGalleryActivity", "Filtered to ${statusList.size} valid statuses")

                // Log details about found statuses
                statusList.forEachIndexed { index, status ->
                    Log.d(
                        "StatusGalleryActivity",
                        "Valid Status $index: ${status.fileName} (${status.filePath})"
                    )
                    Log.d("StatusGalleryActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("StatusGalleryActivity", "  - Is video: ${status.isVideo}")
                    Log.d("StatusGalleryActivity", "  - Last modified: ${status.lastModified}")
                }

                isLoading = false
                Log.d("StatusGalleryActivity", "✅ Status loading completed successfully")

            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error loading statuses", e)
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    fun debugStatusDetection() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== DEBUGGING STATUS DETECTION ===")

            // Check permissions
            val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
            Log.d("StatusGalleryActivity", "Has required permissions: $hasPermissions")

            // Check SAF URI
            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val safUri = pref.getUriFromPreference()
            Log.d("StatusGalleryActivity", "SAF URI from preferences: $safUri")

            // Check all possible paths
            val detector = StatusPathDetector()
            detector.debugWhatsAppPaths()

            Log.d("StatusGalleryActivity", "=== END DEBUGGING ===")
        }
    }

    val primaryGreen = Color(0xFF25D366)
    val darkGreen = Color(0xFF128C7E)
    val systemUiController = rememberSystemUiController()

    // Set status bar color for gallery
    SideEffect {
        systemUiController.setStatusBarColor(primaryGreen, darkIcons = false)
    }

    LaunchedEffect(Unit) {
        loadStatuses()
        loadSavedStatuses()
    }

    // Load saved statuses only once when first switching to Saved tab
    LaunchedEffect(currentTab) {
        if (currentTab == 1 && !savedStatusesLoaded) {
            loadSavedStatuses()
            savedStatusesLoaded = true
        }
    }

    if (showStatusView) {
        StatusView(
            statusList = if (currentTab == 0) statusList else savedStatusList,
            initialIndex = selectedStatusIndex,
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
                                    text = "StatusWp",
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
                                                loadStatuses()
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
            containerColor = Color(0xFFF8F9FA)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    count = 2,
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> { // Statuses tab
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
                                                onClick = { loadStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Try Again", color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { debugStatusDetection() },
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("Debug", color = primaryGreen)
                                            }
                                        }
                                    }
                                }

                                statusList.isEmpty() -> {
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
                                                "No Statuses Found",
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Make sure you have granted folder permission and have WhatsApp statuses",
                                                color = Color.Gray,
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = { loadStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Text("Refresh", color = Color.White, fontWeight = FontWeight.Medium)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedButton(
                                                onClick = { debugStatusDetection() },
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("Debug Detection", color = primaryGreen)
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    Log.d(
                                        "StatusGalleryActivity",
                                        "Showing status grid with ${statusList.size} statuses"
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        itemsIndexed(statusList) { index, status ->
                                            ModernStatusCard(
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
                        1 -> { // Saved tab
                            when {
                                isLoadingSaved -> {
                                    // Show shimmer grid for saved statuses
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

                                savedStatusList.isEmpty() -> {
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
                                                "No Saved Statuses",
                                                color = Color.Black,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Save some statuses to see them here",
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
                                        "Showing saved status grid with ${savedStatusList.size} statuses"
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        itemsIndexed(savedStatusList) { index, status ->
                                            ModernStatusCard(
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
            }
        }
    }
}

@Composable
fun ModernStatusCard(
    status: StatusModel,
    context: Context,
    thumbCache: MutableMap<String, Bitmap?>,
    getVideoThumbnailIO: suspend (Context, String) -> Bitmap?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(0.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(0.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (status.isVideo) {
                val thumb by produceState<Bitmap?>(
                    null,
                    status.filePath
                ) {
                    value = thumbCache[status.filePath]
                        ?: getVideoThumbnailIO(context, status.filePath).also {
                            thumbCache[status.filePath] = it
                        }
                }
                if (thumb != null) {
                    androidx.compose.foundation.Image(
                        bitmap = thumb!!.asImageBitmap(),
                        contentDescription = "Video thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                // Play icon overlay with better styling
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                AsyncImage(
                    model = status.filePath,
                    contentDescription = "Status image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun CustomToolbar(
    isStatusView: Boolean,
    title: String,
    currentIndex: Int,
    totalCount: Int,
    isLoading: Boolean,
    onBackPressed: () -> Unit,
    onRefresh: () -> Unit
) {
    val primaryGreen = Color(0xFF25D366)
    val darkGreen = Color(0xFF128C7E)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(primaryGreen, darkGreen)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isStatusView) {
                // Status View Toolbar: Back + Counter
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Gallery Toolbar: Title + Refresh
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.size(48.dp),
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
                        onClick = onRefresh,
                        modifier = Modifier.size(48.dp)
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
        }
    }
} 
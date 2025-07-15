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
                        val files = folder.listFiles { file -> true }
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

    fun loadSavedStatuses() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== STARTING SAVED STATUS LOADING ===")
            isLoadingSaved = true

            try {
                val savedStatuses = FileUtils.getSavedStatus(context)
                Log.d("StatusGalleryActivity", "Found ${savedStatuses.size} saved statuses")
                savedStatusList = savedStatuses
                isLoadingSaved = false
                Log.d("StatusGalleryActivity", "✅ Saved status loading completed successfully")
            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error loading saved statuses", e)
                isLoadingSaved = false
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

    val green = Color(0xFF25D366)
    val systemUiController = rememberSystemUiController()

    // Set status bar color for gallery
    SideEffect {
        systemUiController.setStatusBarColor(green, darkIcons = false)
    }

    LaunchedEffect(Unit) {
        loadStatuses()
        loadSavedStatuses()
    }

    // Refresh saved statuses when switching to Saved tab or when refresh is triggered
    LaunchedEffect(currentTab, savedStatusesRefreshTrigger) {
        if (currentTab == 1) {
            loadSavedStatuses()
        }
    }

    if (showStatusView) {
        StatusView(
            statusList = if (currentTab == 0) statusList else savedStatusList,
            initialIndex = selectedStatusIndex,
            onBackPressed = { showStatusView = false },
            onStatusSaved = {
                // Trigger refresh of saved statuses when a status is saved
                savedStatusesRefreshTrigger++
            }
        )
    } else {
        Scaffold(
            topBar = {
                Column {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    CustomToolbar(
                        isStatusView = showStatusView,
                        title = "StatusWp",
                        currentIndex = selectedStatusIndex,
                        totalCount = if (currentTab == 0) statusList.size else savedStatusList.size,
                        isLoading = if (currentTab == 0) isLoading else isLoadingSaved,
                        onBackPressed = { showStatusView = false },
                        onRefresh = { if (currentTab == 0) loadStatuses() else loadSavedStatuses() }
                    )
                    // Top navigation tabs
                    TabRow(
                        selectedTabIndex = currentTab,
                        containerColor = green,
                        contentColor = Color.White,
                        modifier = Modifier.height(40.dp),
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                                height = 3.dp,
                                color = Color.White
                            )
                        }
                    ) {
                        Tab(
                            selected = currentTab == 0,
                            onClick = { currentTab = 0 },
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = {
                                Text(
                                    "Statuses",
                                    fontSize = 14.sp,
                                    fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = currentTab == 1,
                            onClick = { currentTab = 1 },
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = {
                                Text(
                                    "Saved",
                                    fontSize = 14.sp,
                                    fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            },
            containerColor = Color.White
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
                                        contentPadding = PaddingValues(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("Error", color = Color.Red, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                errorMessage ?: "Unknown error",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { loadStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                            ) {
                                                Text("Retry", color = Color.Black)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { debugStatusDetection() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                            ) {
                                                Text("Debug", color = Color.White)
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("No Statuses Found", color = Color.White, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Make sure you have granted folder permission",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { loadStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                            ) {
                                                Text("Refresh", color = Color.Black)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { debugStatusDetection() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                            ) {
                                                Text("Debug Detection", color = Color.White)
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
                                        contentPadding = PaddingValues(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(statusList) { index, status ->
                                            Card(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(
                                                        0xFFF5F5F5
                                                    )
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Box(modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        selectedStatusIndex = index
                                                        showStatusView = true
                                                    }
                                                ) {
                                                    if (status.isVideo) {
                                                        val thumb by produceState<Bitmap?>(
                                                            null,
                                                            status.filePath
                                                        ) {
                                                            value = thumbCache[status.filePath]
                                                                ?: getVideoThumbnailIO(
                                                                    context,
                                                                    status.filePath
                                                                ).also {
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
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(48.dp)
                                                                )
                                                            }
                                                        }
                                                        // Play icon overlay
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.White.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.PlayArrow,
                                                                contentDescription = "Play",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(36.dp)
                                                            )
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
                                        contentPadding = PaddingValues(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("No Saved Statuses", color = Color.White, fontSize = 18.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Save some statuses to see them here",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = { loadSavedStatuses() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                                            ) {
                                                Text("Refresh", color = Color.Black)
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
                                        contentPadding = PaddingValues(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        itemsIndexed(savedStatusList) { index, status ->
                                            Card(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(2.dp)),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = Color(
                                                        0xFFF5F5F5
                                                    )
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                            ) {
                                                Box(modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        selectedStatusIndex = index
                                                        showStatusView = true
                                                    }
                                                ) {
                                                    if (status.isVideo) {
                                                        val thumb by produceState<Bitmap?>(
                                                            null,
                                                            status.filePath
                                                        ) {
                                                            value = thumbCache[status.filePath]
                                                                ?: getVideoThumbnailIO(
                                                                    context,
                                                                    status.filePath
                                                                ).also {
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
                                                                    tint = Color.Black,
                                                                    modifier = Modifier.size(48.dp)
                                                                )
                                                            }
                                                        }
                                                        // Play icon overlay
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Color.White.copy(alpha = 0.15f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.PlayArrow,
                                                                contentDescription = "Play",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(36.dp)
                                                            )
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
fun CustomToolbar(
    isStatusView: Boolean,
    title: String,
    currentIndex: Int,
    totalCount: Int,
    isLoading: Boolean,
    onBackPressed: () -> Unit,
    onRefresh: () -> Unit
) {
    val green = Color(0xFF25D366)
    
    Surface(
        color = green,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
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
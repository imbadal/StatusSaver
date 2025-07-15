package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.launch
import java.io.File
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.toArgb
import android.app.Activity
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.shimmer.ShimmerFrameLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.core.view.WindowCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.inningsstudio.statussaver.core.constants.LIGHT_GREEN
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.res.painterResource
import com.inningsstudio.statussaver.R

class StatusGalleryActivity : ComponentActivity() {

    companion object {
        private const val TAG = "StatusGalleryActivity_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== STATUS GALLERY ACTIVITY STARTED ===")
        Log.d(TAG, "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Check if permission is granted
        val preferenceUtils = PreferenceUtils(application)
        val safUri = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()

        Log.d(TAG, "SAF URI from preferences: $safUri")
        Log.d(TAG, "Onboarding completed: $onboardingCompleted")

        // Check permissions
        val hasPermissions = StorageAccessHelper.hasRequiredPermissions(this)
        Log.d(TAG, "Has required permissions: $hasPermissions")
        
        if (safUri.isNullOrBlank() || !onboardingCompleted) {
            Log.w(TAG, "❌ Permission not granted or onboarding not completed - launching onboarding")
            // Permission not granted, launch onboarding
            val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        Log.d(TAG, "✅ Permission granted and onboarding completed - showing status gallery")
        
        // Permission granted, show status gallery
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    StandaloneStatusGallery(this)
                }
            }
        }
    }
}

@Composable
fun ShimmerCard() {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            factory = { context ->
                ShimmerFrameLayout(context).apply {
                    val shimmer = com.facebook.shimmer.Shimmer.ColorHighlightBuilder()
                        .setBaseColor(0xFFE0E0E0.toInt())
                        .setHighlightColor(0xFFF5F5F5.toInt())
                        .setBaseAlpha(1.0f)
                        .setHighlightAlpha(1.0f)
                        .setDuration(1200)
                        .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
                        .setAutoStart(true)
                        .build()
                    setShimmer(shimmer)

                    // Add a child view to shimmer that fills the entire space
                    val child = android.view.View(context).apply {
                        setBackgroundColor(0xFFE0E0E0.toInt())
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(child)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StatusView(
    statusList: List<StatusModel>,
    initialIndex: Int,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)
    
    // Track ExoPlayer instances to terminate them on back press
    val players = remember { mutableListOf<ExoPlayer>() }
    
    // Get lifecycle owner for app background detection
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Observe lifecycle to pause/resume media
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // Pause all players when app goes to background
                    players.forEach { player ->
                        try {
                            if (player.isPlaying) {
                                player.pause()
                            }
                        } catch (e: Exception) {
                            // Ignore errors
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // Optionally resume players when app comes to foreground
                    // Uncomment the following if you want auto-resume:
                    // players.forEach { player ->
                    //     try {
                    //         if (!player.isPlaying) {
                    //             player.play()
                    //         }
                    //     } catch (e: Exception) {
                    //         // Ignore errors
                    //     }
                    // }
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pagerState = rememberPagerState(initialPage = initialIndex)

    // Track current index when user swipes
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            currentIndex = pagerState.currentPage
        }
    }

    // Enable edge-to-edge display and set black status bar
    SideEffect {
        (context as? Activity)?.let { activity ->
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            activity.window.statusBarColor = Color.Black.toArgb()
        }
    }

    // Handle back press - terminate all players and navigate back
    fun handleBackPress() {
        // Release all ExoPlayer instances
        players.forEach { player ->
            try {
                player.release()
            } catch (e: Exception) {
                // Ignore errors during release
            }
        }
        players.clear()
        
        // Navigate back to home screen
        onBackPressed()
    }

    // Handle system back gesture
    BackHandler {
        handleBackPress()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { handleBackPress() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                pageCount = statusList.size,
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { index ->
                val status = statusList[index]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (status.isVideo) {
                        // Video player with tap-to-show-controls and play/pause
                        var player by remember { mutableStateOf<ExoPlayer?>(null) }
                        var showControls by remember { mutableStateOf(true) }
                        
                        DisposableEffect(key1 = index) {
                            val newPlayer = ExoPlayer.Builder(context).build().apply {
                                val mediaItem = MediaItem.fromUri(status.filePath)
                                setMediaItem(mediaItem)
                                prepare()
                            }
                            player = newPlayer
                            players.add(newPlayer)
                            
                            onDispose {
                                try {
                                    newPlayer.release()
                                    players.remove(newPlayer)
                                } catch (e: Exception) {
                                    // Ignore errors during release
                                }
                            }
                        }
                        
                        // Pause if not the current page, play if current
                        LaunchedEffect(pagerState.currentPage) {
                            if (pagerState.currentPage == index) {
                                player?.playWhenReady = true
                                player?.play()
                            } else {
                                player?.pause()
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    // On tap: only show/hide controls, do not play/pause
                                    showControls = !showControls
                                }
                        ) {
                            AndroidView(
                                factory = { context ->
                                    StyledPlayerView(context).apply {
                                        useController = showControls
                                    }
                                },
                                update = { playerView ->
                                    playerView.player = player
                                    playerView.useController = showControls
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        // Image viewer
                        AsyncImage(
                            model = status.filePath,
                            contentDescription = "Status image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
            
            // Bottom action buttons in navigation bar area
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    // Download button
                    FloatingActionButton(
                        onClick = {
                            statusList.getOrNull(currentIndex)?.let { status ->
                                coroutineScope.launch {
                                    FileUtils.copyFileToInternalStorage(Uri.parse(status.filePath), context)
                                }
                            }
                        },
                        containerColor = Color.Transparent,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_download_24),
                            contentDescription = "Download",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Share button (for both images and videos)
                    FloatingActionButton(
                        onClick = {
                            statusList.getOrNull(currentIndex)?.let { status ->
                                coroutineScope.launch {
                                    FileUtils.shareStatus(context, status.filePath)
                                }
                            }
                        },
                        containerColor = Color.Transparent,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // Status counter
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${statusList.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StandaloneStatusGallery(context: Context) {
    var statusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showStatusView by remember { mutableStateOf(false) }
    var selectedStatusIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

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
                    errorMessage = "Media permissions are required. Please grant permissions in app settings."
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
                            Log.d("StatusGalleryActivity", "    - ${file.name} (hidden: ${file.isHidden}, size: ${file.length()})")
                        }
                    }
                }

                Log.d("StatusGalleryActivity", "Calling FileUtils.getStatus()...")
            val statuses = FileUtils.getStatus(context, safUri ?: "")
                Log.d("StatusGalleryActivity", "FileUtils.getStatus() returned ${statuses.size} statuses")

                // Log all statuses for debugging
                statuses.forEachIndexed { index, status ->
                    Log.d("StatusGalleryActivity", "Status $index: ${status.fileName} (${status.filePath})")
                    Log.d("StatusGalleryActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("StatusGalleryActivity", "  - Is video: ${status.isVideo}")
                    Log.d("StatusGalleryActivity", "  - Last modified: ${status.lastModified}")
                    Log.d("StatusGalleryActivity", "  - File path empty: ${status.filePath.isEmpty()}")
                }

            statusList = statuses.filter { it.filePath.isNotEmpty() }
                Log.d("StatusGalleryActivity", "Filtered to ${statusList.size} valid statuses")

                // Log details about found statuses
                statusList.forEachIndexed { index, status ->
                    Log.d("StatusGalleryActivity", "Valid Status $index: ${status.fileName} (${status.filePath})")
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

    val green = Color(0xFF25D366)

    // Set status bar color
    SideEffect {
        (context as? Activity)?.window?.statusBarColor = green.toArgb()
    }

    LaunchedEffect(Unit) { loadStatuses() }

    if (showStatusView) {
        StatusView(
            statusList = statusList,
            initialIndex = selectedStatusIndex,
            onBackPressed = { showStatusView = false }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Statuses", color = Color.White) },
                    actions = {
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            IconButton(onClick = { loadStatuses() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = green)
                )
            },
            containerColor = Color.White
        ) { innerPadding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)) {
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error", color = Color.Red, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "Unknown error", color = Color.White, fontSize = 14.sp)
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Statuses Found", color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Make sure you have granted folder permission", color = Color.Gray, fontSize = 14.sp)
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
                        Log.d("StatusGalleryActivity", "Showing status grid with ${statusList.size} statuses")
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
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
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
                                            val thumb by produceState<Bitmap?>(null, status.filePath) {
                                                value = thumbCache[status.filePath] ?: getVideoThumbnailIO(context, status.filePath).also {
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

@Composable
fun StandaloneStatusItem(status: StatusModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val isVideo = status.fileName.lowercase().endsWith(".mp4") || 
                     status.fileName.lowercase().endsWith(".3gp") ||
                     status.fileName.lowercase().endsWith(".mkv")
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isVideo) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("VIDEO", color = Color.White, fontSize = 10.sp)
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
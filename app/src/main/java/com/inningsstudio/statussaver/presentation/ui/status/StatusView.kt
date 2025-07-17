package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.inningsstudio.statussaver.R
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.data.model.StatusModel
import android.net.Uri
import android.widget.Toast
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Delete
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay

// Helper composable for circular icon with green stroke
@Composable
fun CircularIconWithStroke(
    icon: @Composable () -> Unit,
    borderColor: Color,
    borderWidth: Float = 6f, // Increased from 4f
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    showStroke: Boolean = true, // New parameter to control stroke visibility
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .drawBehind {
                if (showStroke) {
                    drawCircle(
                        color = borderColor,
                        radius = size.minDimension / 2f - borderWidth / 2f,
                        style = Stroke(width = borderWidth)
                    )
                }
            }
            .background(color = backgroundColor, shape = androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StatusView(
    statusList: List<StatusModel>,
    initialIndex: Int,
    isFromSavedStatuses: Boolean = false,
    onBackPressed: () -> Unit,
    onStatusSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialIndex)

    // Track ExoPlayer instances to terminate them on back press
    val players = remember { mutableListOf<ExoPlayer>() }

    // Get lifecycle owner for app background detection
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for permission dialog
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // State for delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // State for toolbar visibility
    var showToolbar by remember { mutableStateOf(false) }

    // System UI controller for professional status/nav bar handling
    val systemUiController = rememberSystemUiController()

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            PreferenceUtils(context.applicationContext as android.app.Application).setUriToPreference(uri.toString())
            statusList.getOrNull(pagerState.currentPage)?.let { status ->
                coroutineScope.launch {
                    Toast.makeText(context, "Saving status...", Toast.LENGTH_SHORT).show()
                    val success = FileUtils.saveStatusToFolder(context, uri, status.filePath)
                    Toast.makeText(
                        context,
                        if (success) "Saved successfully" else "Failed to save",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (success) {
                        onStatusSaved()
                    }
                }
            }
        }
    }

    // Add state for download progress
    var isDownloading by remember { mutableStateOf(false) }
    var isDownloaded by remember { mutableStateOf(false) }

    // Reset isDownloaded when the user changes the current page
    LaunchedEffect(pagerState.currentPage) {
        isDownloaded = false
    }

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

    // Set system UI for full-screen status view
    SideEffect {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
        systemUiController.setNavigationBarColor(Color.Black, darkIcons = false)
        systemUiController.isSystemBarsVisible = false
    }

    // Restore system bars when exiting StatusView
    DisposableEffect(Unit) {
        onDispose {
            // Restore system bars when leaving StatusView
            systemUiController.isSystemBarsVisible = true
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // VerticalPager for Reels-like experience
        VerticalPager(
            state = pagerState,
                pageCount = statusList.size,
                modifier = Modifier.fillMaxSize()
        ) { page ->
            val status = statusList[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showToolbar = !showToolbar
                    }
                ) {
                    if (status.isVideo) {
                        // Video player with tap-to-show-controls and play/pause
                        var player by remember { mutableStateOf<ExoPlayer?>(null) }
                    var showControls by remember { mutableStateOf(false) }

                    DisposableEffect(key1 = page) {
                            val newPlayer = ExoPlayer.Builder(context).build().apply {
                                val mediaItem = MediaItem.fromUri(status.filePath)
                                setMediaItem(mediaItem)
                                prepare()
                            play() // Auto-play video when loaded
                            }
                            player = newPlayer
                            players.add(newPlayer)

                            onDispose {
                                try {
                                newPlayer.pause()
                                    newPlayer.release()
                                    players.remove(newPlayer)
                                } catch (e: Exception) {
                                    // Ignore errors during release
                                }
                            }
                        }

                    // Handle video playback on page changes
                        LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage == page) {
                            // Auto-play video when this page becomes current
                                player?.play()
                            } else {
                            // Pause video when swiping away from this page
                                player?.pause()
                            }
                        }

                    // Synchronize video controls with toolbar visibility
                    LaunchedEffect(showToolbar) {
                        showControls = showToolbar
                                }
                    
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

            // Partially transparent toolbar overlay - appears above media when visible
            if (showToolbar) {
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
                    actions = {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${statusList.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

                        // Bottom action buttons - always visible (vertical layout like Instagram Reels)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isFromSavedStatuses) {
                        // Delete button for saved statuses
                        IconButton(
                            onClick = {
                                showDeleteConfirmation = true
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        // Download button for regular statuses
                        CircularIconWithStroke(
                            borderColor = Color(0xFF25D366), // WhatsApp green
                            showStroke = !isDownloading, // Hide stroke when downloading
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(enabled = !isDownloading) {
                                    val pref = PreferenceUtils(context.applicationContext as android.app.Application)
                                    val folderUri = pref.getUriFromPreference()
                                    val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
                                    statusList.getOrNull(pagerState.currentPage)?.let { status ->
                                        coroutineScope.launch {
                                            if (!hasPermissions) {
                                                showPermissionDialog = true
                                            } else if (folderUri.isNullOrBlank()) {
                                                folderPickerLauncher.launch(null)
                                            } else {
                                                val uri = Uri.parse(folderUri)
                                                Toast.makeText(context, "Saving status...", Toast.LENGTH_SHORT).show()
                                                isDownloading = true
                                                val startTime = System.currentTimeMillis()
                                                val success = FileUtils.saveStatusToFolder(
                                                    context,
                                                    uri,
                                                    status.filePath
                                                )
                                                val elapsed = System.currentTimeMillis() - startTime
                                                val minDuration = 1500L
                                                if (elapsed < minDuration) {
                                                    delay(minDuration - elapsed)
                                                }
                                                isDownloading = false
                                                isDownloaded = success
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Saved successfully" else "Failed to save",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                if (success) {
                                                    onStatusSaved()
                                                }
                                            }
                                        }
                                    }
                                },
                            icon = {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isDownloading) {
                                        // Calculate the exact radius to match the stroke
                                        val strokeRadius = 24.dp - 3.dp // 48dp/2 - borderWidth/2
                                        CircularProgressIndicator(
                                            color = Color(0xFF25D366),
                                            strokeWidth = 6.dp,
                                            modifier = Modifier.size(strokeRadius * 2)
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.baseline_download_24),
                                        contentDescription = "Download",
                                        tint = if (isDownloaded) Color(0xFF25D366) else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp)) // Reduced from 20.dp

                        // Share button (for both images and videos)
                        CircularIconWithStroke(
                            borderColor = Color(0xFF25D366),
                            showStroke = true,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    statusList.getOrNull(pagerState.currentPage)?.let { status ->
                                        coroutineScope.launch {
                                            FileUtils.shareStatus(context, status.filePath)
                                        }
                                    }
                                },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Storage permission is required to save statuses. Please grant permission in app settings.") },
            confirmButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("OK")
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
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
                        statusList.getOrNull(pagerState.currentPage)?.let { status ->
                            coroutineScope.launch {
                                val success = FileUtils.deleteSavedStatus(context, status.filePath)
                                Toast.makeText(
                                    context,
                                    if (success) "Deleted successfully" else "Failed to delete",
                                    Toast.LENGTH_SHORT
                                ).show()
                                if (success) {
                                    onStatusSaved() // This will refresh the saved statuses list
                                }
                            }
                        }
                        showDeleteConfirmation = false
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
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Gray
        )
    }
} 

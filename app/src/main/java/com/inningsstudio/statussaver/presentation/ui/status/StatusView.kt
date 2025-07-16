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
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)

    // Track ExoPlayer instances to terminate them on back press
    val players = remember { mutableListOf<ExoPlayer>() }

    // Get lifecycle owner for app background detection
    val lifecycleOwner = LocalLifecycleOwner.current

    // State for permission dialog
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // State for delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    // State for toolbar visibility
    var showToolbar by remember { mutableStateOf(true) }

    // System UI controller for professional status/nav bar handling
    val systemUiController = rememberSystemUiController()

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            PreferenceUtils(context.applicationContext as android.app.Application).setUriToPreference(uri.toString())
            statusList.getOrNull(currentIndex)?.let { status ->
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
        // Main content area - takes entire screen
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
                                    // On tap: show/hide controls and toolbar
                                    showControls = !showControls
                                    showToolbar = !showToolbar
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
                            text = "${currentIndex + 1} / ${statusList.size}",
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

            // Bottom action buttons - always visible
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    if (isFromSavedStatuses) {
                        // Delete button for saved statuses
                        FloatingActionButton(
                            onClick = {
                                showDeleteConfirmation = true
                            },
                            containerColor = Color.Transparent,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        // Download button for regular statuses
                        FloatingActionButton(
                            onClick = {
                                val pref =
                                    PreferenceUtils(context.applicationContext as android.app.Application)
                                val folderUri = pref.getUriFromPreference()
                                val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
                                statusList.getOrNull(currentIndex)?.let { status ->
                                    coroutineScope.launch {
                                        if (!hasPermissions) {
                                            showPermissionDialog = true
                                        } else if (folderUri.isNullOrBlank()) {
                                            folderPickerLauncher.launch(null)
                                        } else {
                                            val uri = Uri.parse(folderUri)
                                            Toast.makeText(context, "Saving status...", Toast.LENGTH_SHORT).show()
                                            val success = FileUtils.saveStatusToFolder(
                                                context,
                                                uri,
                                                status.filePath
                                            )
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
                        statusList.getOrNull(currentIndex)?.let { status ->
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
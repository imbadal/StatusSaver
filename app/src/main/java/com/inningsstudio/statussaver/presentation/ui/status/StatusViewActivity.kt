package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.inningsstudio.statussaver.core.constants.LIGHT_GREEN
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme
import kotlinx.coroutines.launch

class StatusViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            StatusSaverTheme {
                StatusViewScreen()
            }
        }
    }
}

@Composable
fun StatusViewScreen() {
    val context = LocalContext.current
    val statusPath = (context as? StatusViewActivity)?.intent?.getStringExtra("status_path") ?: ""
    val isVideo = (context as? StatusViewActivity)?.intent?.getBooleanExtra("is_video", false) ?: false
    
    var statusModel by remember { mutableStateOf<StatusModel?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(statusPath) {
        if (statusPath.isNotEmpty()) {
            statusModel = StatusModel(
                id = statusPath.hashCode().toLong(),
                filePath = statusPath,
                fileName = statusPath.substringAfterLast("/", statusPath),
                fileSize = 0L,
                lastModified = 0L,
                isVideo = isVideo
            )
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        statusModel?.let { status ->
            if (status.isVideo) {
                // Video player
                AndroidView(
                    factory = { context ->
                        StyledPlayerView(context).apply {
                            player = ExoPlayer.Builder(context).build().apply {
                                val mediaItem = MediaItem.fromUri(status.filePath)
                                setMediaItem(mediaItem)
                                prepare()
                            }
                        }
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
        
        // Action buttons
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            // Download button
            FloatingActionButton(
                onClick = {
                    statusModel?.let { status ->
                        coroutineScope.launch {
                            FileUtils.copyFileToInternalStorage(Uri.parse(status.filePath), context)
                        }
                    }
                },
                containerColor = LIGHT_GREEN,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Download",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Share button
            FloatingActionButton(
                onClick = {
                    statusModel?.let { status ->
                        coroutineScope.launch {
                            FileUtils.shareStatus(context, status.filePath)
                        }
                    }
                },
                containerColor = LIGHT_GREEN,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = Color.White
                )
            }
        }
        
        // Back button
        FloatingActionButton(
            onClick = { (context as? StatusViewActivity)?.finish() },
            containerColor = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}


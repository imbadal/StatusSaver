package com.inningsstudio.statussaver.presentation.ui.status

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.inningsstudio.statussaver.data.model.StatusModel
import com.inningsstudio.statussaver.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun StatusListingScreen(
    viewModel: MainViewModel,
    isSaved: Boolean = false
) {
    
    // Set status bar color to match gradient top color
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        activity?.let {
            // Set status bar color to match gradient top color (#1A1A2E)
            it.window.statusBarColor = android.graphics.Color.parseColor("#1A1A2E")
            
            // Make status bar icons light (white) for better visibility on dark background
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.window.decorView.systemUiVisibility = it.window.decorView.systemUiVisibility or 
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (val currentState = uiState) {
        is com.inningsstudio.statussaver.presentation.state.StatusUiState.Loading -> {
            LoadingView()
        }
        is com.inningsstudio.statussaver.presentation.state.StatusUiState.Success -> {
            val statusList = currentState.statuses.filter { it.filePath.isNotEmpty() }
            if (statusList.isEmpty()) {
                EmptyStateView {
                    viewModel.refreshStatuses()
                }
            } else {
                StatusGridView(statusList) { index ->
                    // Handle status click
                    val status = statusList[index]
                    val intent = Intent(context, StatusViewActivity::class.java).apply {
                        putExtra("status_path", status.filePath)
                        putExtra("is_video", status.fileName.lowercase().endsWith(".mp4") || 
                                               status.fileName.lowercase().endsWith(".3gp") ||
                                               status.fileName.lowercase().endsWith(".mkv"))
                    }
                    context.startActivity(intent)
                }
            }
        }
        is com.inningsstudio.statussaver.presentation.state.StatusUiState.Error -> {
            ErrorView(
                message = currentState.message,
                onRetry = {
                    viewModel.refreshStatuses()
                }
            )
        }
        is com.inningsstudio.statussaver.presentation.state.StatusUiState.Empty -> {
            EmptyStateView(
                onRefresh = {
                    viewModel.refreshStatuses()
                }
            )
        }
    }
}

@Composable
fun StatusGridView(statusList: List<StatusModel>, onStatusClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3)
        ) {
            itemsIndexed(statusList) { index, status ->
                val modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(2.dp)
                    .clickable {
                        onStatusClick(index)
                    }
                ImageItemView(status, modifier)
            }
        }
    }
}

@Composable
fun ImageItemView(status: StatusModel, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val isVideo = status.fileName.lowercase().endsWith(".mp4") || 
                     status.fileName.lowercase().endsWith(".3gp") ||
                     status.fileName.lowercase().endsWith(".mkv")
        
        if (isVideo) {
            // Video thumbnail - for now just show a play icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else {
            // Image
            AsyncImage(
                model = status.filePath,
                contentDescription = "Status image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF6366F1),
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun EmptyStateView(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Info",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF6366F1)
            )
            
            Text(
                text = "No Statuses Found",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            
            Text(
                text = "No new statuses available at the moment. Pull to refresh to check for new ones.",
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
            )
            // Refresh button
            Button(
                onClick = onRefresh,
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Text(
                    text = "Refresh",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFEF5350)
            )
            
            Text(
                text = "Error",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            
            Text(
                text = message,
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
            )

            
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Retry",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Text(
                    text = "Retry",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
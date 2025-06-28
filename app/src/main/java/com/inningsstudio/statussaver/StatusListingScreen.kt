package com.inningsstudio.statussaver

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun StatusListingScreen(statusList: List<StatusModel>, onStatusClick: (Int) -> Unit) {
    
    // Filter out empty status models (padding items)
    val validStatusList = statusList.filter { it.path.isNotEmpty() }
    
    if (validStatusList.isEmpty()) {
        // Show appropriate message based on the scenario
        EmptyStateView()
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3)
            ) {
                itemsIndexed(validStatusList) { index, statusModel ->
                    val modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(2.dp)
                        .clickable {
                            onStatusClick(index)
                        }
                    ImageItemView(statusModel, modifier)
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    // Rotation animation for refresh icon
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
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
            // Check if WhatsApp is installed
            if (!StatusPathDetector.isWhatsAppInstalled(LocalContext.current) && 
                !StatusPathDetector.isWhatsAppBusinessInstalled(LocalContext.current) &&
                !StatusPathDetector.isGBWhatsAppInstalled(LocalContext.current) &&
                !StatusPathDetector.isYoWhatsAppInstalled(LocalContext.current)) {
                
                // WhatsApp not installed - Colorful design
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(64.dp),
                    tint = if (isRefreshing) Color(0xFF6366F1).copy(alpha = shimmerAlpha) else Color(0xFF6366F1)
                )
                
                Text(
                    text = "No Status App Found",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRefreshing) Color.White.copy(alpha = shimmerAlpha) else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
                
                Text(
                    text = "Please install a messaging app that supports status features to view and save statuses.",
                    fontSize = 16.sp,
                    color = if (isRefreshing) Color(0xFFB0BEC5).copy(alpha = shimmerAlpha) else Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
                )
                
                // Refresh button
                Button(
                    onClick = { 
                        isRefreshing = true
                        // Simulate refresh functionality
                        // In real implementation, this would trigger status detection
                    },
                    modifier = Modifier.padding(top = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRefreshing) Color(0xFF6366F1).copy(alpha = shimmerAlpha) else Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isRefreshing) rotation else 0f),
                        tint = Color.White
                    )
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Refresh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
            } else {
                // WhatsApp is installed but no statuses found - Colorful design
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(64.dp),
                    tint = if (isRefreshing) Color(0xFF4CAF50).copy(alpha = shimmerAlpha) else Color(0xFF4CAF50)
                )
                
                Text(
                    text = "No Status Found",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRefreshing) Color.White.copy(alpha = shimmerAlpha) else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp)
                )
                
                Text(
                    text = "No statuses found at this time. Statuses will appear here once they are available.",
                    fontSize = 16.sp,
                    color = if (isRefreshing) Color(0xFFB0BEC5).copy(alpha = shimmerAlpha) else Color(0xFFB0BEC5),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
                )
                
                // Refresh button
                Button(
                    onClick = { 
                        isRefreshing = true
                        // Simulate refresh functionality
                        // In real implementation, this would trigger status detection
                    },
                    modifier = Modifier.padding(top = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRefreshing) Color(0xFF4CAF50).copy(alpha = shimmerAlpha) else Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (isRefreshing) rotation else 0f),
                        tint = Color.White
                    )
                    Text(
                        text = if (isRefreshing) "Refreshing..." else "Refresh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                // Show device info for debugging
                Card(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRefreshing) Color(0xFF1E1E1E).copy(alpha = shimmerAlpha) else Color(0xFF1E1E1E)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Device: ${StatusPathDetector.getDeviceManufacturer().replaceFirstChar { it.uppercase() }}\nAndroid: ${StatusPathDetector.getAndroidVersion()}",
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    // Auto-reset shimmer after 2 seconds
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(2000)
            isRefreshing = false
        }
    }
}

@Composable
fun ImageItemView(statusModel: StatusModel, modifier: Modifier) {

    if (statusModel.isVideo) {
        AsyncImage(
            model = statusModel.thumbnail,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = statusModel.imageRequest,
            contentDescription = "icon",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inningsstudio.statussaver.core.utils.StatusSaver
import com.inningsstudio.statussaver.data.model.StatusModel

@Composable
fun StatusGridContent(
    isLoading: Boolean,
    errorMessage: String?,
    statusList: List<StatusModel>,
    displayStatusList: List<StatusModel>,
    statusFilterTab: Int,
    gridColumns: Int,
    thumbCache: MutableMap<String, Bitmap?>,
    context: Context,
    getVideoThumbnailIO: suspend (Context, String) -> Bitmap?,
    onStatusClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onDelete: (StatusModel) -> Unit,
    onFavoriteToggle: (StatusModel) -> Unit,
    isSavedTab: Boolean = false,
    savedFilterTab: Int = 0
) {
    val primaryGreen = Color(0xFF25D366)
    
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
                            errorMessage,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onRefresh,
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
                            imageVector = if (isSavedTab) Icons.Filled.Favorite else Icons.Filled.Home,
                            contentDescription = if (isSavedTab) "No Saved Statuses" else "No Statuses",
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
                            when {
                                isSavedTab -> when (savedFilterTab) {
                                    0 -> "No saved statuses available"
                                    1 -> "No favourite statuses available"
                                    else -> "No saved statuses available"
                                }
                                else -> when (statusFilterTab) {
                                    0 -> "Make sure you have granted folder permission and have WhatsApp statuses"
                                    1 -> "No image statuses available"
                                    2 -> "No video statuses available"
                                    else -> "No statuses available"
                                }
                            },
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onRefresh,
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
                    "Showing status grid with ${displayStatusList.size} statuses"
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(displayStatusList) { index, status ->
                        if (isSavedTab) {
                            val isFavorite = StatusSaver.isFileInFavorites(status.filePath)
                            SavedStatusCardWithActions(
                                status = status,
                                isFavorite = isFavorite,
                                context = context,
                                thumbCache = thumbCache,
                                getVideoThumbnailIO = { ctx, path -> getVideoThumbnailIO(ctx, path) },
                                onDelete = { onDelete(status) },
                                onFavoriteToggle = { onFavoriteToggle(status) },
                                onClick = { onStatusClick(index) }
                            )
                        } else {
                            ModernStatusCard(
                                status = status,
                                context = context,
                                thumbCache = thumbCache,
                                getVideoThumbnailIO = { ctx, path -> getVideoThumbnailIO(ctx, path) },
                                onClick = { onStatusClick(index) }
                            )
                        }
                    }
                }
            }
        }
    }
} 
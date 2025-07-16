package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background

@Composable
fun SavedStatusScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var savedStatuses by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var favoriteStatuses by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Filter tabs for Saved Statuses
    var savedFilterTab by remember { mutableStateOf(0) } // 0 = Favourites, 1 = Others
    var displaySavedList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    
    // Function to filter and sort saved statuses based on selected tab
    fun filterAndSortSavedStatuses(allStatuses: List<StatusModel>, favorites: List<StatusModel>, filterTab: Int): List<StatusModel> {
        return when (filterTab) {
            0 -> {
                // Favourites - sort by last modified time (latest first)
                favorites.sortedByDescending { it.lastModified }
            }
            1 -> {
                // Others (non-favorites) - sort by last modified time (latest first)
                allStatuses.sortedByDescending { it.lastModified }
            }
            else -> favorites.sortedByDescending { it.lastModified }
        }
    }
    
    // Update display list when source lists or filter changes
    LaunchedEffect(savedStatuses, favoriteStatuses, savedFilterTab) {
        displaySavedList = filterAndSortSavedStatuses(savedStatuses, favoriteStatuses, savedFilterTab)
    }
    
    // Load saved statuses when the screen is first displayed
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoading = true
                val saved = FileUtils.getSavedStatusesFromFolder(context)
                val favorites = FileUtils.getFavoriteStatusesFromFolder(context)
                savedStatuses = saved
                favoriteStatuses = favorites
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error loading saved statuses: ${e.message}"
                isLoading = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    errorMessage = null
                                    val saved = FileUtils.getSavedStatusesFromFolder(context)
                                    val favorites = FileUtils.getFavoriteStatusesFromFolder(context)
                                    savedStatuses = saved
                                    favoriteStatuses = favorites
                                    isLoading = false
                                } catch (e: Exception) {
                                    errorMessage = "Error loading saved statuses: ${e.message}"
                                    isLoading = false
                                }
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            displaySavedList.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Status Available",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (savedFilterTab) {
                            0 -> "No favourite statuses available"
                            1 -> "No other saved statuses available"
                            else -> "No saved statuses available"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter tabs header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter tabs
                        Row(
                            horizontalArrangement = Arrangement.Start
                        ) {
                            // Favourites tab
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
                                        color = if (savedFilterTab == 0) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Favourites",
                                    color = if (savedFilterTab == 0) Color(0xFF4CAF50) else Color(0xFF757575),
                                    fontSize = 13.sp,
                                    fontWeight = if (savedFilterTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Others tab
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
                                        color = if (savedFilterTab == 1) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Others",
                                    color = if (savedFilterTab == 1) Color(0xFF4CAF50) else Color(0xFF757575),
                                    fontSize = 13.sp,
                                    fontWeight = if (savedFilterTab == 1) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Content grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displaySavedList) { status ->
                            StandaloneStatusItem(
                                status = status,
                                onClick = { /* Handle status click */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
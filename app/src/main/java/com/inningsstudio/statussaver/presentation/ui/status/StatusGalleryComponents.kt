package com.inningsstudio.statussaver.presentation.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

@Composable
fun StatusFilterTabs(
    currentFilter: Int,
    onFilterChanged: (Int) -> Unit,
    showSettingsButton: Boolean = false,
    onSettingsClick: (() -> Unit)? = null
) {
    val primaryGreen = Color(0xFF25D366)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter tabs
        Row(
            horizontalArrangement = Arrangement.Start
        ) {
            // All tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        if (currentFilter == 0) Color(0xFFE8F5E8) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (currentFilter == 0) primaryGreen else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All",
                    color = if (currentFilter == 0) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 0) FontWeight.Bold else FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Image tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        if (currentFilter == 1) Color(0xFFE8F5E8) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (currentFilter == 1) primaryGreen else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Image",
                    color = if (currentFilter == 1) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 1) FontWeight.Bold else FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Video tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        if (currentFilter == 2) Color(0xFFE8F5E8) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (currentFilter == 2) primaryGreen else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Video",
                    color = if (currentFilter == 2) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 2) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        
        // Settings button (optional)
        if (showSettingsButton && onSettingsClick != null) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.inningsstudio.statussaver.R.drawable.outline_dataset_24),
                    contentDescription = "Display Controls",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun SavedStatusFilterTabs(
    currentFilter: Int,
    onFilterChanged: (Int) -> Unit
) {
    val primaryGreen = Color(0xFF25D366)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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
                    .background(
                        if (currentFilter == 0) Color(0xFFE8F5E8) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (currentFilter == 0) primaryGreen else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Favourites",
                    color = if (currentFilter == 0) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 0) FontWeight.Bold else FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Others tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .background(
                        if (currentFilter == 1) Color(0xFFE8F5E8) else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (currentFilter == 1) primaryGreen else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Others",
                    color = if (currentFilter == 1) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 1) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
} 
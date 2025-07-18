package com.inningsstudio.statussaver.presentation.ui.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inningsstudio.statussaver.R

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
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter tabs
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start
        ) {
            // All tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clickable { onFilterChanged(0) }
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
                    .clickable { onFilterChanged(1) }
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
                    .clickable { onFilterChanged(2) }
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
        
        // Control icon (settings) - only show if requested
        if (showSettingsButton && onSettingsClick != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_dataset_24),
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
            // Saved tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clickable { onFilterChanged(0) }
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
                    text = "Saved",
                    color = if (currentFilter == 0) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 0) FontWeight.Bold else FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Favourites tab
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clickable { onFilterChanged(1) }
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
                    text = "Favourites",
                    color = if (currentFilter == 1) primaryGreen else Color(0xFF757575),
                    fontSize = 13.sp,
                    fontWeight = if (currentFilter == 1) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
} 
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSettingsBottomSheet(
    showSettingsBottomSheet: Boolean,
    onDismiss: () -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    sortOrder: Int,
    onSortOrderChange: (Int) -> Unit
) {
    if (showSettingsBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState()
        val primaryGreen = Color(0xFF25D366)
        
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            containerColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(Color.Gray, RoundedCornerShape(2.dp))
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Header
                Text(
                    text = "Display Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // Grid Layout Section
                Text(
                    text = "Grid Layout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2 Columns option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onGridColumnsChange(2) }
                            .background(
                                if (gridColumns == 2) primaryGreen else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (gridColumns == 2) primaryGreen else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 2x2 Grid Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (gridColumns == 2) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                    
                    // 3 Columns option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onGridColumnsChange(3) }
                            .background(
                                if (gridColumns == 3) primaryGreen else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (gridColumns == 3) primaryGreen else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 3x2 Grid Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (gridColumns == 3) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                    
                    // 4 Columns option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onGridColumnsChange(4) }
                            .background(
                                if (gridColumns == 4) primaryGreen else Color(0xFFF5F5F5),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (gridColumns == 4) primaryGreen else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 4x2 Grid Icon
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            if (gridColumns == 4) Color.White else Color.Gray,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Sort Order Section
                Text(
                    text = "Sort Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Latest first option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderChange(0) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == 0,
                            onClick = { onSortOrderChange(0) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = primaryGreen,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Latest First",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = if (sortOrder == 0) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    
                    // Oldest first option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSortOrderChange(1) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == 1,
                            onClick = { onSortOrderChange(1) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = primaryGreen,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Oldest First",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black,
                            fontWeight = if (sortOrder == 1) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
} 
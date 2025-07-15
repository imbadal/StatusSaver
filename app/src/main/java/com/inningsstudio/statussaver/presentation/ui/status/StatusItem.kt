package com.inningsstudio.statussaver.presentation.ui.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.inningsstudio.statussaver.data.model.StatusModel

@Composable
fun StandaloneStatusItem(status: StatusModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val isVideo = status.fileName.lowercase().endsWith(".mp4") ||
                status.fileName.lowercase().endsWith(".3gp") ||
                status.fileName.lowercase().endsWith(".mkv")

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isVideo) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("VIDEO", color = Color.White, fontSize = 10.sp)
                }
            } else {
                AsyncImage(
                    model = status.filePath,
                    contentDescription = "Status image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
} 
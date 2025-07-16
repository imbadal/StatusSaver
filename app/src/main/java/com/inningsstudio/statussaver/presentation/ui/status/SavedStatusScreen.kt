package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.launch

@Composable
fun SavedStatusScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var savedStatuses by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load saved statuses when the screen is first displayed
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                isLoading = true
                val statuses = FileUtils.getSavedStatusesFromFolder(context)
                savedStatuses = statuses
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
                                    val statuses = FileUtils.getSavedStatusesFromFolder(context)
                                    savedStatuses = statuses
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
            savedStatuses.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No saved statuses found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Saved statuses will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedStatuses) { status ->
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
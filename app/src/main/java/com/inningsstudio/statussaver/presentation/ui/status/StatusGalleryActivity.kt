package com.inningsstudio.statussaver.presentation.ui.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.launch
import java.io.File

class StatusGalleryActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "StatusGalleryActivity_"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "=== STATUS GALLERY ACTIVITY STARTED ===")
        Log.d(TAG, "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Check if permission is granted
        val preferenceUtils = PreferenceUtils(application)
        val safUri = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
        
        Log.d(TAG, "SAF URI from preferences: $safUri")
        Log.d(TAG, "Onboarding completed: $onboardingCompleted")
        
        // Check permissions
        val hasPermissions = StorageAccessHelper.hasRequiredPermissions(this)
        Log.d(TAG, "Has required permissions: $hasPermissions")
        
        if (safUri.isNullOrBlank() || !onboardingCompleted) {
            Log.w(TAG, "❌ Permission not granted or onboarding not completed - launching onboarding")
            // Permission not granted, launch onboarding
            val intent = Intent(this, com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        Log.d(TAG, "✅ Permission granted and onboarding completed - showing status gallery")
        
        // Permission granted, show status gallery
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    StandaloneStatusGallery(this)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandaloneStatusGallery(context: Context) {
    var statusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadStatuses() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== STARTING STATUS LOADING ===")
            isLoading = true
            errorMessage = null
            
            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val safUri = pref.getUriFromPreference()
            
            Log.d("StatusGalleryActivity", "Loading statuses with SAF URI: $safUri")
            
            try {
                // First, let's check if we have permissions
                val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
                Log.d("StatusGalleryActivity", "Has required permissions: $hasPermissions")
                
                if (!hasPermissions) {
                    Log.e("StatusGalleryActivity", "❌ NO PERMISSIONS - Cannot load statuses")
                    errorMessage = "Media permissions are required. Please grant permissions in app settings."
                    isLoading = false
                    return@launch
                }
                
                // Let's also check what paths are available
                val detector = StatusPathDetector()
                val allPaths = detector.getAllPossibleStatusPaths()
                Log.d("StatusGalleryActivity", "All possible paths: $allPaths")
                
                // Check each path manually
                allPaths.forEach { path ->
                    val folder = File(path)
                    Log.d("StatusGalleryActivity", "Checking path: $path")
                    Log.d("StatusGalleryActivity", "  - Exists: ${folder.exists()}")
                    Log.d("StatusGalleryActivity", "  - Is directory: ${folder.isDirectory}")
                    Log.d("StatusGalleryActivity", "  - Can read: ${folder.canRead()}")
                    Log.d("StatusGalleryActivity", "  - Is hidden: ${folder.isHidden}")
                    
                    if (folder.exists() && folder.isDirectory && folder.canRead()) {
                        val files = folder.listFiles { file -> true }
                        Log.d("StatusGalleryActivity", "  - Total files: ${files?.size ?: 0}")
                        files?.take(5)?.forEach { file ->
                            Log.d("StatusGalleryActivity", "    - ${file.name} (hidden: ${file.isHidden}, size: ${file.length()})")
                        }
                    }
                }
                
                Log.d("StatusGalleryActivity", "Calling FileUtils.getStatus()...")
                val statuses = FileUtils.getStatus(context, safUri ?: "")
                Log.d("StatusGalleryActivity", "FileUtils.getStatus() returned ${statuses.size} statuses")
                
                // Log all statuses for debugging
                statuses.forEachIndexed { index, status ->
                    Log.d("StatusGalleryActivity", "Status $index: ${status.fileName} (${status.filePath})")
                    Log.d("StatusGalleryActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("StatusGalleryActivity", "  - Is video: ${status.isVideo}")
                    Log.d("StatusGalleryActivity", "  - Last modified: ${status.lastModified}")
                    Log.d("StatusGalleryActivity", "  - File path empty: ${status.filePath.isEmpty()}")
                }
                
                statusList = statuses.filter { it.filePath.isNotEmpty() }
                Log.d("StatusGalleryActivity", "Filtered to ${statusList.size} valid statuses")
                
                // Log details about found statuses
                statusList.forEachIndexed { index, status ->
                    Log.d("StatusGalleryActivity", "Valid Status $index: ${status.fileName} (${status.filePath})")
                    Log.d("StatusGalleryActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("StatusGalleryActivity", "  - Is video: ${status.isVideo}")
                    Log.d("StatusGalleryActivity", "  - Last modified: ${status.lastModified}")
                }
                
                isLoading = false
                Log.d("StatusGalleryActivity", "✅ Status loading completed successfully")
                
            } catch (e: Exception) {
                Log.e("StatusGalleryActivity", "❌ Error loading statuses", e)
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    fun debugStatusDetection() {
        coroutineScope.launch {
            Log.d("StatusGalleryActivity", "=== DEBUGGING STATUS DETECTION ===")
            
            // Check permissions
            val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
            Log.d("StatusGalleryActivity", "Has required permissions: $hasPermissions")
            
            // Check SAF URI
            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val safUri = pref.getUriFromPreference()
            Log.d("StatusGalleryActivity", "SAF URI from preferences: $safUri")
            
            // Check all possible paths
            val detector = StatusPathDetector()
            detector.debugWhatsAppPaths()
            
            Log.d("StatusGalleryActivity", "=== END DEBUGGING ===")
        }
    }

    LaunchedEffect(Unit) {
        loadStatuses()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Log.d("StatusGalleryActivity", "Showing loading state")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading statuses...", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
            errorMessage != null -> {
                Log.d("StatusGalleryActivity", "Showing error state: $errorMessage")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error", color = Color.Red, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "Unknown error", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadStatuses() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Retry", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { debugStatusDetection() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Debug", color = Color.White)
                        }
                    }
                }
            }
            statusList.isEmpty() -> {
                Log.d("StatusGalleryActivity", "Showing empty state - no statuses found")
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Statuses Found", color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Make sure you have granted folder permission", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadStatuses() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Refresh", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { debugStatusDetection() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Debug Detection", color = Color.White)
                        }
                    }
                }
            }
            else -> {
                Log.d("StatusGalleryActivity", "Showing status grid with ${statusList.size} statuses")
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header with refresh button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "WhatsApp Statuses (${statusList.size})",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        IconButton(
                            onClick = { loadStatuses() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                    
                    // Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        itemsIndexed(statusList) { index, status ->
                            StandaloneStatusItem(status) {
                                Log.d("StatusGalleryActivity", "Status clicked: ${status.fileName}")
                                // Open StatusViewActivity to show/play the status
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
                }
            }
        }
    }
}

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
package com.inningsstudio.statussaver.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.core.utils.StatusPathDetector
import com.inningsstudio.statussaver.core.utils.FileUtils
import com.inningsstudio.statussaver.data.model.StatusModel
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity_"
    }

    private lateinit var requestPermissionsLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>
    private var onPermissionsGranted: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== MAIN ACTIVITY STARTED ===")

        // Register permission launcher
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Log.d(TAG, "✅ Permissions granted via launcher")
                onPermissionsGranted?.invoke()
            } else {
                Log.e(TAG, "❌ Permissions denied via launcher")
            }
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    StandaloneStatusGallery(this@MainActivity, ::requestMediaPermissions)
                }
            }
        }
    }

    private fun requestMediaPermissions(onGranted: () -> Unit) {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onGranted()
        } else {
            onPermissionsGranted = onGranted
            requestPermissionsLauncher.launch(permissions)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandaloneStatusGallery(context: Context, requestMediaPermissions: ((onGranted: () -> Unit) -> Unit)) {
    var statusList by remember { mutableStateOf<List<StatusModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun loadStatuses() {
        coroutineScope.launch {
            Log.d("MainActivity", "=== STARTING STATUS LOADING ===")
            isLoading = true
            errorMessage = null
            
            val pref = PreferenceUtils(context.applicationContext as android.app.Application)
            val safUri = pref.getUriFromPreference()
            
            Log.d("MainActivity", "Loading statuses with SAF URI: $safUri")
            
            try {
                // First, let's check if we have permissions
                val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
                Log.d("MainActivity", "Has required permissions: $hasPermissions")
                
                if (!hasPermissions) {
                    Log.e("MainActivity", "❌ NO PERMISSIONS - Cannot load statuses")
                    errorMessage = "Media permissions are required. Please grant permissions in app settings."
                    isLoading = false
                    return@launch
                }
                
                // Let's also check what paths are available
                val detector = StatusPathDetector()
                val allPaths = detector.getAllPossibleStatusPaths()
                Log.d("MainActivity", "All possible paths: $allPaths")
                
                // Check each path manually
                allPaths.forEach { path ->
                    val folder = File(path)
                    Log.d("MainActivity", "Checking path: $path")
                    Log.d("MainActivity", "  - Exists: ${folder.exists()}")
                    Log.d("MainActivity", "  - Is directory: ${folder.isDirectory}")
                    Log.d("MainActivity", "  - Can read: ${folder.canRead()}")
                    Log.d("MainActivity", "  - Is hidden: ${folder.isHidden}")
                    
                    if (folder.exists() && folder.isDirectory && folder.canRead()) {
                        val files = folder.listFiles { file -> true }
                        Log.d("MainActivity", "  - Total files: ${files?.size ?: 0}")
                        files?.take(5)?.forEach { file ->
                            Log.d("MainActivity", "    - ${file.name} (hidden: ${file.isHidden}, size: ${file.length()})")
                        }
                    }
                }
                
                Log.d("MainActivity", "Calling FileUtils.getStatus()...")
                val statuses = FileUtils.getStatus(context, safUri ?: "")
                Log.d("MainActivity", "FileUtils.getStatus() returned ${statuses.size} statuses")
                
                // Log all statuses for debugging
                statuses.forEachIndexed { index, status ->
                    Log.d("MainActivity", "Status $index: ${status.fileName} (${status.filePath})")
                    Log.d("MainActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("MainActivity", "  - Is video: ${status.isVideo}")
                    Log.d("MainActivity", "  - Last modified: ${status.lastModified}")
                    Log.d("MainActivity", "  - File path empty: ${status.filePath.isEmpty()}")
                }
                
                statusList = statuses.filter { it.filePath.isNotEmpty() }
                Log.d("MainActivity", "Filtered to ${statusList.size} valid statuses")
                
                // Log details about found statuses
                statusList.forEachIndexed { index, status ->
                    Log.d("MainActivity", "Valid Status $index: ${status.fileName} (${status.filePath})")
                    Log.d("MainActivity", "  - Size: ${status.fileSize} bytes")
                    Log.d("MainActivity", "  - Is video: ${status.isVideo}")
                    Log.d("MainActivity", "  - Last modified: ${status.lastModified}")
                }
                
                isLoading = false
                Log.d("MainActivity", "✅ Status loading completed successfully")
                
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error loading statuses", e)
                errorMessage = e.message
                isLoading = false
            }
        }
    }

    fun handleRetry() {
        val hasPermissions = StorageAccessHelper.hasRequiredPermissions(context)
        if (!hasPermissions) {
            requestMediaPermissions {
                loadStatuses()
            }
        } else {
            loadStatuses()
        }
    }

    LaunchedEffect(Unit) {
        loadStatuses()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading statuses...", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
            errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error", color = Color.Red, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "Unknown error", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { handleRetry() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Retry", color = Color.Black)
                        }
                    }
                }
            }
            statusList.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Statuses Found", color = Color.White, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Make sure you have granted folder permission", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { handleRetry() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("Refresh", color = Color.Black)
                        }
                    }
                }
            }
            else -> {
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
                                imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                    // Grid
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(statusList.size) { index ->
                            val status = statusList[index]
                            Card(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f),
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
                                                imageVector = androidx.compose.material.icons.Icons.Filled.PlayArrow,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("VIDEO", color = Color.White, fontSize = 10.sp)
                                        }
                                    } else {
                                        coil.compose.AsyncImage(
                                            model = status.filePath,
                                            contentDescription = "Status image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

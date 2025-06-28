package com.inningsstudio.statussaver.presentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme

class PermissionRequestActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StatusSaverTheme {
                PermissionRequestScreen(
                    onPermissionGranted = {
                        // Navigate to main activity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    
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
            // Icon
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Permission Required",
                modifier = Modifier.size(80.dp),
                tint = Color(0xFF6366F1)
            )
            
            // Title
            Text(
                text = "Storage Permission Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )
            
            // Description
            Text(
                text = "This app needs access to your device storage to find and save WhatsApp statuses. " +
                       "Please grant the 'All Files Access' permission to continue.",
                fontSize = 16.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            // Android version specific info
            if (android.os.Build.VERSION.SDK_INT >= 34) {
                Text(
                    text = "Android 15 requires special permission for file access",
                    fontSize = 14.sp,
                    color = Color(0xFFEF5350),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            
            // Permission button
            Button(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= 34) {
                        // For Android 15, request MANAGE_EXTERNAL_STORAGE
                        val activity = context as? FragmentActivity
                        if (activity != null) {
                            StorageAccessHelper.requestManageExternalStoragePermission(activity)
                        }
                    }
                },
                modifier = Modifier.padding(top = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Grant Permission",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Text(
                    text = "Grant Permission",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Check permission button
            Button(
                onClick = {
                    if (StorageAccessHelper.hasRequiredPermissions(context)) {
                        onPermissionGranted()
                    }
                },
                modifier = Modifier.padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Check Permission",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White
                )
                Text(
                    text = "Check Permission",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            // Instructions
            Card(
                modifier = Modifier.padding(top = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A3E)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "How to grant permission:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "1. Tap 'Grant Permission'\n" +
                               "2. Go to 'All files access'\n" +
                               "3. Enable the toggle for this app\n" +
                               "4. Return and tap 'Check Permission'",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
} 
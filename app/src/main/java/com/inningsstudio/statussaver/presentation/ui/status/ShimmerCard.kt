package com.inningsstudio.statussaver.presentation.ui.status

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.shimmer.ShimmerFrameLayout

@Composable
fun ShimmerCard() {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            factory = { context ->
                ShimmerFrameLayout(context).apply {
                    val shimmer = com.facebook.shimmer.Shimmer.ColorHighlightBuilder()
                        .setBaseColor(0xFFE0E0E0.toInt())
                        .setHighlightColor(0xFFF5F5F5.toInt())
                        .setBaseAlpha(1.0f)
                        .setHighlightAlpha(1.0f)
                        .setDuration(1200)
                        .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
                        .setAutoStart(true)
                        .build()
                    setShimmer(shimmer)

                    // Add a child view to shimmer that fills the entire space
                    val child = android.view.View(context).apply {
                        setBackgroundColor(0xFFE0E0E0.toInt())
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    addView(child)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
} 
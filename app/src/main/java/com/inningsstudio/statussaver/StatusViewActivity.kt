package com.inningsstudio.statussaver

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme

class StatusViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StatusSaverTheme {
                // A surface container using the 'background' color from the theme
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                Scaffold(
                    bottomBar = {
                        BottomAppBar() {
                            IconButton(
                                onClick = {
                                    (context as StatusViewActivity).finish()
                                }
                            ) {
                                Icon(
                                    Icons.Filled.ArrowBack,
                                    "",
                                    tint = LIGHT_GREEN
                                )
                            }

                            Spacer(Modifier.weight(1f, true))

                            FloatingActionButton(
                                modifier = Modifier.padding(end = 16.dp),
                                containerColor = Color.Black,
                                contentColor = LIGHT_GREEN, onClick = {}
                            ) {
                                Icon(Icons.Outlined.Share, "")
                            }

                            FloatingActionButton(
                                modifier = Modifier.padding(end = 16.dp),
                                containerColor = Color.Black,
                                contentColor = LIGHT_GREEN,
                                onClick = {
                                    FileUtils.copyFileToInternalStorage(
                                        Uri.parse(currentPath),
                                        context
                                    )
                                }
                            ) {
                                Icon(Icons.Outlined.ArrowDropDown, "")
                            }
                        }
                    }) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues = innerPadding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val clickedIndex = getClickedIndexFromExtras()
                        StatusPreview(FileUtils.statusList, clickedIndex)
                    }
                }
            }
        }
    }

    private fun getClickedIndexFromExtras(): Int {
        return intent.extras?.getInt(Const.CLICKED_INDEX) ?: 0
    }
}

var currentPath = ""

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusPreview(statusList: MutableList<StatusModel>, clickedIndex: Int) {
    val pagerState = rememberPagerState(initialPage = clickedIndex)
    currentPath = statusList[clickedIndex].path
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            pageCount = statusList.size,
            state = pagerState,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            val currentStatus = statusList[index]
            val painter: Any? =
                if (currentStatus.isVideo) currentStatus.thumbnail else currentStatus.imageRequest

            currentPath = statusList[pagerState.currentPage].path
            if (currentStatus.isVideo && pagerState.currentPage == index) {
                val context = LocalContext.current
                val exoPlayer = ExoPlayer.Builder(LocalContext.current).build()
                val mediaItem = MediaItem.fromUri(currentStatus.path)
                exoPlayer.setMediaItem(mediaItem)

                val playerView = StyledPlayerView(context)
                playerView.hideController()
                playerView.controllerAutoShow = false
                playerView.player = exoPlayer

                DisposableEffect(AndroidView(factory = {
                    playerView.apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                })) {
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                    onDispose {
                        exoPlayer.release()
                    }
                }
            } else {
                AsyncImage(
                    model = painter,
                    contentDescription = "",
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


package com.inningsstudio.statussaver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme
import kotlinx.coroutines.launch

class StatusViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StatusSaverTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val clickedIndex = getClickedIndexFromExtras()
                    GreetingPreview(FileUtils.statusList, clickedIndex)
                }
            }
        }
    }

    private fun getClickedIndexFromExtras(): Int {
        return intent.extras?.getInt(Const.CLICKED_INDEX) ?: 0
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GreetingPreview(statusList: MutableList<StatusModel>, clickedIndex: Int) {
    val pagerState = rememberPagerState(initialPage = clickedIndex)
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            pageCount = statusList.size,
            state = pagerState,
            key = { statusList[it].path },
            pageSize = PageSize.Fill
        ) { index ->
            val painter: Any? =
                if (statusList[index].isVideo) statusList[index].thumbnail else statusList[index].imageRequest
            AsyncImage(
                model = painter,
                contentDescription = "",
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .offset(y = -(16).dp)
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(100))
                .padding(8.dp)
                .align(Alignment.BottomCenter)
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage - 1
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Go back"
                )
            }
            IconButton(
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1
                        )
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Go forward"
                )
            }

        }
    }
}
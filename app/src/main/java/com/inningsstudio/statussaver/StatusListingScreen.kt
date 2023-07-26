package com.inningsstudio.statussaver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun StatusListingScreen(statusList: List<StatusModel>, onStatusClick: (Int) -> Unit) {

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3)
        ) {
            items(statusList.size) { index ->
                val modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(2.dp)
                    .clickable {
                        onStatusClick(index)
                    }
                ImageItemView(statusList[index], modifier)
            }
        }
    }
}

@Composable
fun ImageItemView(statusModel: StatusModel, modifier: Modifier) {

    if (statusModel.isVideo) {
        AsyncImage(
            model = statusModel.thumbnail,
            contentDescription = "",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = modifier
        )
    } else {
        AsyncImage(
            model = statusModel.imageRequest,
            contentDescription = "icon",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
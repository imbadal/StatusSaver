package com.inningsstudio.statussaver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun MoreScreen() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {

        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clickable {
                    SharedUtils.privacyPolicy(context)
                },
        ) {
            Text(text = "Privacy Policy", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clickable {
                    SharedUtils.shareApp(context)
                },
        ) {
            Text(text = "Share App", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clickable {
                    SharedUtils.rateUs(context)
                },
        ) {
            Text(text = "Rate Us", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }

        Card(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .clickable {
                    SharedUtils.contactUs(context)
                },
        ) {
            Text(text = "Contact Us", fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }

    }
}
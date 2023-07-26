package com.inningsstudio.statussaver

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults.colors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inningsstudio.statussaver.Const.STATUS_URI
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme
import com.inningsstudio.statussaver.viewmodels.MainViewModel

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            StatusSaverTheme {

                val viewModel = viewModel<MainViewModel>()
                viewModel.fetchStatus(applicationContext, getUriFromExtras())
                viewModel.fetchSavedStatus(applicationContext)

                val navController = rememberNavController()
                Scaffold(bottomBar = {
                    BottomNavigationBar(items = listOf(
                        BottomNavItem(
                            name = "Home", route = "home", icon = Icons.Default.Home
                        ),
                        BottomNavItem(
                            name = "Saved",
                            route = "saved",
                            icon = Icons.Filled.Favorite
                        ),
                        BottomNavItem(
                            name = "More",
                            route = "more",
                            icon = Icons.Default.MoreVert
                        ),
                    ), navController = navController, onItemClick = {
                        navController.navigate(it.route)
                    })
                }) {
                    Navigation(navHostController = navController, viewModel, LocalContext.current)
                }
            }
        }
    }

    private fun getUriFromExtras(): String {
        return intent.extras?.getString(STATUS_URI) ?: ""
    }
}

@Composable
fun Navigation(navHostController: NavHostController, viewModel: MainViewModel, current: Context) {
    NavHost(navController = navHostController, startDestination = "home") {
        composable("home") {
            StatusListingScreen(viewModel.statusList) { clickedIndex ->
                viewModel.onStatusClicked(current, clickedIndex)
            }
        }

        composable("saved") {
            StatusListingScreen(viewModel.savedStatusList) { clickedIndex ->
                viewModel.onStatusClicked(current, clickedIndex, true)
            }
        }

        composable("more") {
            MoreScreen()
        }
    }
}

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    navController: NavController,
    modifier: Modifier = Modifier,
    onItemClick: (BottomNavItem) -> Unit
) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    NavigationBar(
        modifier = modifier, containerColor = Color.Black
    ) {
        items.forEach { item ->
            val isSelected = item.route == backStackEntry.value?.destination?.route
            NavigationBarItem(selected = isSelected,
                onClick = { onItemClick(item) },
                colors = colors(
                    selectedIconColor = LIGHT_GREEN,
                    selectedTextColor = LIGHT_GREEN,
                    indicatorColor = Color(0xFF001010),
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White
                ),
                icon = {
                    Column(horizontalAlignment = CenterHorizontally) {
                        Icon(imageVector = item.icon, contentDescription = item.name)
                        Text(
                            text = item.name,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp)
                        )
                    }
                })
        }
    }
}

package com.inningsstudio.statussaver.presentation.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.inningsstudio.statussaver.core.constants.BottomNavItem
import com.inningsstudio.statussaver.core.constants.LIGHT_GREEN
import com.inningsstudio.statussaver.core.utils.PreferenceUtils
import com.inningsstudio.statussaver.core.utils.StorageAccessHelper
import com.inningsstudio.statussaver.presentation.ui.settings.SettingsScreen
import com.inningsstudio.statussaver.presentation.ui.status.StatusListingScreen
import com.inningsstudio.statussaver.presentation.viewmodel.MainViewModel
import com.inningsstudio.statussaver.presentation.viewmodel.MainViewModelFactory
import com.inningsstudio.statussaver.ui.theme.StatusSaverTheme
import com.inningsstudio.statussaver.data.datasource.StatusLocalDataSource
import com.inningsstudio.statussaver.data.repository.StatusRepositoryImpl
import com.inningsstudio.statussaver.domain.usecase.DetectStatusPathsUseCase
import com.inningsstudio.statussaver.domain.usecase.GetStatusesUseCase

class MainActivity : ComponentActivity() {

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Check if we have folder permissions and onboarding is completed
        val preferenceUtils = PreferenceUtils(application)
        val safUri = preferenceUtils.getUriFromPreference()
        val onboardingCompleted = preferenceUtils.isOnboardingCompleted()
        val hasRequiredPermissions = StorageAccessHelper.hasRequiredPermissions(this)
        
        // Check if we need to request permissions or show onboarding
        if (!hasRequiredPermissions || safUri.isNullOrBlank() || !onboardingCompleted) {
            // If no permissions or no folder access, start onboarding
            startActivity(Intent(this, com.inningsstudio.statussaver.presentation.ui.onboarding.OnBoardingActivity::class.java))
            finish()
            return
        }

        // If we have all required permissions and folder access, proceed to StatusGalleryActivity
        // (as mentioned by user, StatusGalleryActivity is the main activity after permissions)
        startActivity(Intent(this, com.inningsstudio.statussaver.presentation.ui.status.StatusGalleryActivity::class.java))
        finish()
    }
}

@Composable
fun Navigation(navHostController: NavHostController, viewModel: MainViewModel, current: Context) {
    NavHost(navController = navHostController, startDestination = "home") {
        composable("home") {
            StatusListingScreen(viewModel = viewModel)
        }

        composable("saved") {
            StatusListingScreen(viewModel = viewModel, isSaved = true)
        }

        composable("more") {
            SettingsScreen()
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

package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skripsi.chefly.ui.navigation.Screen
import com.skripsi.chefly.ui.screens.*
import com.skripsi.chefly.ui.screens.onboarding.OnboardingScreen
import com.skripsi.chefly.ui.screens.splash.SplashScreen
import com.skripsi.chefly.ui.theme.CheflyTheme
import com.skripsi.chefly.ui.viewmodel.RecipeDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            CheflyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                BottomNavigationBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            // 1. Ganti startDestination ke rute Splash
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- 2. Tambahkan Route Splash Screen ---
            composable("splash") {
                SplashScreen(onTimeout = {
                    // Pindah ke Onboarding setelah splash selesai
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            // --- Halaman Utama ---
            composable(Screen.Beranda.route) {
                HomeScreen(
                    onScanClick = { navController.navigate(Screen.Pindai.route) },
                    onRecipeClick = { id ->
                        navController.navigate(Screen.RecipeDetail.createRoute(id))
                    },
                    onSeeAllClick = {
                        navController.navigate(Screen.Resep.route) {
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Pindai.route) { CameraScanScreen() }
            composable(Screen.Resep.route) { RecipeExploreScreen() }
            composable(Screen.Tersimpan.route) { SavedRecipesScreen() }

            // --- Halaman Detail ---
            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                val viewModel: RecipeDetailViewModel = hiltViewModel()

                RecipeDetailScreen(
                    navController = navController,
                    recipeId = recipeId,
                    viewModel = viewModel
                )
            }

            // --- Onboarding ---
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinish = {
                    navController.navigate(Screen.Beranda.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
    // Gunakan Color(0xFFE36C47) langsung atau panggil dari Theme agar konsisten
    val themeTerracotta = Color(0xFFE36C47)

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("Beranda", Screen.Beranda.route, Icons.Default.Home),
            Triple("Pindai", Screen.Pindai.route, Icons.Default.CenterFocusStrong),
            Triple("Resep", Screen.Resep.route, Icons.Default.RestaurantMenu),
            Triple("Simpan", Screen.Tersimpan.route, Icons.Default.Bookmark)
        )

        items.forEach { (label, route, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                label = { Text(label, fontSize = 10.sp) },
                icon = { Icon(icon, contentDescription = label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = themeTerracotta,
                    selectedTextColor = themeTerracotta,
                    indicatorColor = themeTerracotta.copy(alpha = 0.1f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

private fun shouldShowBottomBar(route: String?): Boolean {
    // Bottom bar TIDAK boleh muncul di DetailScreen atau Onboarding
    return route in listOf(
        Screen.Beranda.route,
        Screen.Pindai.route,
        Screen.Resep.route,
        Screen.Tersimpan.route
    )
}
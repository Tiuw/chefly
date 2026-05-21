package com.skripsi.chefly

import android.net.Uri
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
import com.skripsi.chefly.util.toDatabaseKey
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
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            // --- Splash Screen ---
            composable("splash") {
                SplashScreen(onTimeout = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }

            // --- Home Screen ---
            composable(Screen.Beranda.route) {
                HomeScreen(
                    onScanClick = { navController.navigate(Screen.Pindai.route) },
                    onRecipeClick = { id ->
                        navController.navigate(Screen.RecipeDetail.createRoute(id.toString()))
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

            // --- Camera/Scan Screen ---
            composable(route = Screen.Pindai.route) {
                CameraScreen(
                    onAddMoreClick = {
                        navController.navigate(Screen.TambahBahan.route)
                    },
                    onNavigateToResult = { selectedIngredients ->
                        // 🟢 REVISI: Oper hasil kamera langsung ke query pencarian RecipeScreen
                        val ingredientsCsv = selectedIngredients.joinToString(",")
                        navController.navigate("${Screen.Resep.route}?query=$ingredientsCsv") {
                            popUpTo(Screen.Beranda.route)
                        }
                    }
                )
            }

// --- Add Ingredient Screen ---
            composable(Screen.TambahBahan.route) {
                AddIngredientScreen(
                    onBackClick = { navController.popBackStack() },
                    onNavigateToResult = { selectedIngredients ->
                        // 🟢 REVISI: Balikkan ke RecipeScreen sambil mengirimkan CSV ke parameter kueri
                        val ingredientsCsv = selectedIngredients.joinToString(",")
                        navController.navigate("${Screen.Resep.route}?query=$ingredientsCsv") {
                            // Bersihkan tumpukan backstack agar saat back tidak memutar kembali ke input bahan
                            popUpTo(Screen.Beranda.route) { saveState = false }
                        }
                    }
                )
            }

            // --- All Recipes Screen (Explore) ---
            composable(
                route = "${Screen.Resep.route}?query={query}", // Menerima parameter kueri opsional
                arguments = listOf(navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val argumentQuery = backStackEntry.arguments?.getString("query") ?: ""

                RecipeScreen(
                    initialQuery = argumentQuery, // 🟢 Oper kueri kiriman dari screen sebelumnya ke Compose
                    onRecipeClick = { id ->
                        navController.navigate(Screen.RecipeDetail.createRoute(id))
                    },
                    onScanClick = {
                        navController.navigate(Screen.Pindai.route)
                    }
                )
            }

            // --- Saved Recipes Screen ---
            composable(Screen.Tersimpan.route) {
                SavedScreen(
                    onRecipeClick = { id ->
                        navController.navigate(Screen.RecipeDetail.createRoute(id.toString()))
                    },
                    onAddClick = {
                        navController.navigate(Screen.Resep.route) {
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // --- Recipe Detail Screen ---
            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                val viewModel: RecipeDetailViewModel = hiltViewModel()

                RecipeDetailScreen(
                    // 🟢 REVISI: Alihkan navigasi back agar memicu restoreState halaman resep
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
            val isSelected = currentRoute?.startsWith(route) == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
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
    if (route == null) return false
    return route.startsWith(Screen.Beranda.route) ||
            route.startsWith(Screen.Pindai.route) ||
            route.startsWith(Screen.Resep.route) ||
            route.startsWith(Screen.Tersimpan.route)
}
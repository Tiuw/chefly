package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.skripsi.chefly.ui.viewmodel.AddIngredientViewModel
import com.skripsi.chefly.ui.viewmodel.MainViewModel
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
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()

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
                    if (isOnboardingCompleted != null) {
                        if (isOnboardingCompleted == true) {
                            navController.navigate(Screen.Beranda.route) {
                                popUpTo("splash") { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    }
                })
            }

            // --- Home Screen ---
            composable(Screen.Beranda.route) {
                HomeScreen(
                    onScanClick = {
                        navController.navigate(Screen.Pindai.route) {
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRecipeClick = { id ->
                        navController.navigate(
                            "${Screen.RecipeDetail.route.replace("{recipeId}", id)}?query=&similarity=0"
                        )
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
                        val dbKeys = selectedIngredients.map { it.toDatabaseKey() }
                        val ingredientsCsv = dbKeys.joinToString(",")
                        navController.navigate("${Screen.Rekomendasi.route}?ingredients=$ingredientsCsv")
                    }
                )
            }

            // --- Add Ingredient Screen ---
            composable(Screen.TambahBahan.route) {
                val viewModel: AddIngredientViewModel = hiltViewModel()

                AddIngredientScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToResult = { selectedIngredients ->
                        viewModel.saveToRepository()
                        val ingredientsCsv = selectedIngredients.joinToString(",")
                        navController.navigate("${Screen.Rekomendasi.route}?ingredients=$ingredientsCsv")
                    }
                )
            }

            // --- Recommendation Screen (Khusus Hasil TF-IDF & Cosine Similarity) ---
            composable(
                route = "${Screen.Rekomendasi.route}?ingredients={ingredients}",
                arguments = listOf(navArgument("ingredients") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val ingredients = backStackEntry.arguments?.getString("ingredients") ?: ""
                RecommendationScreen(
                    ingredients = ingredients,
                    onBackClick = { navController.popBackStack() },
                    onAddMoreClick = { navController.navigate(Screen.TambahBahan.route) },
                    onRecipeClick = { id, score ->
                        navController.navigate(
                            "${Screen.RecipeDetail.route.replace("{recipeId}", id)}?query=$ingredients&similarity=$score"
                        )
                    }
                )
            }

            // --- All Recipes Screen (Khusus Pencarian Judul Standar Room DB) ---
            composable(
                route = "${Screen.Resep.route}?query={query}",
                arguments = listOf(navArgument("query") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val argumentQuery = backStackEntry.arguments?.getString("query") ?: ""

                RecipeScreen(
                    initialQuery = argumentQuery,
                    onRecipeClick = { id, score ->
                        navController.navigate(
                            "${Screen.RecipeDetail.route.replace("{recipeId}", id)}?query=$argumentQuery&similarity=$score"
                        )
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
                        navController.navigate(
                            "${Screen.RecipeDetail.route.replace("{recipeId}", id)}?query=&similarity=0"
                        )
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
                route = "${Screen.RecipeDetail.route}?query={query}&similarity={similarity}",
                arguments = listOf(
                    navArgument("recipeId") { type = NavType.StringType },
                    navArgument("query") { type = NavType.StringType; nullable = true; defaultValue = "" },
                    navArgument("similarity") { type = NavType.FloatType; defaultValue = 0f }
                )
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                val query = backStackEntry.arguments?.getString("query") ?: ""
                val similarity = backStackEntry.arguments?.getFloat("similarity") ?: 0f

                val viewModel: RecipeDetailViewModel = hiltViewModel()

                RecipeDetailScreen(
                    navController = navController,
                    recipeId = recipeId,
                    currentQuery = query,
                    passedSimilarity = similarity,
                    viewModel = viewModel
                )
            }

            // --- Onboarding Screen ---
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onFinish = {
                    mainViewModel.completeOnboarding()
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
                label = {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                icon = { Icon(icon, contentDescription = label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = themeTerracotta,
                    selectedTextColor = themeTerracotta,
                    indicatorColor = themeTerracotta.copy(alpha = 0.1f),
                    unselectedIconColor = Color(0xFF5F6368),
                    unselectedTextColor = Color(0xFF5F6368)
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
package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.skripsi.chefly.ui.theme.*
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

    // Menggunakan Box induk agar navbar mengambang bebas di atas NavHost (True Floating)
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.fillMaxSize()
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
                    },
                    onCategoryClick = { category ->
                        navController.navigate("${Screen.Resep.route}?category=$category") {
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
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

            // --- Recommendation Screen ---
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

            // --- All Recipes Screen ---
            composable(
                route = "${Screen.Resep.route}?query={query}&category={category}",
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("category") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val argumentQuery = backStackEntry.arguments?.getString("query") ?: ""
                val argumentCategory = backStackEntry.arguments?.getString("category") ?: ""

                RecipeScreen(
                    initialQuery = argumentQuery,
                    initialCategory = argumentCategory,
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

        // Floating Navbar Overlay (Mengambang di atas konten yang di-scroll)
        if (shouldShowBottomBar(currentRoute)) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                TrueFloatingBottomDock(navController, currentRoute)
            }
        }
    }
}

/**
 * Modern Clean 4-Tab Floating Dock
 */
@Composable
fun TrueFloatingBottomDock(navController: NavHostController, currentRoute: String?) {
    val navAction: (String) -> Unit = { route ->
        if (currentRoute?.startsWith(route) != true) {
            navController.navigate(route) {
                popUpTo(Screen.Beranda.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = PureSurface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, WhisperBorder),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Beranda
            DockNavItem(
                label = "Beranda",
                selectedIcon = Icons.Default.Home,
                unselectedIcon = Icons.Outlined.Home,
                isSelected = currentRoute?.startsWith(Screen.Beranda.route) == true,
                onClick = { navAction(Screen.Beranda.route) },
                modifier = Modifier.weight(1f)
            )

            // 2. Pindai (YOLO26 Camera)
            DockNavItem(
                label = "Pindai",
                selectedIcon = Icons.Default.CenterFocusStrong,
                unselectedIcon = Icons.Outlined.CenterFocusStrong,
                isSelected = currentRoute?.startsWith(Screen.Pindai.route) == true,
                onClick = { navAction(Screen.Pindai.route) },
                modifier = Modifier.weight(1f)
            )

            // 3. Resep
            DockNavItem(
                label = "Resep",
                selectedIcon = Icons.Default.RestaurantMenu,
                unselectedIcon = Icons.Outlined.RestaurantMenu,
                isSelected = currentRoute?.startsWith(Screen.Resep.route) == true,
                onClick = { navAction(Screen.Resep.route) },
                modifier = Modifier.weight(1f)
            )

            // 4. Simpan
            DockNavItem(
                label = "Simpan",
                selectedIcon = Icons.Default.Bookmark,
                unselectedIcon = Icons.Outlined.BookmarkBorder,
                isSelected = currentRoute?.startsWith(Screen.Tersimpan.route) == true,
                onClick = { navAction(Screen.Tersimpan.route) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Item Tab dengan Capsule Indicator Ringan
 */
@Composable
fun DockNavItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) Terracotta else SecondaryText,
        animationSpec = tween(durationMillis = 180),
        label = "navColorAnim"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScaleAnim"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) Terracotta.copy(alpha = 0.12f) else Color.Transparent
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = animatedColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = animatedColor
        )
    }
}

private fun shouldShowBottomBar(route: String?): Boolean {
    if (route == null) return false
    return route.startsWith(Screen.Beranda.route) ||
            route.startsWith(Screen.Pindai.route) ||
            route.startsWith(Screen.Resep.route) ||
            route.startsWith(Screen.Tersimpan.route)
}
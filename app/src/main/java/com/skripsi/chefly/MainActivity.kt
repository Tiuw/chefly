package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skripsi.chefly.ui.RecipeViewModel
import com.skripsi.chefly.ui.navigation.Screen
import com.skripsi.chefly.ui.screens.CameraScreen
import com.skripsi.chefly.ui.screens.FavoritesScreen
import com.skripsi.chefly.ui.screens.HomeScreen
import com.skripsi.chefly.ui.screens.ProfileScreen
import com.skripsi.chefly.ui.screens.RecipeDetailScreen
import com.skripsi.chefly.ui.theme.CheflyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheflyTheme {
                CheflyApp()
            }
        }
    }
}

@Composable
fun CheflyApp() {
    val navController = rememberNavController()
    val viewModel: RecipeViewModel = viewModel()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = {
                        currentDestination = it
                        when (it) {
                            AppDestinations.HOME -> navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                            AppDestinations.CAMERA -> navController.navigate(Screen.Camera.route) {
                                popUpTo(Screen.Home.route)
                            }
                            AppDestinations.FAVORITES -> navController.navigate(Screen.Favorites.route) {
                                popUpTo(Screen.Home.route)
                            }
                            AppDestinations.PROFILE -> navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    currentDestination = AppDestinations.HOME
                    HomeScreen(
                        viewModel = viewModel,
                        onRecipeClick = { recipeId ->
                            navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                        }
                    )
                }

                composable(Screen.Camera.route) {
                    currentDestination = AppDestinations.CAMERA
                    CameraScreen(
                        viewModel = viewModel,
                        onSearchRecipes = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                            currentDestination = AppDestinations.HOME
                        }
                    )
                }

                composable(Screen.Favorites.route) {
                    currentDestination = AppDestinations.FAVORITES
                    FavoritesScreen(
                        viewModel = viewModel,
                        onRecipeClick = { recipeId ->
                            navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                        }
                    )
                }

                composable(Screen.Profile.route) {
                    currentDestination = AppDestinations.PROFILE
                    ProfileScreen()
                }

                composable(
                    route = Screen.RecipeDetail.route,
                    arguments = listOf(navArgument("recipeId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val recipeId = backStackEntry.arguments?.getInt("recipeId") ?: 0
                    RecipeDetailScreen(
                        recipeId = recipeId,
                        viewModel = viewModel,
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    CAMERA("Camera", Icons.Default.Camera),
    FAVORITES("Favorites", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

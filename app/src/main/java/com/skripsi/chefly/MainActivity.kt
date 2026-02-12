package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.skripsi.chefly.ui.screens.FridgeScreen
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(80.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home
                    NavigationItem(
                        icon = Icons.Default.Home,
                        label = "Home",
                        selected = currentDestination == AppDestinations.HOME,
                        onClick = {
                            currentDestination = AppDestinations.HOME
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )

                    // Fridge
                    NavigationItem(
                        icon = Icons.Default.Kitchen,
                        label = "My Fridge",
                        selected = currentDestination == AppDestinations.FRIDGE,
                        onClick = {
                            currentDestination = AppDestinations.FRIDGE
                            navController.navigate(Screen.Fridge.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )

                    // Camera FAB (centered, elevated)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .offset(y = (-16).dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                currentDestination = AppDestinations.CAMERA
                                navController.navigate(Screen.Camera.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(
                                    elevation = 12.dp,
                                    shape = CircleShape,
                                    ambientColor = MaterialTheme.colorScheme.primary,
                                    spotColor = MaterialTheme.colorScheme.primary
                                ),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = "Scan Ingredients",
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "Scan",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    // Favorites
                    NavigationItem(
                        icon = Icons.Default.Favorite,
                        label = "Favorites",
                        selected = currentDestination == AppDestinations.FAVORITES,
                        onClick = {
                            currentDestination = AppDestinations.FAVORITES
                            navController.navigate(Screen.Favorites.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )

                    // Profile
                    NavigationItem(
                        icon = Icons.Default.AccountBox,
                        label = "Profile",
                        selected = currentDestination == AppDestinations.PROFILE,
                        onClick = {
                            currentDestination = AppDestinations.PROFILE
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
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

            composable(Screen.Fridge.route) {
                currentDestination = AppDestinations.FRIDGE
                FridgeScreen(viewModel = viewModel)
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

@Composable
fun NavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

enum class AppDestinations {
    HOME,
    FRIDGE,
    CAMERA,
    FAVORITES,
    PROFILE
}

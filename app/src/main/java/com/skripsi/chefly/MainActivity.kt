package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skripsi.chefly.ui.viewmodel.SharedViewModel
import com.skripsi.chefly.ui.navigation.Screen
import com.skripsi.chefly.ui.screens.CameraScreen
import com.skripsi.chefly.ui.screens.FridgeScreen
import com.skripsi.chefly.ui.screens.HomeScreen
import com.skripsi.chefly.ui.screens.RecipeDetailScreen
import com.skripsi.chefly.ui.theme.CheflyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
    val sharedViewModel: SharedViewModel = viewModel()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Bauhaus Bottom Navigation Bar
            BottomAppBar(
                modifier = Modifier
                    .height(72.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home
                    BauhausNavItem(
                        icon = Icons.Default.Home,
                        label = "HOME",
                        selected = currentDestination == AppDestinations.HOME,
                        onClick = {
                            currentDestination = AppDestinations.HOME
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )

                    // Favorites
                    BauhausNavItem(
                        icon = Icons.Default.Favorite,
                        label = "SAVED",
                        selected = currentDestination == AppDestinations.FAVORITES,
                        onClick = {
                            currentDestination = AppDestinations.FAVORITES
                            navController.navigate(Screen.Favorites.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )

                    // Camera FAB (centered, elevated)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .offset(y = (-8).dp)
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
                                .border(3.dp, MaterialTheme.colorScheme.primary),
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Scan",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Fridge
                    BauhausNavItem(
                        icon = Icons.Default.Kitchen,
                        label = "FRIDGE",
                        selected = currentDestination == AppDestinations.FRIDGE,
                        onClick = {
                            currentDestination = AppDestinations.FRIDGE
                            navController.navigate(Screen.Fridge.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )

                    // Profile
                    BauhausNavItem(
                        icon = Icons.Default.AccountBox,
                        label = "PROFILE",
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
                    sharedViewModel = sharedViewModel,
                    onRecipeClick = { recipeId ->
                        navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                    }
                )
            }

            composable(Screen.Fridge.route) {
                currentDestination = AppDestinations.FRIDGE
                FridgeScreen(
                    sharedViewModel = sharedViewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                        currentDestination = AppDestinations.HOME
                    }
                )
            }

            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""
                RecipeDetailScreen(
                    recipeId = recipeId,
                    sharedViewModel = sharedViewModel,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun BauhausNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            fontSize = androidx.compose.ui.unit.TextUnit(10f, androidx.compose.ui.unit.TextUnitType.Sp)
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

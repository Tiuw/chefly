package com.skripsi.chefly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skripsi.chefly.ui.navigation.Screen
import com.skripsi.chefly.ui.screens.* // Pastikan semua screen diimport
import com.skripsi.chefly.ui.theme.CheflyTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.skripsi.chefly.ui.viewmodel.RecipeDetailViewModel

// Warna sesuai palette desainmu
val Terracotta = Color(0xFFE36C47)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CheflyTheme { // Ganti dengan nama theme projectmu
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    // Memantau route yang sedang aktif secara real-time
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Navbar hanya muncul di halaman utama (Beranda, Pindai, Resep, Tersimpan)
            if (shouldShowBottomBar(currentRoute)) {
                BottomNavigationBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Beranda.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Beranda.route) {
                HomeScreen(
                    onScanClick = { navController.navigate(Screen.Pindai.route) },
                    onRecipeClick = { id ->
                        navController.navigate(Screen.RecipeDetail.createRoute(id))
                    },
                    // FIX: Navigasi Lihat Semua agar Navbar ikut berubah
                    onSeeAllClick = {
                        navController.navigate(Screen.Resep.route) {
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Pindai.route) { CameraScanScreen() } // Ganti dengan screenmu
            composable(Screen.Resep.route) { RecipeExploreScreen() } // Ganti dengan screenmu
            composable(Screen.Tersimpan.route) { SavedRecipesScreen() } // Ganti dengan screenmu

            composable(
                route = Screen.RecipeDetail.route,
                arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
            ) { backStackEntry ->
                // 1. Ambil recipeId dari arguments navigasi
                val recipeId = backStackEntry.arguments?.getString("recipeId") ?: ""

                // 2. Inisialisasi ViewModel menggunakan Hilt
                val viewModel: RecipeDetailViewModel = hiltViewModel()

                // 3. Masukkan ke dalam fungsi Screen
                RecipeDetailScreen(
                    navController = navController,
                    recipeId = recipeId,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String?) {
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
                            // Menghindari penumpukan halaman di stack
                            popUpTo(Screen.Beranda.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                label = { Text(label, fontSize = 10.sp) },
                icon = { Icon(icon, contentDescription = label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Terracotta,
                    selectedTextColor = Terracotta,
                    indicatorColor = Terracotta.copy(alpha = 0.1f)
                )
            )
        }
    }
}

// Fungsi bantu untuk cek apakah Navbar harus muncul
private fun shouldShowBottomBar(route: String?): Boolean {
    return route in listOf(
        Screen.Beranda.route,
        Screen.Pindai.route,
        Screen.Resep.route,
        Screen.Tersimpan.route
    )
}
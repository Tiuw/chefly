package com.skripsi.chefly.ui.navigation

sealed class Screen(val route: String) {
    object Beranda : Screen("beranda")
    object Pindai : Screen("pindai")
    object Resep : Screen("resep")
    object Tersimpan : Screen("tersimpan")
    object RecipeDetail : Screen("recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe/$recipeId"
    }
}

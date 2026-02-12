package com.skripsi.chefly.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Fridge : Screen("fridge")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
    object RecipeDetail : Screen("recipe/{recipeId}") {
        fun createRoute(recipeId: Int) = "recipe/$recipeId"
    }
}


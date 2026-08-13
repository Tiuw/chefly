package com.skripsi.chefly.ui.navigation

sealed class Screen(val route: String) {
    object Beranda : Screen("beranda")
    object Pindai : Screen("pindai")
    object Resep : Screen("resep")
    object Tersimpan : Screen("tersimpan")
    object TambahBahan : Screen("tambah-bahan")
    object Rekomendasi : Screen("rekomendasi")

    // TAMBAHKAN INI
    object Onboarding : Screen("onboarding")

    object RecipeDetail : Screen("recipe/{recipeId}") {
        fun createRoute(recipeId: String) = "recipe/$recipeId"
    }
}

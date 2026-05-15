package com.skripsi.chefly.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.skripsi.chefly.data.local.RecipeDao
import com.skripsi.chefly.ui.theme.SoftSage
import com.skripsi.chefly.ui.theme.Terracotta
import com.skripsi.chefly.ui.viewmodel.IngredientGroup
import androidx.compose.ui.graphics.Color
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    suspend fun getCategorizedIngredients(): List<IngredientGroup> {
        // 1. Ganti ke getAllOnceList() sesuai nama di RecipeDao
        val allRecipes = recipeDao.getAllOnceList()

        // 2. Ekstrak bahan unik
        val allIngredients = allRecipes.flatMap { recipe ->
            recipe.uiIngredients?.split(",")?.map { item ->
                item.trim().lowercase()
            } ?: emptyList()
        }.distinct().filter { it.isNotEmpty() }

        // 3. Mapping Kategori
        val categories = listOf(
            Triple("Protein", Icons.Default.Egg, Color(0xFFA03B1A)),
            Triple("Bumbu & Cabe", Icons.Default.Restaurant, Terracotta),
            Triple("Rempah", Icons.Default.Grain, Color(0xFF8B4513)),
            Triple("Sayuran", Icons.Default.SoupKitchen, SoftSage),
            Triple("Dedaunan", Icons.Default.Eco, Color(0xFF4CAF50)),
            Triple("Tepung & Lainnya", Icons.Default.Kitchen, Color(0xFF94A3B8))
        )

        val keywords = mapOf(
            "Protein" to listOf("daging", "ayam", "sapi", "kambing", "ikan", "udang", "telur", "tempe", "tahu"),
            "Bumbu & Cabe" to listOf("bawang", "cabe", "sambal", "kemiri", "terasi", "jahe", "kunyit", "lengkuas", "serai"),
            "Rempah" to listOf("ketumbar", "merica", "pala", "kapulaga", "kayu_manis", "bunga_lawang", "penyedap", "gula_merah", "asam_jawa"),
            "Sayuran" to listOf("tomat", "kubis", "wortel", "kentang", "kacang", "kangkung", "seledri"),
            "Dedaunan" to listOf("daun", "nipis", "pandan"),
            "Tepung & Lainnya" to listOf("tepung", "minyak", "kecap", "santan", "kelapa")
        )

        // 4. Proses pengelompokan (Fixing 'it' and 'ingredient' issues)
        return categories.map { categoryTriple ->
            val categoryName = categoryTriple.first
            val icon = categoryTriple.second
            val color = categoryTriple.third

            val filterKeys = keywords[categoryName] ?: emptyList()

            val filteredIngredients = allIngredients.filter { ingredient: String ->
                filterKeys.any { key -> ingredient.contains(key, ignoreCase = true) }
            }.map { text ->
                text.replace("_", " ").replaceFirstChar { it.uppercase() }
            }

            IngredientGroup(
                categoryName = categoryName,
                icon = icon,
                color = color,
                ingredients = filteredIngredients
            )
        }.filter { it.ingredients.isNotEmpty() }
    }
}
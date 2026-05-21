package com.skripsi.chefly.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.skripsi.chefly.data.local.RecipeDao
import com.skripsi.chefly.ui.theme.SoftSage
import com.skripsi.chefly.ui.theme.Terracotta
import com.skripsi.chefly.ui.viewmodel.IngredientGroup
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor(
    private val recipeDao: RecipeDao
) {
    // 🟢 TAMBAHAN SKRIPSI: StateFlow global di level Repository untuk menyimpan cache bahan dari YOLO26
    private val _detectedIngredientsFromCamera = MutableStateFlow<Set<String>>(emptySet())
    val detectedIngredientsFromCamera: StateFlow<Set<String>> = _detectedIngredientsFromCamera.asStateFlow()

    /**
     * Fungsi untuk menyimpan daftar bahan yang berhasil dideteksi oleh YOLO26
     */
    fun saveDetectedIngredients(ingredients: List<String>) {
        // Simpan dalam format lowercase dan hilangkan spasi kosong agar pencocokan string akurat
        _detectedIngredientsFromCamera.value = ingredients.map { it.trim().lowercase() }.toSet()
        android.util.Log.d("Chefly_Repo", "💾 Menyimpan cache deteksi YOLO26: ${_detectedIngredientsFromCamera.value}")
    }

    /**
     * Fungsi untuk membersihkan cache peninggalan kamera setelah diambil oleh AddIngredientViewModel
     */
    fun clearDetectedIngredients() {
        _detectedIngredientsFromCamera.value = emptySet()
        android.util.Log.d("Chefly_Repo", "🧹 Cache deteksi YOLO26 dibersihkan.")
    }

    suspend fun getCategorizedIngredients(): List<IngredientGroup> {
        // 1. Ambil seluruh data dari database lokal (Room) via DAO
        val allRecipes = recipeDao.getAllOnceList()

        // 2. Ekstrak bahan unik dan bersihkan string-nya
        val allIngredients = allRecipes.flatMap { recipe ->
            recipe.uiIngredients?.split(",")?.map { item ->
                item.trim().lowercase()
            } ?: emptyList()
        }.distinct().filter { it.isNotEmpty() }

        // 3. Mapping Kategori Visual
        val categories = listOf(
            Triple("Protein", Icons.Default.Egg, Color(0xFFA03B1A)),
            Triple("Bumbu & Cabe", Icons.Default.Restaurant, Terracotta),
            Triple("Rempah", Icons.Default.Grain, Color(0xFF8B4513)),
            Triple("Sayuran", Icons.Default.SoupKitchen, SoftSage),
            Triple("Dedaunan", Icons.Default.Eco, Color(0xFF4CAF50)),
            Triple("Tepung & Lainnya", Icons.Default.Kitchen, Color(0xFF94A3B8))
        )

        // Kamus Kata Kunci (Keywords) untuk filter kelompok bahan lokal
        val keywords = mapOf(
            "Protein" to listOf("daging", "ayam", "sapi", "kambing", "ikan", "udang", "telur", "tempe", "tahu"),
            "Bumbu & Cabe" to listOf("bawang", "cabe", "cabai", "sambal", "kemiri", "terasi", "jahe", "kunyit", "lengkuas", "serai"),
            "Rempah" to listOf("ketumbar", "merica", "pala", "kapulaga", "kayu_manis", "bunga_lawang", "penyedap", "gula_merah", "asam_jawa"),
            "Sayuran" to listOf("tomat", "kubis", "wortel", "kentang", "kacang", "kangkung", "seledri"),
            "Dedaunan" to listOf("daun", "nipis", "pandan"),
            "Tepung & Lainnya" to listOf("tepung", "minyak", "kecap", "santan", "kelapa")
        )

        // 4. Proses pengelompokan berbasis Keyword-Matching
        return categories.map { categoryTriple ->
            val categoryName = categoryTriple.first
            val icon = categoryTriple.second
            val color = categoryTriple.third

            val filterKeys = keywords[categoryName] ?: emptyList()

            val filteredIngredients = allIngredients.filter { ingredient: String ->
                filterKeys.any { key -> ingredient.contains(key, ignoreCase = true) }
            }.map { text ->
                // Rapikan teks untuk tampilan UI (Ubah "_" jadi spasi dan buat Huruf Kapital di awal kata)
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
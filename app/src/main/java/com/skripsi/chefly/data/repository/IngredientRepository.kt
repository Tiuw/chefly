package com.skripsi.chefly.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import com.skripsi.chefly.ui.theme.SoftSage
import com.skripsi.chefly.ui.theme.Terracotta
import com.skripsi.chefly.ui.viewmodel.IngredientGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IngredientRepository @Inject constructor() {

    // State untuk menampung hasil deteksi kamera (Fitur YOLO)
    private val _detectedIngredientsFromCamera = MutableStateFlow<Set<String>>(emptySet())
    val detectedIngredientsFromCamera: StateFlow<Set<String>> = _detectedIngredientsFromCamera.asStateFlow()

    // State untuk menampung daftar bahan yang sedang aktif pada sistem rekomendasi TF-IDF & Cosine Similarity
    private val _currentRecommendationIngredients = MutableStateFlow<Set<String>>(emptySet())
    val currentRecommendationIngredients: StateFlow<Set<String>> = _currentRecommendationIngredients.asStateFlow()

    fun saveDetectedIngredients(ingredients: List<String>) {
        _detectedIngredientsFromCamera.value = ingredients.map { it.trim().lowercase() }.toSet()
    }

    fun clearDetectedIngredients() {
        _detectedIngredientsFromCamera.value = emptySet()
    }

    fun setCurrentRecommendationIngredients(ingredients: List<String>) {
        _currentRecommendationIngredients.value = ingredients.map { it.trim() }.toSet()
    }

    fun removeRecommendationIngredient(ingredient: String) {
        _currentRecommendationIngredients.value = _currentRecommendationIngredients.value - ingredient
    }

    /**
     * MENGAMBIL DATA LANGSUNG DARI LIST STATIS (INSTANT LOAD)
     */
    fun getCategorizedIngredients(): List<IngredientGroup> {

        // 1. Definisikan Keywords kelompok bahan pangan
        val keywords = mapOf(
            "Protein" to listOf(
                "Daging",
                "Ayam",
                "Sapi",
                "Kambing",
                "Ikan",
                "Udang",
                "Telur",
                "Tempe",
                "Tahu",
                "Bakso",
                "Sosis"
            ),
            "Bumbu & Cabe" to listOf(
                "Bawang",
                "Cabe",
                "Cabai",
                "Sambal",
                "Kemiri",
                "Terasi",
                "Jahe",
                "Kunyit",
                "Lengkuas",
                "Serai",
                "Garam",
                "Gula"
            ),
            "Rempah" to listOf(
                "Ketumbar",
                "Merica",
                "Lada",
                "Pala",
                "Kapulaga",
                "Kayu manis",
                "Cengkeh",
                "Jinten",
                "Asam jawa"
            ),
            "Sayuran" to listOf(
                "Tomat",
                "Kubis",
                "Kol",
                "Wortel",
                "Kentang",
                "Kacang",
                "Kangkung",
                "Seledri",
                "Sawi",
                "Bayam",
                "Jagung"
            ),
            "Dedaunan" to listOf("Daun", "Nipis", "Pandan", "Kemangi"),
            "Tepung & Lainnya" to listOf(
                "Tepung",
                "Minyak",
                "Mentega",
                "Santan",
                "Kelapa",
                "Susu",
                "Keju",
                "Mie",
                "Pasta"
            )
        )

        // 2. Mapping ke UI Model dengan Ikon dan Warna
        val categories = listOf(
            Triple("Protein", Icons.Default.Egg, Color(0xFFA03B1A)),
            Triple("Bumbu & Cabe", Icons.Default.Restaurant, Terracotta),
            Triple("Rempah", Icons.Default.Grain, Color(0xFF8B4513)),
            Triple("Sayuran", Icons.Default.SoupKitchen, SoftSage),
            Triple("Dedaunan", Icons.Default.Eco, Color(0xFF4CAF50)),
            Triple("Tepung & Lainnya", Icons.Default.Kitchen, Color(0xFF94A3B8))
        )

        // 3. Langsung kembalikan list tanpa memproses database resep
        return categories.map { (name, icon, color) ->
            IngredientGroup(
                categoryName = name,
                icon = icon,
                color = color,
                ingredients = keywords[name] ?: emptyList()
            )
        }
    }
}
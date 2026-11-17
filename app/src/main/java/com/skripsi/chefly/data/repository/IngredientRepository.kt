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

        // 1. Definisikan Keywords kelompok bahan pangan lengkap
        val keywords = mapOf(
            "Protein & Hewani" to listOf(
                "Daging sapi",
                "Daging ayam",
                "Daging kambing",
                "Ikan",
                "Udang",
                "Telur"
            ),
            "Cabai & Bawang" to listOf(
                "Cabe merah",
                "Cabe hijau",
                "Cabe rawit",
                "Bawang merah",
                "Bawang putih",
                "Bawang bombay",
                "Daun bawang"
            ),
            "Daun & Rempah" to listOf(
                "Daun jeruk",
                "Daun salam",
                "Daun kemangi",
                "Daun pandan",
                "Daun seledri",
                "Daun kunyit",
                "Daun pisang",
                "Daun singkong",
                "Daun pepaya",
                "Pala",
                "Asam jawa",
                "Bunga lawang",
                "Kapulaga",
                "Kayu manis",
                "Ketumbar",
                "Merica",
                "Jahe",
                "Kunyit",
                "Laos",
                "Serai",
                "Kemiri",
                "Terasi",
                "Jeruk nipis",
                "Kencur",
                "Lengkuas"
            ),
            "Penyedap, Gula & Saus" to listOf(
                "Penyedap",
                "Gula merah",
                "Gula pasir",
                "Garam",
                "Kecap manis",
                "Kecap asin",
                "Saus tiram",
                "Saus sambal",
                "Saus tomat",
                "Santan",
                "Kelapa parut",
                "Mentega",
                "Margarin"
            ),
            "Sayur & Jamur" to listOf(
                "Tahu",
                "Tempe",
                "Jamur",
                "Kentang",
                "Wortel",
                "Kubis",
                "Kol",
                "Bayam",
                "Kangkung",
                "Tomat",
                "Jagung",
                "Sawi",
                "Terong",
                "Kacang tanah",
                "Kacang panjang",
                "Kacang hijau",
                "Kacang merah",
                "Brokoli"
            ),
            "Tepung & Karbohidrat" to listOf(
                "Nasi",
                "Keju",
                "Susu",
                "Tepung terigu",
                "Tepung beras",
                "Tepung tapioka",
                "Tepung maizena",
                "Tepung panir",
                "Tepung bumbu",
                "Makaroni",
                "Bihun",
                "Mie",
                "Roti",
                "Sosis"
            )
        )

        // 2. Mapping ke UI Model dengan Ikon dan Warna yang Representatif
        val categories = listOf(
            Triple("Protein & Hewani", Icons.Default.Egg, Color(0xFFA03B1A)),
            Triple("Cabai & Bawang", Icons.Default.LocalFireDepartment, Terracotta),
            Triple("Sayur & Jamur", Icons.Default.SoupKitchen, SoftSage),
            Triple("Daun & Rempah", Icons.Default.Eco, Color(0xFF4CAF50)),
            Triple("Penyedap, Gula & Saus", Icons.Default.Kitchen, Color(0xFF8B4513)),
            Triple("Tepung & Karbohidrat", Icons.Default.BakeryDining, Color(0xFF94A3B8))
        )

        // 3. Langsung kembalikan list grup bahan
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
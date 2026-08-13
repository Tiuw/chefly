package com.skripsi.chefly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.IngredientRepository
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.util.FavoriteManager
import com.skripsi.chefly.util.RecipeRecommendationSystem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendationUIState(
    val isLoading: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val ingredientsQuery: String = "",
    val ingredients: List<String> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class RecommendationViewModel @Inject constructor(
    private val repository: RecipeRepository,
    private val ingredientRepository: IngredientRepository, // Inject IngredientRepository
    private val recommendationSystem: RecipeRecommendationSystem,
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val favoriteManager = FavoriteManager(context)

    private val _uiState = MutableStateFlow(RecommendationUIState())
    val uiState: StateFlow<RecommendationUIState> = _uiState.asStateFlow()

    init {
        observeFavorites()
        observeIngredients()
    }

    /**
     * Memantau perubahan daftar bahan secara reactive dari IngredientRepository.
     * Jika ada bahan yang ditambah/dihapus, kalkulasi Cosine Similarity dipanggil otomatis.
     */
    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.currentRecommendationIngredients.collect { ingredientsSet ->
                val list = ingredientsSet.toList()
                _uiState.update { it.copy(ingredients = list) }
                if (list.isNotEmpty()) {
                    getRecommendations(list.joinToString(","))
                }
            }
        }
    }

    /**
     * Memastikan status bookmark/favorit tetap ter-update secara real-time.
     */
    private fun observeFavorites() {
        viewModelScope.launch {
            favoriteManager.favoriteIds.collect { savedIds ->
                _uiState.update { state ->
                    state.copy(
                        recipes = state.recipes.map { recipe ->
                            recipe.copy(isFavorite = savedIds.contains(recipe.id))
                        }
                    )
                }
            }
        }
    }

    /**
     * Menghitung perangkingan Cosine Similarity berdasarkan input bahan CSV.
     */
    fun getRecommendations(csvIngredients: String) {
        if (csvIngredients.isBlank()) return

        // PREVENT RE-COMPUTATION: Abaikan jika query sama dan hasil resep sudah dimuat
        val currentState = _uiState.value
        if (currentState.ingredientsQuery == csvIngredients && currentState.recipes.isNotEmpty()) {
            return
        }

        // Sinkronkan state repository agar komponen UI lain yang mengamati repository tetap konsisten
        val ingredientsList = csvIngredients.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (ingredientsList.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, recipes = emptyList()) }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                ingredientsQuery = csvIngredients,
                ingredients = ingredientsList
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 1. Hitung Perangkingan Cosine Similarity
                val aiRecommendations = recommendationSystem.getRecommendations(ingredientsList)
                val currentFavorites = favoriteManager.favoriteIds.first()

                // 2. Ambil data resep lengkap dari Room DB & pasang skor kemiripan
                val finalResults = aiRecommendations.mapNotNull { result ->
                    val fullRecipe = repository.getRecipeById(context, result.recipeId.toString().trim())
                    fullRecipe?.copy(
                        isFavorite = currentFavorites.contains(fullRecipe.id),
                        similarity = result.similarityScore
                    )
                }

                _uiState.update {
                    it.copy(
                        recipes = finalResults,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat rekomendasi: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Fungsi opsional untuk menghapus bahan dari daftar aktif secara langsung.
     */
    fun removeIngredient(ingredient: String) {
        ingredientRepository.removeRecommendationIngredient(ingredient)
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipe.id)
        }
    }
}
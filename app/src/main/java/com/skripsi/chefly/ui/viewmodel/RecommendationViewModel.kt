package com.skripsi.chefly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.IngredientRepository
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.util.FavoriteManager
import com.skripsi.chefly.util.RecipeRecommendationSystem
import com.skripsi.chefly.util.toDatabaseKey
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
    private val ingredientRepository: IngredientRepository,
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

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.currentRecommendationIngredients.collect { ingredientsSet ->
                val list = ingredientsSet.toList()
                if (list.isNotEmpty()) {
                    getRecommendations(list.joinToString(","))
                } else {
                    _uiState.update {
                        it.copy(
                            ingredients = emptyList(),
                            recipes = emptyList(),
                            ingredientsQuery = "",
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

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

    fun getRecommendations(csvIngredients: String) {
        if (csvIngredients.isBlank()) return

        val ingredientsList = csvIngredients.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val dbIngredients = ingredientsList.map { it.toDatabaseKey() }
        val dbCsvForQuery = dbIngredients.joinToString(",")

        val currentState = _uiState.value
        if (currentState.ingredientsQuery == dbCsvForQuery && currentState.recipes.isNotEmpty()) {
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                ingredientsQuery = dbCsvForQuery,
                ingredients = ingredientsList
            )
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val aiRecommendations = recommendationSystem.getRecommendations(dbIngredients)
                val currentFavorites = favoriteManager.favoriteIds.first()

                val finalResults = aiRecommendations.mapNotNull { result ->
                    val fullRecipe = repository.getRecipeById(context, result.recipeId.toString().trim())
                    fullRecipe?.copy(
                        isFavorite = currentFavorites.contains(fullRecipe.id),
                        similarity = result.similarityScore
                    )
                }.take(10) // 🟢 Menampilkan max 10 resep teratas

                _uiState.update {
                    it.copy(
                        recipes = finalResults,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal memuat rekomendasi"
                    )
                }
            }
        }
    }

    fun removeIngredient(ingredient: String) {
        ingredientRepository.removeRecommendationIngredient(ingredient)
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipe.id)
        }
    }
}
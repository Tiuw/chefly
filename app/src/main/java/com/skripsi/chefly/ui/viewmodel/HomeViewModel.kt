package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.util.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeUiModel(
    val id: String,
    val title: String,
    val imageUrl: String,
    val loves: Int,
    val isFavorite: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: RecipeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val favoriteManager = FavoriteManager(context)

    private val _suggestedRecipes = MutableStateFlow<List<RecipeUiModel>>(emptyList())

    // Gabungkan state resep mentah dengan Flow favoriteIds agar UI selalu sinkron
    val suggestedRecipes: StateFlow<List<RecipeUiModel>> = combine(
        _suggestedRecipes,
        favoriteManager.favoriteIds
    ) { recipes, favIds ->
        recipes.map { it.copy(isFavorite = favIds.contains(it.id)) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.init(context)

                val total = repository.getRecipeCount(context)
                Log.d("DEBUG_CHEF", "Total data di DB: $total")

                val recipes = repository.getRecommendedRecipes(context, limit = 10)
                Log.d("DEBUG_CHEF", "Data yang didapat: ${recipes.size}")

                _suggestedRecipes.value = recipes.map { recipe ->
                    RecipeUiModel(
                        id = recipe.id,
                        title = recipe.name,
                        imageUrl = recipe.imageUrl,
                        loves = recipe.loves ?: 0
                    )
                }
            } catch (e: Exception) {
                Log.e("DEBUG_CHEF", "Error: ${e.message}", e)
                _suggestedRecipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(recipeId: String) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipeId)
        }
    }
}
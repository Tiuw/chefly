package com.skripsi.chefly.ui.viewmodel

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.util.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryData(
    val name: String,
    val icon: ImageVector,
    val isActive: Boolean = false
)

data class RecipeUIState(
    val isLoading: Boolean = false,
    val isLoadMore: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val selectedCategory: String = "Semua",
    val searchQuery: String = "",
    val isEndReached: Boolean = false,
    val currentPage: Int = 0
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val repository: RecipeRepository,
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val favoriteManager = FavoriteManager(context)

    private val _uiState = MutableStateFlow(RecipeUIState())
    val uiState: StateFlow<RecipeUIState> = _uiState.asStateFlow()

    private val _searchQueryInternal = MutableStateFlow("")

    init {
        initializeCategories()
        fetchFilteredRecipes()
        observeFavorites()

        // Debounce agar query SQL tidak dipanggil beruntun saat mengetik cepat
        viewModelScope.launch {
            _searchQueryInternal
                .debounce(300)
                .distinctUntilChanged()
                .collect { performSearch() }
        }
    }

    private fun initializeCategories() {
        val defaultCategories = listOf(
            CategoryData("Semua", Icons.Default.AllInclusive, true),
            CategoryData("Ayam", Icons.Default.Restaurant),
            CategoryData("Daging", Icons.Default.SetMeal),
            CategoryData("Ikan", Icons.Default.Sailing),
            CategoryData("Sayur", Icons.Default.Eco),
            CategoryData("Sambal", Icons.Default.Whatshot)
        )
        _uiState.update { it.copy(categories = defaultCategories) }
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

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                recipes = emptyList(),
                currentPage = 0,
                isEndReached = false
            )
        }
        _searchQueryInternal.value = query
    }

    fun onCategorySelected(categoryName: String) {
        val updatedCategories = _uiState.value.categories.map {
            it.copy(isActive = it.name == categoryName)
        }
        _uiState.update {
            it.copy(
                selectedCategory = categoryName,
                categories = updatedCategories,
                searchQuery = "",
                recipes = emptyList(),
                currentPage = 0,
                isEndReached = false
            )
        }
        _searchQueryInternal.value = ""
        fetchFilteredRecipes()
    }

    private fun performSearch() {
        fetchFilteredRecipes()
    }

    private fun fetchFilteredRecipes() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = if (state.searchQuery.isBlank()) {
                    repository.getRecipesPaged(context, state.selectedCategory, 0)
                } else {
                    repository.searchRecipesWithFilters(context, state.searchQuery, state.selectedCategory)
                }

                val currentFavoriteIds = favoriteManager.favoriteIds.first()
                val finalResults = results.map { recipe ->
                    recipe.copy(isFavorite = currentFavoriteIds.contains(recipe.id))
                }

                _uiState.update {
                    it.copy(
                        recipes = finalResults,
                        isLoading = false,
                        isEndReached = state.searchQuery.isNotBlank() || finalResults.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadMore || state.isEndReached || state.searchQuery.isNotBlank()) return

        _uiState.update { it.copy(isLoadMore = true) }
        val nextPage = state.currentPage + 1

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = repository.getRecipesPaged(context, state.selectedCategory, nextPage)
                val currentFavoriteIds = favoriteManager.favoriteIds.first()
                val finalResults = results.map { recipe ->
                    recipe.copy(isFavorite = currentFavoriteIds.contains(recipe.id))
                }

                if (finalResults.isEmpty()) {
                    _uiState.update { it.copy(isEndReached = true, isLoadMore = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            recipes = it.recipes + finalResults,
                            isLoadMore = false,
                            currentPage = nextPage
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMore = false) }
            }
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipe.id)
        }
    }
}
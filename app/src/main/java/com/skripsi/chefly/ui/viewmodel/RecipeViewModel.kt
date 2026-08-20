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
        loadDynamicCategories()
        fetchFilteredRecipes()
        observeFavorites()

        viewModelScope.launch {
            _searchQueryInternal
                .debounce(300)
                .distinctUntilChanged()
                .collect { performSearch() }
        }
    }

    private fun loadDynamicCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbCategories = repository.getUniqueCategories(context)
            val currentSelected = _uiState.value.selectedCategory

            val mappedCategories = mutableListOf(
                CategoryData(
                    name = "Semua",
                    icon = Icons.Default.AllInclusive,
                    isActive = currentSelected.equals("Semua", ignoreCase = true)
                )
            )

            // Filter out 'kambing' dan ubah ke Title Case
            dbCategories
                .filterNot { it.trim().equals("kambing", ignoreCase = true) }
                .forEach { cat ->
                    val cleanName = cat.trim().lowercase().replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                    val icon = when (cleanName.lowercase()) {
                        "ayam" -> Icons.Default.Restaurant
                        "daging", "sapi" -> Icons.Default.DinnerDining
                        "ikan", "seafood", "udang" -> Icons.Default.Sailing
                        "telur" -> Icons.Default.EggAlt
                        "sayur", "sayuran" -> Icons.Default.Eco
                        "tahu", "tempe" -> Icons.Default.SetMeal
                        else -> Icons.Default.Fastfood
                    }
                    mappedCategories.add(
                        CategoryData(
                            name = cleanName,
                            icon = icon,
                            isActive = currentSelected.equals(cleanName, ignoreCase = true)
                        )
                    )
                }

            _uiState.update { it.copy(categories = mappedCategories) }
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
            it.copy(isActive = it.name.equals(categoryName, ignoreCase = true))
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
            val results = repository.searchRecipesByTitle(
                context = context,
                query = state.searchQuery,
                category = state.selectedCategory,
                page = 0
            )

            val currentFavoriteIds = favoriteManager.favoriteIds.first()
            val finalResults = results.map { it.copy(isFavorite = currentFavoriteIds.contains(it.id)) }

            _uiState.update {
                it.copy(
                    recipes = finalResults,
                    isLoading = false,
                    isEndReached = finalResults.isEmpty()
                )
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadMore || state.isEndReached) return

        _uiState.update { it.copy(isLoadMore = true) }
        val nextPage = state.currentPage + 1

        viewModelScope.launch(Dispatchers.IO) {
            val results = repository.searchRecipesByTitle(
                context = context,
                query = state.searchQuery,
                category = state.selectedCategory,
                page = nextPage
            )
            val currentFavoriteIds = favoriteManager.favoriteIds.first()
            val finalResults = results.map { it.copy(isFavorite = currentFavoriteIds.contains(it.id)) }

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
        }
    }

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipe.id)
        }
    }
}
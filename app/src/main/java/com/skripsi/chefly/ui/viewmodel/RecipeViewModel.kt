package com.skripsi.chefly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers

// UI State tetap sama
data class RecipeUIState(
    val isLoading: Boolean = false,
    val isLoadMore: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val cookingMethods: List<String> = listOf("Semua", "Goreng", "Tumis", "Rebus", "Kukus", "Panggang"),
    val selectedCategory: String = "",
    val selectedMethod: String = "Semua",
    val searchQuery: String = "",
    val currentOffset: Int = 0,
    val isEndReached: Boolean = false,
    val errorMessage: String? = null
)

data class CategoryData(
    val name: String,
    val icon: ImageVector,
    val isActive: Boolean
)

@OptIn(FlowPreview::class)
@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val repository: RecipeRepository,
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val favoriteManager = com.skripsi.chefly.util.FavoriteManager(context)
    private val _uiState = MutableStateFlow(RecipeUIState())
    val uiState = _uiState.asStateFlow()

    private val defaultCategories = listOf(
        CategoryData("Semua", Icons.Default.AllInclusive, true),
        CategoryData("Ayam", Icons.Default.Restaurant, false),
        CategoryData("Sapi", Icons.Default.DinnerDining, false),
        CategoryData("Telur", Icons.Default.EggAlt, false),
        CategoryData("Tahu", Icons.Default.BakeryDining, false),
        CategoryData("Tempe", Icons.Default.BreakfastDining, false),
        CategoryData("Ikan", Icons.Default.SetMeal, false)
    )

    private val _searchQuery = MutableStateFlow("")

    init {
        _uiState.update { it.copy(categories = defaultCategories, selectedCategory = "Semua") }
        fetchFilteredRecipes()

        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .collect { performSearch(it) }
        }

        observeFavorites()
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

    // --- FUNGSI TOGGLE FAVORITE (MENGGUNAKAN DATASTORE) ---
    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipe.id)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        fetchFilteredRecipes()
    }

    fun onMethodSelected(method: String) {
        if (_uiState.value.selectedMethod == method) return
        _uiState.update { it.copy(selectedMethod = method) }
        fetchFilteredRecipes()
    }

    fun onCategorySelected(categoryName: String) {
        val updatedCategories = _uiState.value.categories.map {
            it.copy(isActive = it.name == categoryName)
        }

        _uiState.update {
            it.copy(
                categories = updatedCategories,
                selectedCategory = categoryName,
                searchQuery = "",
                recipes = emptyList(),
                currentOffset = 0,
                isEndReached = false,
                isLoading = true
            )
        }
        _searchQuery.value = ""
        fetchFilteredRecipes()
    }

    private fun fetchFilteredRecipes() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, recipes = emptyList()) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Ambil data mentah dari DB
                val rawResults = if (state.searchQuery.isNotBlank()) {
                    repository.searchRecipesWithFilters(context, state.searchQuery, state.selectedCategory, state.selectedMethod)
                } else {
                    repository.getRecipesPaged(context, state.selectedCategory, state.selectedMethod, 0)
                }

                // 2. Ambil ID favorit yang ada saat ini dari DataStore secara manual (untuk inisialisasi)
                val currentFavoriteIds = favoriteManager.favoriteIds.first()

                // 3. Gabungkan: Tandai mana yang favorit
                val finalResults = rawResults.map { recipe ->
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
        if (state.isLoadMore || state.isEndReached || state.searchQuery.isNotEmpty()) return

        _uiState.update { it.copy(isLoadMore = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = state.recipes.size / 30
                val newRecipes = repository.getRecipesPaged(
                    context = context,
                    category = state.selectedCategory,
                    method = state.selectedMethod,
                    pageNumber = nextPage
                )

                if (newRecipes.isEmpty()) {
                    _uiState.update { it.copy(isEndReached = true, isLoadMore = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            recipes = it.recipes + newRecipes,
                            isLoadMore = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadMore = false) }
            }
        }
    }
}
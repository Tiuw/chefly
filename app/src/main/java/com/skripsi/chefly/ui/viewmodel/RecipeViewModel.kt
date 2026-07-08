package com.skripsi.chefly.ui.viewmodel

import android.app.Application
import android.util.Log
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
import com.skripsi.chefly.util.RecipeRecommendationSystem
import kotlinx.coroutines.Dispatchers

data class RecipeUIState(
    val isLoading: Boolean = false,
    val isLoadMore: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val categories: List<CategoryData> = emptyList(),
    val selectedCategory: String = "",
    val searchQuery: String = "",
    val currentOffset: Int = 0,
    val isEndReached: Boolean = false,
    val errorMessage: String? = null,
    val isAiSearchActive: Boolean = false,
    val isFromAiScanner: Boolean = false
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
    private val recommendationSystem: RecipeRecommendationSystem, // Sistem rekomendasi TF-IDF & Cosine Similarity
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val favoriteManager = com.skripsi.chefly.util.FavoriteManager(context)
    private val _uiState = MutableStateFlow(RecipeUIState())
    val uiState = _uiState.asStateFlow()

    fun disableLoadingPlaceholder() {
        _uiState.update { it.copy(isLoading = false) }
    }

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

    fun toggleFavorite(recipe: Recipe) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipe.id)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (!query.contains(",")) {
            _uiState.update { it.copy(isFromAiScanner = false) }
        }
        _searchQuery.value = query
    }

    fun triggerAiScannerInput(csvQuery: String) {
        _uiState.update {
            it.copy(
                searchQuery = csvQuery,
                isFromAiScanner = true
            )
        }
        _searchQuery.value = csvQuery
    }

    private fun performSearch(query: String) {
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
                isLoading = true,
                isAiSearchActive = false
            )
        }
        _searchQuery.value = ""
        fetchFilteredRecipes()
    }

    // LOGIKA UTAMA: Menggabungkan Filter Kategori dengan Sistem Perangkingan Cosine Similarity
    private fun fetchFilteredRecipes() {
        val state = _uiState.value
        val isAiInput = state.searchQuery.contains(",")

        if (isAiInput) {
            executeCosineRecommendation(state.searchQuery)
        } else {
            executeStandardTextSearch(state)
        }
    }

    /**
     * Jalankan filter teks SQL standard bawaan Room DB
     */
    private fun executeStandardTextSearch(state: RecipeUIState) {
        _uiState.update { it.copy(isLoading = true, isAiSearchActive = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // REVISI: Parameter method dihapus dari pemanggilan repositori
                val rawResults = if (state.searchQuery.isNotBlank()) {
                    repository.searchRecipesWithFilters(context, state.searchQuery, state.selectedCategory)
                } else {
                    repository.getRecipesPaged(context, state.selectedCategory, 0)
                }

                val currentFavoriteIds = favoriteManager.favoriteIds.first()
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

    /**
     * Jalankan Komputasi TF-IDF & Cosine Similarity Skripsi
     */
    fun executeCosineRecommendation(rawQuery: String) {
        _uiState.update { it.copy(isLoading = true, isAiSearchActive = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Pecah teks CSV kembali menjadi list elemen bahan pangan bersih
                val ingredientsList = rawQuery.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (ingredientsList.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, recipes = emptyList(), isAiSearchActive = false) }
                    return@launch
                }

                // Jalankan perhitungan spasial vektor jarak Cosine Similarity
                val aiRecommendations = recommendationSystem.getRecommendations(ingredientsList)

                Log.d("Chefly_Debug", "Hasil Cosine mendeteksi total: ${aiRecommendations.size} resep.")

                val currentFavoriteIds = favoriteManager.favoriteIds.first()

                // Proses Mapping Objek Resep secara Utuh dari Room lokal
                val finalAiResults = aiRecommendations.mapNotNull { aiResult ->
                    val fullRecipe = repository.getRecipeById(context, aiResult.recipeId.toString().trim())

                    if (fullRecipe != null) {
                        // REVISI: Logika post-filtering berbasis metode memasak dihapus penuh
                        fullRecipe.copy(
                            isFavorite = currentFavoriteIds.contains(fullRecipe.id),
                            similarity = aiResult.similarityScore
                        )
                    } else {
                        Log.e("RecipeViewModel", "ID Resep #${aiResult.recipeId} ada di TF-IDF tapi tidak ditemukan di Room DB!")
                        null
                    }
                }

                _uiState.update {
                    it.copy(
                        recipes = finalAiResults,
                        isLoading = false,
                        isEndReached = true,
                        isFromAiScanner = false
                    )
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Gagal menghitung matriks kecocokan AI: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        // Jika mode pencarian AI aktif atau kueri terisi, kunci mekanisme pagination bawaan
        if (state.isAiSearchActive || state.isLoadMore || state.isEndReached || state.searchQuery.isNotEmpty()) return

        _uiState.update { it.copy(isLoadMore = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = state.recipes.size / 30
                // REVISI: Parameter method dihapus dari pemanggilan repositori paged
                val newRecipes = repository.getRecipesPaged(
                    context = context,
                    category = state.selectedCategory,
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
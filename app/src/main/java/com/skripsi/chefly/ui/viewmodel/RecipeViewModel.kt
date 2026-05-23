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
    val cookingMethods: List<String> = listOf("Semua", "Goreng", "Tumis", "Rebus", "Kukus", "Panggang"),
    val selectedCategory: String = "",
    val selectedMethod: String = "Semua",
    val searchQuery: String = "",
    val currentOffset: Int = 0,
    val isEndReached: Boolean = false,
    val errorMessage: String? = null,
    val isAiSearchActive: Boolean = false // 🟢 Menandai apakah sedang memakai mode perangkingan Cosine Similarity
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
    private val recommendationSystem: RecipeRecommendationSystem, // 🟢 Suntikkan sistem rekomendasi TF-IDF milikmu
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val favoriteManager = com.skripsi.chefly.util.FavoriteManager(context)
    private val _uiState = MutableStateFlow(RecipeUIState())
    val uiState = _uiState.asStateFlow()

    // 🟢 Tambahkan fungsi ini di dalam RecipeViewModel.kt
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
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        fetchFilteredRecipes()
    }

    fun onMethodSelected(method: String) {
        if (_uiState.value.selectedMethod == method) return

        // 1. Update status metode yang dipilih di UI terlebih dahulu
        _uiState.update { it.copy(selectedMethod = method) }

        // 2. Cek pemicu kueri search bar
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.contains(",")) {
            // 🟢 Jika sedang dalam mode AI (ada koma), hitung ulang Cosine dengan filter barumu
            executeCosineRecommendation(currentQuery)
        } else {
            // Jika sedang dalam mode pencarian teks biasa, jalankan pencarian SQL standar
            fetchFilteredRecipes()
        }
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

    // 🟢 LOGIKA UTAMA: Menggabungkan Filter Lokal Bawaan dengan Mesin Perangkingan Cosine Similarity
    private fun fetchFilteredRecipes() {
        val state = _uiState.value

        // Cek apakah string query mengandung tanda koma sebagai indikator luapan data bahan
        val isAiInput = state.searchQuery.contains(",")

        if (isAiInput) {
            executeCosineRecommendation(state.searchQuery)
        } else {
            executeStandardTextSearch(state)
        }
    }

    /**
     * Jalankan filter teks SQL standard bawaan rancangan awalmu
     */
    private fun executeStandardTextSearch(state: RecipeUIState) {
        _uiState.update { it.copy(isLoading = true, isAiSearchActive = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawResults = if (state.searchQuery.isNotBlank()) {
                    repository.searchRecipesWithFilters(context, state.searchQuery, state.selectedCategory, state.selectedMethod)
                } else {
                    repository.getRecipesPaged(context, state.selectedCategory, state.selectedMethod, 0)
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
    /**
     * Jalankan Komputasi TF-IDF & Cosine Similarity Skripsi
     */
    fun executeCosineRecommendation(rawQuery: String) {
        val currentState = _uiState.value
        val activeMethod = currentState.selectedMethod

        _uiState.update { it.copy(isLoading = true, isAiSearchActive = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // 2. Pecah teks CSV kembali menjadi representasi elemen List bersih
                val ingredientsList = rawQuery.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (ingredientsList.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, recipes = emptyList(), isAiSearchActive = false) }
                    return@launch
                }

                // 3. Jalankan perhitungan spasial vector jarak Cosine Similarity
                val aiRecommendations = recommendationSystem.getRecommendations(ingredientsList)

                Log.d("Chefly_Debug", "Hasil Cosine sebelum filter metode mendeteksi total: ${aiRecommendations.size} resep.")

                // 4. Tarik data ID favorit saat ini
                val currentFavoriteIds = favoriteManager.favoriteIds.first()

                // 5. Proses Mapping Dan Post-Filtering berdasarkan Metode Memasak
                val finalAiResults = aiRecommendations.mapNotNull { aiResult ->
                    // Tarik data objek resep utuh dari database Room via Repository
                    val fullRecipe = repository.getRecipeById(context, aiResult.recipeId.toString().trim())

                    if (fullRecipe != null) {
                        // 🟢 LOGIKA POST-FILTERING METODE MEMASAK
                        // Lolos jika user memilih "Semua", string kosong, atau jika cookingMethod dari database cocok dengan filter UI
                        val isMethodMatch = activeMethod.isBlank() ||
                                activeMethod.equals("Semua", ignoreCase = true) ||
                                fullRecipe.cookingMethod?.equals(activeMethod, ignoreCase = true) == true

                        if (isMethodMatch) {
                            // Jika lolos filter, copy objek dan suntikkan skor kedekatan vektor serta status favoritnya
                            fullRecipe.copy(
                                isFavorite = currentFavoriteIds.contains(fullRecipe.id),
                                similarity = aiResult.similarityScore
                            )
                        } else {
                            // Jika metode memasak tidak cocok (misal resep kuah padahal filter tombol "Goreng"), eliminasi dari list
                            null
                        }
                    } else {
                        Log.e("RecipeViewModel", "ID Resep #${aiResult.recipeId} ada di TF-IDF tapi tidak ditemukan di Room DB!")
                        null
                    }
                }

                Log.d("Chefly_Debug", "Hasil Cosine setelah dieksekusi Post-Filtering: ${finalAiResults.size} resep lolos.")

                // 6. Update UI State secara utuh menggunakan Dispatchers utama (Main Thread otomatis via update)
                _uiState.update {
                    it.copy(
                        recipes = finalAiResults,
                        isLoading = false,
                        isEndReached = true // Hasil komputasi spasial matriks langsung keluar utuh satu halaman
                    )
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Gagal menghitung matriks kecocokan AI dan Post-Filter: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        // Jika mode pencarian AI NMS-Free aktif, kunci mekanisme paging bawaan
        if (state.isAiSearchActive || state.isLoadMore || state.isEndReached || state.searchQuery.isNotEmpty()) return

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
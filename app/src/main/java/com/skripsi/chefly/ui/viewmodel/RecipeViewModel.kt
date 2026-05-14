package com.skripsi.chefly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.local.RecipeDao
import com.skripsi.chefly.data.local.entity.RecipeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import com.skripsi.chefly.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlin.collections.map
data class RecipeUIState(
    val isLoading: Boolean = false,
    val isLoadMore: Boolean = false,
    val recipes: List<com.skripsi.chefly.data.Recipe> = emptyList(),
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
    private val repository: RecipeRepository, // Gunakan repository sebagai sumber data utama
    application: Application
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val _uiState = MutableStateFlow(RecipeUIState())
    val uiState = _uiState.asStateFlow()

    private val defaultCategories = listOf(
        CategoryData("Semua", Icons.Default.AllInclusive, true), // Jadikan default pertama
        CategoryData("Ayam", Icons.Default.Restaurant, false),
        CategoryData("Sapi", Icons.Default.DinnerDining, false),
        CategoryData("Telur", Icons.Default.EggAlt, false),
        CategoryData("Tahu", Icons.Default.BakeryDining, false),
        CategoryData("Tempe", Icons.Default.BreakfastDining, false),
        CategoryData("Ikan", Icons.Default.SetMeal, false)
    )

    // Status pencarian internal untuk debounce
    private val _searchQuery = MutableStateFlow("")

    init {
        _uiState.update { it.copy(categories = defaultCategories, selectedCategory = "Semua") }
        fetchFilteredRecipes()

        // OBSERVE SEARCH QUERY DENGAN DEBOUNCE
        viewModelScope.launch {
            _searchQuery
                .debounce(500) // Tunggu 500ms setelah user berhenti mengetik
                .distinctUntilChanged() // Jangan cari jika query sama dengan sebelumnya
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    // --- FUNGSI SEARCH ---
    fun onSearchQueryChanged(query: String) {
        // Update teks di UI secara instan agar tidak lag saat mengetik
        _uiState.update { it.copy(searchQuery = query) }
        // Kirim ke aliran debounce
        _searchQuery.value = query
    }

    private fun performSearch(query: String) {
        // Kita tidak perlu lagi melakukan kueri di sini, cukup panggil fetchFilteredRecipes
        fetchFilteredRecipes()
    }

    // --- FUNGSI FILTER METODE ---
    fun onMethodSelected(method: String) {
        if (_uiState.value.selectedMethod == method) return
        _uiState.update { it.copy(selectedMethod = method) }
        fetchFilteredRecipes() // Panggil fungsi tunggal
    }

    // --- FUNGSI FILTER KATEGORI ---
    fun onCategorySelected(categoryName: String) {
        val updatedCategories = _uiState.value.categories.map {
            it.copy(isActive = it.name == categoryName)
        }

        // 1. Update State UI dan Kosongkan Query
        _uiState.update {
            it.copy(
                categories = updatedCategories,
                selectedCategory = categoryName,
                searchQuery = "", // Reset teks di layar
                recipes = emptyList(),
                currentOffset = 0,
                isEndReached = false,
                isLoading = true
            )
        }

        // 2. Reset internal search query agar debounce tidak terpicu dengan data lama
        _searchQuery.value = ""

        // 3. Ambil data berdasarkan kategori murni
        fetchFilteredRecipes()
    }

    // --- FUNGSI FETCH UTAMA (Halaman Pertama) ---
    private fun fetchFilteredRecipes() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, recipes = emptyList(), isEndReached = false) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val results = if (state.searchQuery.isNotBlank()) {
                    // Sekarang Unresolved Reference akan hilang
                    repository.searchRecipesWithFilters(
                        context = context,
                        query = state.searchQuery,
                        category = state.selectedCategory,
                        method = state.selectedMethod
                    )
                } else {
                    repository.getRecipesPaged(
                        context = context,
                        category = state.selectedCategory,
                        method = state.selectedMethod,
                        pageNumber = 0
                    )
                }

                _uiState.update {
                    it.copy(
                        recipes = results,
                        isLoading = false,
                        // Jika mode search, matikan paging (isEndReached = true)
                        isEndReached = state.searchQuery.isNotBlank() || results.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // --- FUNGSI INFINITE SCROLL (Halaman Berikutnya) ---
    fun loadNextPage() {
        val state = _uiState.value
        // Jangan load jika sedang mencari kata kunci, sedang loading, atau sudah habis
        if (state.isLoadMore || state.isEndReached || state.searchQuery.isNotEmpty()) return

        _uiState.update { it.copy(isLoadMore = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = state.recipes.size / 30 // Mengikuti PAGE_SIZE repository (30)
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
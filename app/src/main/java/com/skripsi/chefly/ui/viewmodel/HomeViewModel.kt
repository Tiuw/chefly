package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for HomeScreen
 * Handles recipe loading, pagination, searching, and filtering by ingredients
 */
class HomeViewModel : ViewModel() {

    // UI State - exposed to UI
    private val _totalRecipes = MutableStateFlow(0)
    val totalRecipes: StateFlow<Int> = _totalRecipes.asStateFlow()

    private val _paginatedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val paginatedRecipes: StateFlow<List<Recipe>> = _paginatedRecipes.asStateFlow()

    private val _filteredRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val filteredRecipes: StateFlow<List<Recipe>> = _filteredRecipes.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Cache for matching ingredients count
    private val _matchingIngredientsCache = MutableStateFlow<Map<String, Pair<Int, Int>>>(emptyMap())
    val matchingIngredientsCache: StateFlow<Map<String, Pair<Int, Int>>> = _matchingIngredientsCache.asStateFlow()

    // Private internal state (not exposed to UI)
    private var lastSearchQueryLocal = ""
    private var lastIngredientSearchListLocal = listOf<String>()
    private var hasInitializedSearchLocal = false

    fun initializeHomeScreen(context: Context) {
        viewModelScope.launch {
            try {
                _isInitialLoading.value = true
                RecipeRepository.init(context)
                _totalRecipes.value = RecipeRepository.getRecipeCount(context)
                _loadError.value = null
            } catch (e: Exception) {
                _loadError.value = "Failed to load recipes"
                Log.e("HomeViewModel", "Error initializing", e)
            } finally {
                _isInitialLoading.value = false
            }
        }
    }

    fun loadFirstPage(context: Context) {
        viewModelScope.launch {
            if (!_isInitialLoading.value && _totalRecipes.value > 0 && _paginatedRecipes.value.isEmpty()) {
                try {
                    _isLoadingMore.value = true
                    val recipes = RecipeRepository.getRecipesPaged(context, 0)
                    _paginatedRecipes.value = recipes
                    _filteredRecipes.value = recipes
                    _currentPage.value = 1
                    lastIngredientSearchListLocal = emptyList()
                } catch (e: Exception) {
                    _loadError.value = "Error loading recipes"
                    Log.e("HomeViewModel", "Error loading first page", e)
                } finally {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    fun loadMoreRecipes(context: Context) {
        viewModelScope.launch {
            if (!_isLoadingMore.value && _searchQuery.value.isEmpty() && lastIngredientSearchListLocal.isEmpty()) {
                try {
                    _isLoadingMore.value = true
                    val nextRecipes = RecipeRepository.getRecipesPaged(context, _currentPage.value)
                    if (nextRecipes.isNotEmpty()) {
                        _paginatedRecipes.value = _paginatedRecipes.value + nextRecipes
                        _currentPage.value = _currentPage.value + 1
                    }
                } catch (e: Exception) {
                    _loadError.value = "Error loading more recipes"
                    Log.e("HomeViewModel", "Error loading more", e)
                } finally {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchRecipes(context: Context, query: String = _searchQuery.value) {
        viewModelScope.launch {
            if (query != lastSearchQueryLocal) {
                delay(800) // Debounce
                _isSearching.value = true
                try {
                    lastSearchQueryLocal = query
                    if (query.isEmpty()) {
                        _filteredRecipes.value = _paginatedRecipes.value
                        _isSearching.value = false
                    } else {
                        val results = RecipeRepository.searchRecipesByQuery(context, query)
                        _filteredRecipes.value = results
                        _isSearching.value = false
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error searching recipes", e)
                    _isSearching.value = false
                }
            }
        }
    }

    fun searchByIngredients(context: Context, ingredients: List<String>) {
        viewModelScope.launch {
            if (_paginatedRecipes.value.isNotEmpty() && ingredients.isNotEmpty()) {
                val ingredientListHasChanged =
                    ingredients != lastIngredientSearchListLocal || !hasInitializedSearchLocal

                if (ingredientListHasChanged) {
                    lastIngredientSearchListLocal = ingredients
                    hasInitializedSearchLocal = true
                    delay(300)
                    try {
                        _isSearching.value = true
                        val results = RecipeRepository.searchRecipesByIngredientsSusp(
                            context,
                            ingredients
                        )
                        _filteredRecipes.value = results
                        lastSearchQueryLocal = "ingredient_search"
                        _isSearching.value = false
                    } catch (e: Exception) {
                        _loadError.value = "Error searching recipes"
                        _isSearching.value = false
                        Log.e("HomeViewModel", "Error searching by ingredients", e)
                    }
                }
            }
        }
    }

    fun precomputeMatchingIngredients(context: Context, recipes: List<Recipe>, ingredients: List<String>) {
        viewModelScope.launch {
            if (ingredients.isNotEmpty() && recipes.isNotEmpty()) {
                try {
                    val cache = mutableMapOf<String, Pair<Int, Int>>()
                    recipes.forEach { recipe ->
                        recipe.id?.let { id ->
                            val matchInfo = RecipeRepository.getMatchingIngredientsCountSuspend(
                                context,
                                id,
                                ingredients
                            )
                            if (matchInfo != null) {
                                cache[id] = matchInfo
                            }
                        }
                    }
                    _matchingIngredientsCache.value = cache
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error precomputing matching ingredients", e)
                }
            } else {
                _matchingIngredientsCache.value = emptyMap()
            }
        }
    }

    fun resetSearch() {
        _searchQuery.value = ""
        lastSearchQueryLocal = ""
        _isSearching.value = false
        _filteredRecipes.value = _paginatedRecipes.value
    }
}









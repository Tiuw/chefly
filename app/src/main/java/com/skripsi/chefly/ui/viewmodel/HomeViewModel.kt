package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

/**
 * ViewModel for HomeScreen
 * Handles recipe loading, pagination, searching, and filtering by ingredients
 */
enum class HomeFeedMode {
    RECOMMENDED,
    CATEGORY,
    SCAN,
    SEARCH
}

class HomeViewModel : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 30
    }

    // UI State - exposed to UI
    private val _totalRecipes = MutableStateFlow(0)
    val totalRecipes: StateFlow<Int> = _totalRecipes.asStateFlow()

    private val _paginatedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val paginatedRecipes: StateFlow<List<Recipe>> = _paginatedRecipes.asStateFlow()

    private val _filteredRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val filteredRecipes: StateFlow<List<Recipe>> = _filteredRecipes.asStateFlow()

    private val _recommendedRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recommendedRecipes: StateFlow<List<Recipe>> = _recommendedRecipes.asStateFlow()

    private val _categoryRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val categoryRecipes: StateFlow<List<Recipe>> = _categoryRecipes.asStateFlow()

    private val _scanIngredients = MutableStateFlow<List<String>>(emptyList())
    val scanIngredients: StateFlow<List<String>> = _scanIngredients.asStateFlow()

    private val _scanRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val scanRecipes: StateFlow<List<Recipe>> = _scanRecipes.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _activeFeed = MutableStateFlow(HomeFeedMode.RECOMMENDED)
    val activeFeed: StateFlow<HomeFeedMode> = _activeFeed.asStateFlow()

    private val _currentPage = MutableStateFlow(0)

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
    private var previousFeedBeforeSearch = HomeFeedMode.RECOMMENDED
    private var lastSyncedScanSignature = ""
    private var searchJob: Job? = null
    private var categoryJob: Job? = null
    private var ingredientSearchJob: Job? = null
    private var preloadJob: Job? = null

    private fun normalizeSignature(items: List<String>): String {
        return items
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .sorted()
            .joinToString("|")
    }

    private fun refreshVisibleRecipes() {
        val visible = when (_activeFeed.value) {
            HomeFeedMode.SEARCH -> _filteredRecipes.value
            HomeFeedMode.CATEGORY -> _categoryRecipes.value
            HomeFeedMode.SCAN -> _scanRecipes.value.ifEmpty { _recommendedRecipes.value }
            HomeFeedMode.RECOMMENDED -> _recommendedRecipes.value
        }
        _filteredRecipes.value = visible
        _paginatedRecipes.value = when (_activeFeed.value) {
            HomeFeedMode.RECOMMENDED -> visible
            else -> _recommendedRecipes.value
        }
    }

    fun showRecommendedFeed() {
        searchJob?.cancel()
        categoryJob?.cancel()
        ingredientSearchJob?.cancel()

        _searchQuery.value = ""
        _filteredRecipes.value = _recommendedRecipes.value
        _selectedCategory.value = null
        _activeFeed.value = HomeFeedMode.RECOMMENDED
        refreshVisibleRecipes()
    }

    fun activateLastScanFeed() {
        if (_scanIngredients.value.isEmpty()) return

        searchJob?.cancel()
        categoryJob?.cancel()
        _searchQuery.value = ""
        _selectedCategory.value = null
        _activeFeed.value = HomeFeedMode.SCAN
        refreshVisibleRecipes()
    }

    fun selectCategory(context: Context, category: String?) {
        if (category.isNullOrBlank()) {
            showRecommendedFeed()
            return
        }

        val normalizedCategory = category.trim()
        if (_selectedCategory.value == normalizedCategory && _categoryRecipes.value.isNotEmpty()) {
            _activeFeed.value = HomeFeedMode.CATEGORY
            refreshVisibleRecipes()
            return
        }

        searchJob?.cancel()
        ingredientSearchJob?.cancel()
        _searchQuery.value = ""
        _filteredRecipes.value = emptyList()
        _selectedCategory.value = normalizedCategory
        _activeFeed.value = HomeFeedMode.CATEGORY

        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = RecipeRepository.getRecipesByCategory(context, normalizedCategory)
                _categoryRecipes.value = results
                refreshVisibleRecipes()
            } catch (e: Exception) {
                _loadError.value = "Error loading category"
                Log.e("HomeViewModel", "Error loading category", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun syncDetectedIngredients(context: Context, ingredients: List<String>) {
        val normalizedIngredients = ingredients
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (normalizedIngredients.isEmpty()) return

        val signature = normalizeSignature(normalizedIngredients)
        if (signature == lastSyncedScanSignature) return

        lastSyncedScanSignature = signature
        lastIngredientSearchListLocal = normalizedIngredients
        hasInitializedSearchLocal = true

        searchJob?.cancel()
        categoryJob?.cancel()
        _searchQuery.value = ""
        _selectedCategory.value = null
        _activeFeed.value = HomeFeedMode.SCAN

        ingredientSearchJob?.cancel()
        ingredientSearchJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                _scanIngredients.value = normalizedIngredients
                val results = RecipeRepository.searchRecipesByIngredientsSusp(context, normalizedIngredients)
                _scanRecipes.value = results
                refreshVisibleRecipes()
            } catch (e: Exception) {
                _loadError.value = "Error searching recipes"
                Log.e("HomeViewModel", "Error searching by ingredients", e)
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun initializeHomeScreen(context: Context) {
        viewModelScope.launch {
            try {
                _isInitialLoading.value = true
                RecipeRepository.init(context)
                _totalRecipes.value = RecipeRepository.getRecipeCount(context)
                _loadError.value = null

                if (_recommendedRecipes.value.isEmpty()) {
                    val recommended = RecipeRepository.getRecommendedRecipes(context, PAGE_SIZE)
                    _recommendedRecipes.value = recommended
                    _paginatedRecipes.value = recommended
                    _filteredRecipes.value = recommended
                    _currentPage.value = 1
                }

                if (preloadJob?.isActive != true) {
                    preloadJob = viewModelScope.launch(Dispatchers.Default) {
                        RecipeRepository.preloadAllRecipes(context)
                    }
                }
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
            if (!_isInitialLoading.value && _totalRecipes.value > 0 && _recommendedRecipes.value.isEmpty()) {
                try {
                    _isLoadingMore.value = true
                    val recipes = RecipeRepository.getRecommendedRecipes(context, PAGE_SIZE)
                    _recommendedRecipes.value = recipes
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
            if (!_isLoadingMore.value &&
                _searchQuery.value.isEmpty() &&
                lastIngredientSearchListLocal.isEmpty() &&
                _selectedCategory.value == null &&
                _activeFeed.value == HomeFeedMode.RECOMMENDED
            ) {
                try {
                    _isLoadingMore.value = true
                    val nextRecipes = RecipeRepository.getRecipesPaged(context, _currentPage.value)
                    if (nextRecipes.isNotEmpty()) {
                        _recommendedRecipes.value = _recommendedRecipes.value + nextRecipes
                        _paginatedRecipes.value = _recommendedRecipes.value
                        _filteredRecipes.value = _recommendedRecipes.value
                        _currentPage.value += 1
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
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val normalizedQuery = query.trim()

            if (normalizedQuery.isEmpty()) {
                lastSearchQueryLocal = ""
                val restoredFeed = if (_activeFeed.value == HomeFeedMode.SEARCH) {
                    previousFeedBeforeSearch
                } else {
                    _activeFeed.value
                }

                _activeFeed.value = restoredFeed
                _filteredRecipes.value = when (restoredFeed) {
                    HomeFeedMode.SEARCH -> _recommendedRecipes.value
                    HomeFeedMode.CATEGORY -> _categoryRecipes.value
                    HomeFeedMode.SCAN -> _scanRecipes.value.ifEmpty { _recommendedRecipes.value }
                    HomeFeedMode.RECOMMENDED -> _recommendedRecipes.value
                }
                return@launch
            }

            if (normalizedQuery == lastSearchQueryLocal) return@launch

            if (_activeFeed.value != HomeFeedMode.SEARCH) {
                previousFeedBeforeSearch = _activeFeed.value
            }
            _activeFeed.value = HomeFeedMode.SEARCH

            delay(250)
            if (normalizedQuery != _searchQuery.value.trim()) return@launch

            _isSearching.value = true
            try {
                val results = RecipeRepository.searchRecipesByQuery(context, normalizedQuery)
                lastSearchQueryLocal = normalizedQuery
                _filteredRecipes.value = results
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error searching recipes", e)
                _loadError.value = "Error searching recipes"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchAndRestoreFeed() {
        _searchQuery.value = ""
        _filteredRecipes.value = when (_activeFeed.value) {
            HomeFeedMode.SEARCH -> when (previousFeedBeforeSearch) {
                HomeFeedMode.CATEGORY -> _categoryRecipes.value
                HomeFeedMode.SCAN -> _scanRecipes.value
                HomeFeedMode.RECOMMENDED -> _recommendedRecipes.value
                HomeFeedMode.SEARCH -> _recommendedRecipes.value
            }
            HomeFeedMode.CATEGORY -> _categoryRecipes.value
            HomeFeedMode.SCAN -> _scanRecipes.value
            HomeFeedMode.RECOMMENDED -> _recommendedRecipes.value
        }
        _activeFeed.value = when {
            _selectedCategory.value != null -> HomeFeedMode.CATEGORY
            _scanIngredients.value.isNotEmpty() -> HomeFeedMode.SCAN
            else -> HomeFeedMode.RECOMMENDED
        }
        lastSearchQueryLocal = ""
    }

    fun searchByIngredients(context: Context, ingredients: List<String>) {
        val normalizedIngredients = ingredients
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (normalizedIngredients.isEmpty()) return

        val signature = normalizeSignature(normalizedIngredients)
        if (signature == lastSyncedScanSignature && _scanRecipes.value.isNotEmpty()) {
            activateLastScanFeed()
            return
        }

        if (signature == lastSyncedScanSignature && _scanRecipes.value.isEmpty()) {
            lastSyncedScanSignature = ""
        }

        syncDetectedIngredients(context, normalizedIngredients)
    }

    fun precomputeMatchingIngredients(context: Context, recipes: List<Recipe>, ingredients: List<String>) {
        viewModelScope.launch(Dispatchers.Default) {
            if (ingredients.isNotEmpty() && recipes.isNotEmpty()) {
                try {
                    val cache = mutableMapOf<String, Pair<Int, Int>>()
                    val toProcess = recipes.take(20)
                    toProcess.forEach { recipe ->
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

}









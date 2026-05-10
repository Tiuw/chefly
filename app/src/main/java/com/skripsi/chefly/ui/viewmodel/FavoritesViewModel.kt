package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for FavoritesScreen
 * Manages loading and displaying favorite recipes
 */
class FavoritesViewModel : ViewModel() {

    private val _allRecipes = MutableStateFlow<List<Recipe>>(emptyList())
    val allRecipes: StateFlow<List<Recipe>> = _allRecipes.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun loadAllRecipes(context: Context) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadError.value = null
                RecipeRepository.init(context)
                
                val recipes = RecipeRepository.getAllRecipes(context)
                _allRecipes.value = recipes
            } catch (e: Exception) {
                _loadError.value = "Error loading recipes: ${e.message}"
                Log.e("FavoritesViewModel", "Error loading recipes", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRecipeById(recipeId: String): Recipe? {
        return _allRecipes.value.find { it.id == recipeId }
    }
}


package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for RecipeDetailScreen
 * Handles loading and displaying recipe details
 */
class RecipeDetailViewModel : ViewModel() {

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun loadRecipe(context: Context, recipeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadError.value = null

                val loadedRecipe = withContext(Dispatchers.IO) {
                    RecipeRepository.init(context)
                    RecipeRepository.getRecipeById(context, recipeId)
                }

                _recipe.value = loadedRecipe
                if (loadedRecipe == null) {
                    _loadError.value = "Recipe not found"
                }
            } catch (e: Exception) {
                _loadError.value = "Error loading recipe: ${e.message}"
                Log.e("RecipeDetailViewModel", "Error loading recipe", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCleanedIngredients(): List<String> {
        return _recipe.value?.ingredientList
            ?.map { cleanRecipeText(it) }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    private fun cleanRecipeText(raw: String): String {
        return raw
            .trim()
            .replace(Regex("^\\[\\s*"), "")
            .replace(Regex("\\s*]$"), "")
            .replace(Regex("^\""), "")
            .replace(Regex("\"$"), "")
            .replace(Regex("^'"), "")
            .replace(Regex("'$"), "")
            .replace("\\\"", "\"")
            .trim()
    }
}


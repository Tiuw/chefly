package com.skripsi.chefly.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.skripsi.chefly.data.Recipe

class RecipeViewModel : ViewModel() {
    var favoriteRecipes by mutableStateOf<Set<Int>>(emptySet())
        private set

    var detectedIngredients by mutableStateOf<List<String>>(emptyList())
        private set

    fun toggleFavorite(recipeId: Int) {
        favoriteRecipes = if (favoriteRecipes.contains(recipeId)) {
            favoriteRecipes - recipeId
        } else {
            favoriteRecipes + recipeId
        }
    }

    fun isFavorite(recipeId: Int): Boolean {
        return favoriteRecipes.contains(recipeId)
    }

    fun updateDetectedIngredients(ingredients: List<String>) {
        detectedIngredients = ingredients.distinct()
    }
}


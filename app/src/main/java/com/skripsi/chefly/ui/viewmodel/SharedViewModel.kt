package com.skripsi.chefly.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared ViewModel for app-wide state (favorites, detected ingredients, fridge ingredients)
 * This is accessible from all screens via viewModel()
 */
class SharedViewModel : ViewModel() {

    // Favorites state
    private val _favoriteRecipes = MutableStateFlow<Set<String>>(emptySet())
    val favoriteRecipes: StateFlow<Set<String>> = _favoriteRecipes.asStateFlow()

    // Camera-detected ingredients
    private val _detectedIngredients = MutableStateFlow<List<String>>(emptyList())
    val detectedIngredients: StateFlow<List<String>> = _detectedIngredients.asStateFlow()

    // Fridge ingredients
    private val _fridgeIngredients = MutableStateFlow<Set<String>>(emptySet())
    val fridgeIngredients: StateFlow<Set<String>> = _fridgeIngredients.asStateFlow()

    // Combined selected ingredients (detected + fridge)
    private val _allSelectedIngredients = MutableStateFlow<List<String>>(emptyList())
    val allSelectedIngredients: StateFlow<List<String>> = _allSelectedIngredients.asStateFlow()

    // Favorite management
    fun toggleFavorite(recipeId: String) {
        _favoriteRecipes.value = if (_favoriteRecipes.value.contains(recipeId)) {
            _favoriteRecipes.value - recipeId
        } else {
            _favoriteRecipes.value + recipeId
        }
    }

    fun isFavorite(recipeId: String): Boolean {
        return _favoriteRecipes.value.contains(recipeId)
    }

    // Detected ingredients management (from camera)
    fun updateDetectedIngredients(ingredients: List<String>) {
        _detectedIngredients.value = ingredients.distinct()
        updateAllSelectedIngredients()
    }

    fun clearDetectedIngredients() {
        _detectedIngredients.value = emptyList()
        updateAllSelectedIngredients()
    }

    // Fridge ingredients management
    fun toggleFridgeIngredient(ingredient: String) {
        _fridgeIngredients.value = if (_fridgeIngredients.value.contains(ingredient)) {
            _fridgeIngredients.value - ingredient
        } else {
            _fridgeIngredients.value + ingredient
        }
        updateAllSelectedIngredients()
    }

    fun isIngredientInFridge(ingredient: String): Boolean {
        return _fridgeIngredients.value.contains(ingredient)
    }

    fun clearFridgeIngredients() {
        _fridgeIngredients.value = emptySet()
        updateAllSelectedIngredients()
    }

    // Helper to update combined selected ingredients
    private fun updateAllSelectedIngredients() {
        _allSelectedIngredients.value =
            (_detectedIngredients.value + _fridgeIngredients.value).distinct()
    }

    fun getAllSelectedIngredients(): List<String> {
        return _allSelectedIngredients.value
    }
}


package com.skripsi.chefly.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for FridgeScreen
 * Manages ingredient selection and fridge state
 */
class FridgeViewModel : ViewModel() {

    private val _selectedIngredients = MutableStateFlow<Set<String>>(emptySet())
    val selectedIngredients: StateFlow<Set<String>> = _selectedIngredients.asStateFlow()

    fun toggleIngredient(ingredient: String) {
        _selectedIngredients.value = if (_selectedIngredients.value.contains(ingredient)) {
            _selectedIngredients.value - ingredient
        } else {
            _selectedIngredients.value + ingredient
        }
    }

    fun isIngredientSelected(ingredient: String): Boolean {
        return _selectedIngredients.value.contains(ingredient)
    }

    fun clearAllIngredients() {
        _selectedIngredients.value = emptySet()
    }

    fun setIngredients(ingredients: Set<String>) {
        _selectedIngredients.value = ingredients
    }

    fun getSelectedIngredients(): Set<String> {
        return _selectedIngredients.value
    }
}


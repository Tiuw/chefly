package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadError.value = null

                // Ambil data langsung dari Repository
                val result = RecipeRepository.getRecipeById(context, recipeId)

                if (result != null) {
                    _recipe.value = result
                } else {
                    _loadError.value = "Resep tidak ditemukan"
                }
            } catch (e: Exception) {
                _loadError.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
}
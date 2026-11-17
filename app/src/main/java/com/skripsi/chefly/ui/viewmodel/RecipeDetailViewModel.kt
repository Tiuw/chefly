package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.util.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    private val repository: RecipeRepository, // WAJIB DISUNTIKKAN DI SINI
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private val favoriteManager = FavoriteManager(context) // Inisialisasi

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadError.value = null

                // PERBAIKAN: Panggil melalui variabel 'repository' (huruf kecil)
                val result = repository.getRecipeById(context, recipeId)

                if (result != null) {
                    _recipe.value = result // Sekarang tipe datanya sinkron (Recipe)
                    observeFavoriteStatus(recipeId)
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

    // Memantau perubahan favorit khusus untuk resep ini
    private fun observeFavoriteStatus(recipeId: String) {
        viewModelScope.launch {
            favoriteManager.favoriteIds.collectLatest { savedIds ->
                _recipe.value = _recipe.value?.copy(isFavorite = savedIds.contains(recipeId))
            }
        }
    }

    // Fungsi toggle yang dipanggil saat icon pita diklik di detail screen
    fun toggleFavorite() {
        val currentRecipe = _recipe.value ?: return
        viewModelScope.launch {
            favoriteManager.toggleFavorite(currentRecipe.id)
        }
    }
}
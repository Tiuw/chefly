package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.Recipe
import com.skripsi.chefly.data.repository.RecipeRepository
import com.skripsi.chefly.util.FavoriteManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedScreenViewModel @Inject constructor(
    private val repository: RecipeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 1. Inisialisasi FavoriteManager (DataStore)
    private val favoriteManager = FavoriteManager(context)

    // 2. Hubungkan ID dari DataStore dengan data asli di Repository
    val savedRecipes: StateFlow<List<Recipe>> = favoriteManager.favoriteIds
        .map { ids ->
            // Untuk setiap ID yang ada di catatan (DataStore),
            // ambil data lengkap resepnya dari database utama
            ids.mapNotNull { id ->
                repository.getRecipeById(context, id)?.copy(isFavorite = true)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Tambahkan di SavedScreenViewModel.kt
    fun removeFromFavorite(recipeId: String) {
        viewModelScope.launch {
            favoriteManager.toggleFavorite(recipeId) // Karena ini toggle, kalau diklik saat sudah ada, dia akan menghapus
        }
    }
}


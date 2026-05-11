package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.repository.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeUiModel(
    val id: String,
    val title: String,
    val imageUrl: String,
    val loves: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _suggestedRecipes = MutableStateFlow<List<RecipeUiModel>>(emptyList())
    val suggestedRecipes: StateFlow<List<RecipeUiModel>> = _suggestedRecipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                RecipeRepository.init(context)

                // Cek jumlah total di DB
                val total = RecipeRepository.getRecipeCount(context)
                android.util.Log.d("DEBUG_CHEF", "Total data di DB: $total")

                val recipes = RecipeRepository.getRecommendedRecipes(context, limit = 10)
                android.util.Log.d("DEBUG_CHEF", "Data yang didapat: ${recipes.size}")

                _suggestedRecipes.value = recipes.map { recipe ->
                    RecipeUiModel(
                        id = recipe.id,
                        title = recipe.name,
                        imageUrl = recipe.imageUrl,
                        loves = recipe.loves ?: 0
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("DEBUG_CHEF", "Error: ${e.message}")
                _suggestedRecipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
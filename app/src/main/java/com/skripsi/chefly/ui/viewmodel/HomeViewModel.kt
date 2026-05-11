package com.skripsi.chefly.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.local.RecipeDao
import com.skripsi.chefly.data.local.entity.RecipeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Model UI yang bersih tanpa field waktu
data class RecipeUiModel(
    val id: String,
    val title: String,
    val imageUrl: String,
    val loves: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recipeDao: RecipeDao
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
                val entities = recipeDao.getRecommendedRecipes(limit = 10)
                _suggestedRecipes.value = entities.map { entity ->
                    RecipeUiModel(
                        id = entity.id,
                        title = entity.name ?: "Tanpa Judul",
                        imageUrl = entity.imageUrl ?: "",
                        loves = entity.loves ?: 0,
                    )
                }
            } catch (e: Exception) {
                _suggestedRecipes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
package com.skripsi.chefly.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.repository.IngredientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IngredientGroup(
    val categoryName: String,
    val icon: ImageVector,
    val color: Color,
    val ingredients: List<String>
)

sealed class IngredientUiState {
    object Loading : IngredientUiState()
    data class Success(val groups: List<IngredientGroup>) : IngredientUiState()
    data class Error(val message: String) : IngredientUiState()
}

// --- 2. VIEWMODEL ---
@HiltViewModel
class AddIngredientViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<IngredientUiState>(IngredientUiState.Loading)
    val uiState: StateFlow<IngredientUiState> = _uiState.asStateFlow()

    private val _selectedIngredients = MutableStateFlow<Set<String>>(emptySet())
    val selectedIngredients: StateFlow<Set<String>> = _selectedIngredients.asStateFlow()

    init {
        loadIngredients()
    }

    private fun loadIngredients() {
        viewModelScope.launch {
            try {
                val groups = ingredientRepository.getCategorizedIngredients()
                _uiState.value = IngredientUiState.Success(groups)
            } catch (e: Exception) {
                _uiState.value = IngredientUiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun toggleIngredient(name: String) {
        val current = _selectedIngredients.value
        _selectedIngredients.value = if (current.contains(name)) current - name else current + name
    }
}
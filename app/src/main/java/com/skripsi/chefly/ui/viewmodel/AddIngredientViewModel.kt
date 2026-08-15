package com.skripsi.chefly.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.repository.IngredientRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Domain model untuk pengelompokan bahan
data class IngredientGroup(
    val categoryName: String,
    val icon: ImageVector,
    val color: Color,
    val ingredients: List<String>
)

// State UI untuk menangani loading, sukses, dan error
sealed class IngredientUiState {
    object Loading : IngredientUiState()
    data class Success(val groups: List<IngredientGroup>) : IngredientUiState()
    data class Error(val message: String) : IngredientUiState()
}

@HiltViewModel
class AddIngredientViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository
) : ViewModel() {

    // 1. State untuk Query pencarian dari Search Bar
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 2. State penampung data mentah yang didapat dari Repository
    private val _rawGroups = MutableStateFlow<List<IngredientGroup>>(emptyList())

    // 3. State untuk menangani pesan error
    private val _errorMessage = MutableStateFlow<String?>(null)

    // 4. State untuk bahan yang dipilih (dicentang user)
    private val _selectedIngredients = MutableStateFlow<Set<String>>(emptySet())
    val selectedIngredients: StateFlow<Set<String>> = _selectedIngredients.asStateFlow()

    /**
     * 🟢 REAKTIF UI STATE:
     * Menggabungkan data mentah bahan dan query pencarian secara efisien.
     * Logika filter dilakukan di sini agar UI tetap responsif.
     */
    val uiState: StateFlow<IngredientUiState> = combine(
        _rawGroups,
        _searchQuery,
        _errorMessage
    ) { groups, query, error ->
        when {
            error != null -> IngredientUiState.Error(error)
            groups.isEmpty() -> IngredientUiState.Loading
            else -> {
                // Lakukan filter jika user mengetik di search bar
                val filtered = if (query.isBlank()) {
                    groups
                } else {
                    groups.map { group ->
                        group.copy(
                            ingredients = group.ingredients.filter {
                                it.contains(query, ignoreCase = true)
                            }
                        )
                    }.filter { it.ingredients.isNotEmpty() }
                }
                IngredientUiState.Success(filtered)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IngredientUiState.Loading
    )

    init {
        loadIngredientsFromRepository()
        observeCameraDetections()
        // 🟢 Tambahkan sinkronisasi state di sini
        restoreSelectedIngredients()
    }

    /**
     * Mengambil data bahan yang sudah dikategorikan langsung dari Repository.
     * Karena Repository menggunakan Cache, proses ini akan instan setelah pemanggilan pertama.
     */
    private fun loadIngredientsFromRepository() {
        viewModelScope.launch {
            try {
                // Meminta data ke Repository (Pusat Data)
                val groups = ingredientRepository.getCategorizedIngredients()
                _rawGroups.value = groups
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Gagal memuat daftar bahan."
            }
        }
    }

    /**
     * Mengamati cache deteksi kamera secara real-time dari Repository
     */
    private fun observeCameraDetections() {
        viewModelScope.launch {
            ingredientRepository.detectedIngredientsFromCamera.collect { cameraIngredients ->
                if (cameraIngredients.isNotEmpty()) {
                    val formatted = cameraIngredients.map { name ->
                        // Pastikan format sama: "ayam" -> "Ayam"
                        name.replace("_", " ").replaceFirstChar { it.uppercase() }
                    }
                    // Gabungkan dengan yang sudah dipilih sebelumnya
                    _selectedIngredients.value = _selectedIngredients.value + formatted

                    // Bersihkan cache deteksi agar tidak terpanggil berulang kali
                    ingredientRepository.clearDetectedIngredients()
                }
            }
        }
    }

    /**
     * Memulihkan bahan yang sedang aktif di sistem rekomendasi global
     */
    private fun restoreSelectedIngredients() {
        viewModelScope.launch {
            // Ambil bahan yang sedang aktif di sistem rekomendasi
            val activeIngredients = ingredientRepository.currentRecommendationIngredients.value
            if (activeIngredients.isNotEmpty()) {
                _selectedIngredients.value = activeIngredients.toSet()
            }
        }
    }

    /**
     * Simpan state pilihan ke repository sebelum navigasi
     */
    fun saveToRepository() {
        ingredientRepository.setCurrentRecommendationIngredients(_selectedIngredients.value.toList())
    }

    /**
     * Update query pencarian saat user mengetik
     */
    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    /**
     * Menambah atau menghapus bahan dari daftar pilihan
     */
    fun toggleIngredient(name: String) {
        val current = _selectedIngredients.value
        _selectedIngredients.value = if (current.contains(name)) current - name else current + name
    }

    /**
     * Menghapus semua bahan yang dipilih
     */
    fun clearAllSelectedIngredients() {
        _selectedIngredients.value = emptySet()
    }
}
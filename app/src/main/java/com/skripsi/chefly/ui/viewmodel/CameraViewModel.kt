package com.skripsi.chefly.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.model.DetectedIngredient
import com.skripsi.chefly.data.repository.IngredientRepository
import com.skripsi.chefly.ml.YOLO26Detector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository,
    private val detector: YOLO26Detector
) : ViewModel() {

    private val _uploadedImage = MutableStateFlow<Bitmap?>(null)
    val uploadedImage: StateFlow<Bitmap?> = _uploadedImage.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    private val _imageDetections = MutableStateFlow<List<DetectedIngredient>>(emptyList())
    val imageDetections: StateFlow<List<DetectedIngredient>> = _imageDetections.asStateFlow()

    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage.asStateFlow()

    private val TAG = "CameraViewModel"

    /**
     * Memproses foto hasil jepretan kamera manual (Akurasi Tinggi)
     */
    fun processCapturedPhoto(bitmap: Bitmap) {
        _capturedImage.value = bitmap
        _uploadedImage.value = null
        _isProcessingImage.value = true
        _imageDetections.value = emptyList()

        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Inferensi dengan threshold optimal (0.50f)
                val results = detector.detectObjects(bitmap, 0.50f)
                val mapped = results.map { d ->
                    DetectedIngredient(
                        label = d.className,
                        confidence = d.confidence,
                        boundingBox = android.graphics.RectF(
                            d.box.left, d.box.top, d.box.right, d.box.bottom
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    _imageDetections.value = mapped
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error capture detection: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessingImage.value = false
                }
            }
        }
    }

    /**
     * Memproses gambar yang diunggah dari galeri
     */
    fun setUploadedImage(bitmap: Bitmap) {
        _uploadedImage.value = bitmap
        _capturedImage.value = null
        processImage(bitmap)
    }

    private fun processImage(bitmap: Bitmap) {
        _isProcessingImage.value = true
        _imageDetections.value = emptyList()

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val results = detector.detectObjects(bitmap, 0.50f)
                val mapped = results.map { d ->
                    DetectedIngredient(
                        label = d.className,
                        confidence = d.confidence,
                        boundingBox = android.graphics.RectF(
                            d.box.left, d.box.top, d.box.right, d.box.bottom
                        )
                    )
                }
                withContext(Dispatchers.Main) {
                    _imageDetections.value = mapped
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error upload detection: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessingImage.value = false
                }
            }
        }
    }

    fun resetCapture() {
        _capturedImage.value = null
        _uploadedImage.value = null
        _imageDetections.value = emptyList()
    }

    fun saveCurrentDetectionsToRepository() {
        val currentLabels = _imageDetections.value.map { it.label }.distinct()

        // 🟢 Format label agar sinkron dengan display name di AddIngredientScreen
        // Contoh: "ayam" -> "Ayam", "daging_sapi" -> "Daging sapi"
        val formattedLabels = currentLabels.map { name ->
            name.replace("_", " ").replaceFirstChar { it.uppercase() }
        }

        ingredientRepository.saveDetectedIngredients(currentLabels)

        // 🟢 Simpan label yang sudah rapi ke sistem rekomendasi aktif
        ingredientRepository.setCurrentRecommendationIngredients(formattedLabels)
    }
}
package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.model.DetectedIngredient
import com.skripsi.chefly.data.repository.IngredientRepository
import com.skripsi.chefly.ml.YOLO26Detector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel untuk CameraScreen
 * Menangani inferensi real-time Edge AI YOLO26 NMS-Free dan distribusi state deteksi ke UI
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val ingredientRepository: IngredientRepository,
    private val detector: YOLO26Detector // 🟢 REVISI: Suntikkan langsung detector via Hilt Module (Singleton)
) : ViewModel() {

    private val _detections = MutableStateFlow<List<DetectedIngredient>>(emptyList())
    val detections: StateFlow<List<DetectedIngredient>> = _detections.asStateFlow()

    private val _uploadedImage = MutableStateFlow<Bitmap?>(null)
    val uploadedImage: StateFlow<Bitmap?> = _uploadedImage.asStateFlow()

    private val _imageDetections = MutableStateFlow<List<DetectedIngredient>>(emptyList())
    val imageDetections: StateFlow<List<DetectedIngredient>> = _imageDetections.asStateFlow()

    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage.asStateFlow()

    private val _debugMessage = MutableStateFlow<String?>(null)
    val debugMessage: StateFlow<String?> = _debugMessage.asStateFlow()

    private val TAG = "CameraViewModel"

    // 🟢 REVISI: Fungsi inisialisasi sekarang hanya memastikan label dan model siap dipakai tanpa re-instantiate objek berat
    fun initializeDetector(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Cek isi assets secara aman untuk keperluan logging sidang
                val labels = context.assets.open("labels.txt").bufferedReader().use { it.readText() }
                    .split('\n')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (labels.isEmpty()) {
                    throw IllegalStateException("Berkas labels.txt kosong atau tidak valid!")
                }
                Log.i(TAG, "✅ Komponen model YOLO26 TFLite terverifikasi dengan ${labels.size} kelas.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gagal memuat komponen label atau model .tflite: ${e.message}", e)
            }
        }
    }

    fun processCameraFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d("Chefly_Debug", "📸 Frame diterima oleh ViewModel. Ukuran: ${bitmap.width}x${bitmap.height}")

                // Nilai threshold diatur standar 0.60f untuk akurasi optimal demo sidang
                val results = detector.detectObjects(bitmap, 0.60f)

                Log.d("Chefly_Debug", "⚙️ Hasil deteksi model YOLO26: ${results.size} objek ditemukan.")

                val mappedDetections = results.map { d ->
                    DetectedIngredient(
                        label = d.className,
                        confidence = d.confidence,
                        boundingBox = android.graphics.RectF(
                            d.box.left, d.box.top, d.box.right, d.box.bottom
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    _detections.value = mappedDetections
                }
            } catch (e: Exception) {
                Log.e("Chefly_Debug", "❌ Error di pipeline pemrosesan frame: ${e.message}", e)
            }
        }
    }

    /**
     * Fungsi pengalir data list string bahan ke level Repository
     */
    fun saveCurrentDetectionsToRepository(isGallery: Boolean) {
        val currentLabels = if (isGallery) {
            _imageDetections.value.map { it.label }
        } else {
            _detections.value.map { it.label }
        }
        ingredientRepository.saveDetectedIngredients(currentLabels)
        Log.d(TAG, "✈️ Berhasil mengirim data ${currentLabels.size} bahan dari ViewModel ke Repository.")
    }

    // --- PERBAIKAN BUG: Fungsi Pembersih State ---
    fun clearCameraDetections() {
        _detections.value = emptyList()
        Log.d(TAG, "🧹 State deteksi kamera real-time dibersihkan.")
    }

    fun clearUploadedImageDetections() {
        _uploadedImage.value = null
        _imageDetections.value = emptyList()
        Log.d(TAG, "🧹 State deteksi gambar galeri dibersihkan.")
    }

    fun setUploadedImage(bitmap: Bitmap) {
        _uploadedImage.value = bitmap
    }

    fun processUploadedImage(bitmap: Bitmap) {
        _isProcessingImage.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val results = detector.detectObjects(bitmap, 0.60f)
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
                Log.e(TAG, "Error memproses gambar unggahan: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessingImage.value = false
                }
            }
        }
    }

    fun triggerManualDetect() {
        val bitmap = _uploadedImage.value ?: return
        _isProcessingImage.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val t0 = System.currentTimeMillis()
                val results = detector.detectObjects(bitmap, 0.35f)
                val t1 = System.currentTimeMillis()

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
                    _debugMessage.value =
                        "Manual detect: found ${mapped.size} (in ${t1 - t0} ms): ${mapped.joinToString(",") { it.label }}"
                    delay(3000)
                    _debugMessage.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual detection error: ${e.message}", e)
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessingImage.value = false
                }
            }
        }
    }

    fun clearDebugMessage() {
        _debugMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Penutupan interpreter diserahkan ke siklus hidup Singleton,
        // Namun jika ingin tetap aman dari memory leak, panggil close di level aplikasi utama.
    }
}
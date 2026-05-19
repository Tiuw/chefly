package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.model.DetectedIngredient
import com.skripsi.chefly.ml.YOLO26Detector // Mengacu pada detector utama yang sudah aman dan stabil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel untuk CameraScreen
 * Menangani inferensi real-time Edge AI YOLO26 NMS-Free dan distribusi state deteksi ke UI
 */
class CameraViewModel : ViewModel() {

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
    private var detector: YOLO26Detector? = null

    fun initializeDetector(context: Context) {
        if (detector != null) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Parsing labels.txt murni dari asset tanpa hardcoded cadangan defaultLabels
                val labels = context.assets.open("labels.txt").bufferedReader().use { it.readText() }
                    .split('\n')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (labels.isEmpty()) {
                    throw IllegalStateException("Berkas labels.txt kosong atau tidak valid!")
                }

                withContext(Dispatchers.Main) {
                    detector = YOLO26Detector(context, "yolo26s_float32.tflite", labels, false)
                    Log.i(TAG, "✅ YOLO26Detector sukses diinisialisasi dengan ${labels.size} kelas.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Gagal memuat komponen label atau model .tflite: ${e.message}", e)
            }
        }
    }

    fun processCameraFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                Log.d("Chefly_Debug", "📸 Frame diterima oleh ViewModel. Ukuran: ${bitmap.width}x${bitmap.height}")

                // Nilai threshold diatur standar 0.40f (40%) untuk menjaga akurasi sidang
                val results = detector?.detectObjects(bitmap, 0.60f) ?: emptyList()

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
                val results = detector?.detectObjects(bitmap, 0.60f) ?: emptyList()
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
                val results = detector?.detectObjects(bitmap, 0.35f) ?: emptyList()
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
        detector?.close()
    }
}
package com.skripsi.chefly.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skripsi.chefly.data.model.DetectedIngredient
import com.skripsi.chefly.ml.YOLOv8sDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for CameraScreen
 * Handles real-time ingredient detection, image upload, and detection results
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

    private var detector: YOLOv8sDetector? = null
    private val TAG = "CameraViewModel"

    fun initializeDetector(context: Context) {
        try {
            if (detector == null) {
                // Load labels from assets or use defaults
                val defaultLabels = listOf(
                    "Ayam", "Bawang Merah", "Bawang Putih", "Bayam", "Cabai Hijau", "Cabai Merah",
                    "Daging Kambing", "Daging Sapi", "Daun Bawang", "Ikan", "Kacang Panjang", "Kangkung",
                    "Kol", "Nasi", "Tahu", "Telur", "Tempe", "Terong", "Tomat", "Udang", "Wortel"
                )

                val labels = try {
                    val stream = context.assets.open("labels.txt")
                    val text = stream.bufferedReader().use { it.readText() }
                    val parsed = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                    if (parsed.isEmpty()) defaultLabels else parsed
                } catch (_: Exception) {
                    defaultLabels
                }

                detector = YOLOv8sDetector(context, "yolov8s.tflite", labels, useNNAPI = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing detector: ${e.message}", e)
        }
    }

    fun updateCameraDetections(detections: List<DetectedIngredient>) {
        _detections.value = detections
    }

    fun setUploadedImage(bitmap: Bitmap) {
        _uploadedImage.value = bitmap
    }

    fun processUploadedImage(bitmap: Bitmap) {
        _isProcessingImage.value = true
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val detections = detector?.detectObjects(bitmap, 0.35f) ?: emptyList()
                val mapped = detections.map { d ->
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
                Log.e(TAG, "Error processing image: ${e.message}", e)
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
                val detections = detector?.detectObjects(bitmap, 0.35f) ?: emptyList()
                val t1 = System.currentTimeMillis()

                val mapped = detections.map { d ->
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


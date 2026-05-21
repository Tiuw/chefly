package com.skripsi.chefly.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.RectF
import android.util.Log
import androidx.core.graphics.scale
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class YOLO26Detector(

    context: Context,
    modelFilename: String,
    private val detectionClasses: List<String>,
    private val useNNAPI: Boolean = false
) {
    companion object {
        private const val TAG = "YOLO26_DEBUG"
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.4f
        private const val MAX_TOTAL_DETECTIONS = 10
        private const val MIN_BOX_SIZE = 20f
        private const val INPUT_SIZE = 640
    }

    private var interpreter: Interpreter? = null
    private var isModelQuantized = false
    private var numBoxes = 0
    private var attrCount = 0
    private var inputShape: IntArray? = null

    // REVISI OPTIMASI MEMORI: Alokasikan wadah secara global sekali saja di awal
    private lateinit var imgData: ByteBuffer
    private lateinit var intValues: IntArray
    private lateinit var paddedBitmap: Bitmap
    private lateinit var canvas: Canvas

    @Volatile
    private var isProcessing = false
    private var frameCount = 0

    init {
        try {
            val modelFile = FileUtil.loadMappedFile(context, modelFilename)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(useNNAPI)
                setUseXNNPACK(true) // REVISI: Aktifkan XNNPACK untuk akselerasi CPU ARM di HP Android
            }
            interpreter = Interpreter(modelFile, options)

            val inputTensor = interpreter!!.getInputTensor(0)
            inputShape = inputTensor.shape()
            val inputDataType = inputTensor.dataType()

            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            numBoxes = outputShape[1]
            attrCount = outputShape[2]
            isModelQuantized = outputTensor.dataType() == DataType.UINT8

            // REVISI INSIALISASI MEMORI GLOBAL
            val bytesPerChannel = if (isModelQuantized) 1 else 4
            imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * bytesPerChannel).apply {
                order(ByteOrder.nativeOrder())
            }
            intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            paddedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888)
            canvas = Canvas(paddedBitmap)

            Log.i(TAG, "╔════════════════════════════════════════════════════════════╗")
            Log.i(TAG, "║  YOLO26 DETECTOR INITIALIZED SUCCESSFULLY                  ║")
            Log.i(TAG, "╠════════════════════════════════════════════════════════════╣")
            Log.i(TAG, "║  Input Shape: ${inputShape?.joinToString(" x ")}")
            Log.i(TAG, "║  Output Shape: ${outputShape.joinToString(" x ")}")
            Log.i(TAG, "║  Num Boxes: $numBoxes | Attr Per Box: $attrCount")
            Log.i(TAG, "╚════════════════════════════════════════════════════════════╝")

        } catch (e: Exception) {
            Log.e(TAG, "❌ FATAL: Gagal memuat model: ${e.message}", e)
        }
    }

    fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): List<DetectionCamera> {
        val tfliteInterpreter = interpreter ?: return emptyList()
        if (isProcessing) return emptyList()

        frameCount++
        val shouldLog = frameCount % 30 == 1

        try {
            isProcessing = true

            // --- 1. PRE-PROCESSING ---
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            val scale = minOf(INPUT_SIZE.toFloat() / originalWidth, INPUT_SIZE.toFloat() / originalHeight)
            val newWidth = (originalWidth * scale).toInt()
            val newHeight = (originalHeight * scale).toInt()

            val resizedBitmap = bitmap.scale(newWidth, newHeight, false)

            // Gunakan canvas global yang sudah di-init di awal (Hemat Alokasi Memori)
            canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))
            val padX = (INPUT_SIZE - newWidth) / 2f
            val padY = (INPUT_SIZE - newHeight) / 2f
            canvas.drawBitmap(resizedBitmap, padX, padY, null)

            // Reset byte buffer global sebelum diisi ulang
            imgData.rewind()
            paddedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            for (pixel in intValues) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                if (isModelQuantized) {
                    imgData.put(r.toByte()).put(g.toByte()).put(b.toByte())
                } else {
                    // Normalisasi [0.0, 1.0] sesuai metadata standar YOLO yang kita set sebelumnya
                    imgData.putFloat(r / 255f).putFloat(g / 255f).putFloat(b / 255f)

                    // CATATAN: Jika warna deteksi terbalik/kurang akurat, ganti baris di atas dengan format BGR:
                    // imgData.putFloat(b / 255f).putFloat(g / 255f).putFloat(r / 255f)
                }
            }
            imgData.rewind()

            // --- 2. INFERENCE ---
            val t0 = System.currentTimeMillis()
            val flatOut = Array(1) { Array(numBoxes) { FloatArray(attrCount) } }
            val outputs = mutableMapOf<Int, Any>(0 to flatOut)

            tfliteInterpreter.runForMultipleInputsOutputs(arrayOf(imgData), outputs)
            val t1 = System.currentTimeMillis()

            if (shouldLog) {
                Log.d(TAG, "⚡ INFERENCE SPEED: ${t1 - t0}ms")
            }

            // --- 3. POST-PROCESSING ---
            val detections = mutableListOf<DetectionCamera>()
            val maxClasses = detectionClasses.size

            fun sigmoid(x: Float): Float = 1.0f / (1.0f + Math.exp(-x.toDouble()).toFloat())

            for (b in 0 until numBoxes) {
                val xMinNormalized = flatOut[0][b][0]
                val yMinNormalized = flatOut[0][b][1]
                val xMaxNormalized = flatOut[0][b][2]
                val yMaxNormalized = flatOut[0][b][3]

                val leftBox = xMinNormalized * INPUT_SIZE.toFloat()
                val topBox = yMinNormalized * INPUT_SIZE.toFloat()
                val rightBox = xMaxNormalized * INPUT_SIZE.toFloat()
                val bottomBox = yMaxNormalized * INPUT_SIZE.toFloat()

                val w = rightBox - leftBox
                val h = bottomBox - topBox
                if (w <= 0f || h <= 0f) continue

                // REVISI PENANGANAN LOGITS: Konversi skor keyakinan dari raw logits ke probabilitas
                val rawConfidence = flatOut[0][b][4]
                val confidence = if (isModelQuantized) rawConfidence else sigmoid(rawConfidence)

                if (confidence < confidenceThreshold) continue

                // Penentuan Class ID
                val maxClassIdx = flatOut[0][b][5].toInt()
                if (maxClassIdx < 0 || maxClassIdx >= maxClasses) continue

                // Transformasi koordinat kembali ke resolusi asli layar HP
                val normLeft = (leftBox / INPUT_SIZE.toFloat()).coerceIn(0f, 1f)
                val normTop = (topBox / INPUT_SIZE.toFloat()).coerceIn(0f, 1f)
                val normRight = (rightBox / INPUT_SIZE.toFloat()).coerceIn(0f, 1f)
                val normBottom = (bottomBox / INPUT_SIZE.toFloat()).coerceIn(0f, 1f)

                detections.add(
                    DetectionCamera(
                        // Masukkan koordinat rasio [0.0 - 1.0] yang bersih
                        box = RectF(normLeft, normTop, normRight, normBottom),
                        confidence = confidence,
                        classIndex = maxClassIdx,
                        className = detectionClasses[maxClassIdx]
                    )
                )
            }

            return detections.sortedByDescending { it.confidence }.take(MAX_TOTAL_DETECTIONS)

        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION during detection: ${e.message}", e)
            return emptyList()
        } finally {
            isProcessing = false
        }
    }

    fun close() {
        interpreter?.close()
        if (::paddedBitmap.isInitialized) paddedBitmap.recycle()
        Log.i(TAG, "🔒 Detector closed safely.")
    }
}
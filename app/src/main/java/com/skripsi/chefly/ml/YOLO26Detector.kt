package com.skripsi.chefly.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class YOLO26Detector(
    context: Context,
    modelFilename: String,
    private val detectionClasses: List<String>,
    useNNAPI: Boolean = false
) {

    companion object {
        private const val TAG = "YOLO26_DETECTION"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.50f
        private const val MAX_DETECTIONS = 10
    }

    private var interpreter: Interpreter? = null
    private val isModelQuantized: Boolean
    private val imgData: ByteBuffer
    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

    @Volatile
    private var isProcessing = false

    init {
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(useNNAPI)
            setUseXNNPACK(true)
        }
        val modelFile = FileUtil.loadMappedFile(context.applicationContext, modelFilename)
        val tflite = Interpreter(modelFile, options)
        interpreter = tflite

        val inputTensor = tflite.getInputTensor(0)
        isModelQuantized = inputTensor.dataType() == DataType.UINT8
        val bytesPerChannel = if (isModelQuantized) 1 else 4

        imgData = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * bytesPerChannel).apply {
            order(ByteOrder.nativeOrder())
        }
    }

    /**
     * Inferensi objek menggunakan model YOLO26 End-to-End (NMS-Free).
     */
    fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float = CONFIDENCE_THRESHOLD
    ): List<DetectionCamera> {
        val tflite = interpreter ?: return emptyList()
        if (isProcessing) return emptyList()

        try {
            isProcessing = true
            val origW = bitmap.width
            val origH = bitmap.height

            // 1. Letterboxing Canvas 640x640 dengan warna background 114 (Gray)
            val scale = min(INPUT_SIZE.toFloat() / origW, INPUT_SIZE.toFloat() / origH)
            val newW = (origW * scale).toInt()
            val newH = (origH * scale).toInt()
            val padX = (INPUT_SIZE - newW) / 2f
            val padY = (INPUT_SIZE - newH) / 2f

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            val letterboxedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(letterboxedBitmap)
            canvas.drawColor(Color.rgb(114, 114, 114))
            canvas.drawBitmap(scaledBitmap, padX, padY, Paint(Paint.FILTER_BITMAP_FLAG))

            // 2. Normalisasi & Pengisian Buffer Input
            letterboxedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            imgData.rewind()

            for (pixel in intValues) {
                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF

                if (isModelQuantized) {
                    imgData.put(r.toByte())
                    imgData.put(g.toByte())
                    imgData.put(b.toByte())
                } else {
                    imgData.putFloat(r / 255.0f)
                    imgData.putFloat(g / 255.0f)
                    imgData.putFloat(b / 255.0f)
                }
            }

            // 3. Eksekusi Inferensi TFLite
            val outputTensor = tflite.getOutputTensor(0)
            val shape = outputTensor.shape() // [1, 300, 6] untuk YOLO End-to-End NMS-Free
            val flatOut = Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
            val outputs = mutableMapOf<Int, Any>(0 to flatOut)

            tflite.runForMultipleInputsOutputs(arrayOf(imgData), outputs)

            // 4. Parsing Hasil Deteksi & Unpadding Koordinat
            val detections = mutableListOf<DetectionCamera>()
            val isNmsFree = shape.size >= 3 && (shape[2] == 6 || shape[2] == 7)

            if (isNmsFree) {
                val totalBoxes = shape[1]
                for (b in 0 until totalBoxes) {
                    val box = flatOut[0][b]
                    val score = box[4]
                    val clsId = box[5].toInt()

                    if (score < confidenceThreshold || clsId !in detectionClasses.indices) continue

                    val left = ((box[0] * INPUT_SIZE - padX) / scale).coerceIn(0f, origW.toFloat()) / origW
                    val top = ((box[1] * INPUT_SIZE - padY) / scale).coerceIn(0f, origH.toFloat()) / origH
                    val right = ((box[2] * INPUT_SIZE - padX) / scale).coerceIn(0f, origW.toFloat()) / origW
                    val bottom = ((box[3] * INPUT_SIZE - padY) / scale).coerceIn(0f, origH.toFloat()) / origH

                    if (right > left && bottom > top) {
                        detections.add(
                            DetectionCamera(
                                box = RectF(left, top, right, bottom),
                                confidence = score,
                                classIndex = clsId,
                                className = detectionClasses[clsId]
                            )
                        )
                    }
                }
            }

            val finalResults = detections.sortedByDescending { it.confidence }.take(MAX_DETECTIONS)

            // 🟢 LOGCAT RINGKAS
            Log.d(TAG, "Ditemukan ${finalResults.size} objek terdeteksi:")
            finalResults.forEachIndexed { i, d ->
                Log.d(TAG, "#${i + 1} Kelas: ${d.className} | Confidence: ${"%.2f".format(d.confidence * 100)}%")
            }

            return finalResults
        } catch (e: Exception) {
            Log.e(TAG, "Gagal saat deteksi: ${e.message}", e)
            return emptyList()
        } finally {
            isProcessing = false
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
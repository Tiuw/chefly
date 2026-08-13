package com.skripsi.chefly.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
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
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.50f
        private const val MAX_TOTAL_DETECTIONS = 10
        private const val INPUT_SIZE = 640
    }

    private var interpreter: Interpreter? = null
    private var isModelQuantized = false
    private var inputShape: IntArray? = null

    // Alokasi memori global untuk efisiensi Garbage Collection Android
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
                setUseXNNPACK(true)
            }
            interpreter = Interpreter(modelFile, options)

            val inputTensor = interpreter!!.getInputTensor(0)
            inputShape = inputTensor.shape()

            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputShape = outputTensor.shape()

            // Tipe data numerik yang dicocokkan saat menulis piksel adalah format INPUT tensor
            isModelQuantized = inputTensor.dataType() == DataType.UINT8

            val bytesPerChannel = if (isModelQuantized) 1 else 4
            imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * bytesPerChannel).apply {
                order(ByteOrder.nativeOrder())
            }
            intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
            paddedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888)
            canvas = Canvas(paddedBitmap)

            Log.i(
                TAG,
                "✅ YOLO26 Detector Ready. Input: ${inputShape?.joinToString("x")} (dtype=${inputTensor.dataType()}), " +
                        "Output: ${outputShape.joinToString("x")} (dtype=${outputTensor.dataType()}), " +
                        "InputQuantized: $isModelQuantized"
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ FATAL: Gagal memuat model YOLO26: ${e.message}", e)
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

            // --- 1. PRE-PROCESSING (Letterboxing 640x640 dengan Canvas Putih) ---
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height
            val scale = minOf(INPUT_SIZE.toFloat() / originalWidth, INPUT_SIZE.toFloat() / originalHeight)
            val newWidth = (originalWidth * scale).toInt()
            val newHeight = (originalHeight * scale).toInt()

            val resizedBitmap = bitmap.scale(newWidth, newHeight, false)

            // Canvas Putih mencegah gambar transparan PNG ber-alpha ter-render hitam pekat
            canvas.drawColor(Color.WHITE)
            val padX = (INPUT_SIZE - newWidth) / 2f
            val padY = (INPUT_SIZE - newHeight) / 2f
            canvas.drawBitmap(resizedBitmap, padX, padY, null)

            imgData.rewind()
            paddedBitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

            for (pixel in intValues) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                if (isModelQuantized) {
                    imgData.put(r.toByte()).put(g.toByte()).put(b.toByte())
                } else {
                    // Normalisasi standar [0.0f, 1.0f]
                    imgData.putFloat(r / 255.0f)
                    imgData.putFloat(g / 255.0f)
                    imgData.putFloat(b / 255.0f)
                }
            }
            imgData.rewind()

            // --- 2. INFERENCE ---
            val t0 = System.currentTimeMillis()
            val outputTensor = tfliteInterpreter.getOutputTensor(0)
            val shape = outputTensor.shape() // [1, 300, 6] atau [1, 84, 8400]

            val flatOut = Array(shape[0]) { Array(shape[1]) { FloatArray(shape[2]) } }
            val outputs = mutableMapOf<Int, Any>(0 to flatOut)

            tfliteInterpreter.runForMultipleInputsOutputs(arrayOf(imgData), outputs)
            val t1 = System.currentTimeMillis()

            if (shouldLog) {
                Log.d(TAG, "⚡ INFERENCE SPEED YOLO26: ${t1 - t0}ms")
            }

            // --- 3. POST-PROCESSING SAFE PARSER ---
            val detections = mutableListOf<DetectionCamera>()
            val maxClasses = detectionClasses.size

            // Deteksi otomatis format output End-to-End NMS-Free (Atribut Per Box = 6 atau 7)
            val isNmsFreeEndToEnd = (shape[2] == 6 || shape[2] == 7)

            var globalMaxScore = -1f
            var globalMaxClsId = -1

            if (isNmsFreeEndToEnd) {
                // FORMAT YOLO26 NMS-FREE END-TO-END [1, 300, 6]
                val totalBoxes = shape[1]
                val inputSizeFloat = INPUT_SIZE.toFloat()

                for (b in 0 until totalBoxes) {
                    val boxArray = flatOut[0][b]

                    val x1 = boxArray[0]
                    val y1 = boxArray[1]
                    val x2 = boxArray[2]
                    val y2 = boxArray[3]

                    val score = boxArray[4]
                    val clsId = boxArray[5].toInt()

                    if (score > globalMaxScore) {
                        globalMaxScore = score
                        globalMaxClsId = clsId
                    }

                    if (score < confidenceThreshold) continue
                    if (clsId < 0 || clsId >= maxClasses) continue

                    // 🟢 REVISI EKSPLISIT: Konversi langsung dari rasio [0.0, 1.0] ke matriks piksel [0, 640]
                    val leftBox = x1 * inputSizeFloat
                    val topBox = y1 * inputSizeFloat
                    val rightBox = x2 * inputSizeFloat
                    val bottomBox = y2 * inputSizeFloat

                    // Eliminasi offset padding letterboxing
                    val unpadLeft = ((leftBox - padX) / scale).coerceIn(0f, originalWidth.toFloat())
                    val unpadTop = ((topBox - padY) / scale).coerceIn(0f, originalHeight.toFloat())
                    val unpadRight = ((rightBox - padX) / scale).coerceIn(0f, originalWidth.toFloat())
                    val unpadBottom = ((bottomBox - padY) / scale).coerceIn(0f, originalHeight.toFloat())

                    val normLeft = unpadLeft / originalWidth.toFloat()
                    val normTop = unpadTop / originalHeight.toFloat()
                    val normRight = unpadRight / originalWidth.toFloat()
                    val normBottom = unpadBottom / originalHeight.toFloat()

                    if (normRight <= normLeft || normBottom <= normTop) continue

                    detections.add(
                        DetectionCamera(
                            box = RectF(normLeft, normTop, normRight, normBottom),
                            confidence = score,
                            classIndex = clsId,
                            className = detectionClasses[clsId]
                        )
                    )
                }
            } else {
                // FORMAT CONVENTIONAL YOLO [1, 84, 8400]
                val numAnchors = shape[2]
                for (i in 0 until numAnchors) {
                    val cx = flatOut[0][0][i]
                    val cy = flatOut[0][1][i]
                    val w = flatOut[0][2][i]
                    val h = flatOut[0][3][i]

                    var maxScore = 0f
                    var maxClass = -1

                    for (c in 0 until maxClasses) {
                        val score = flatOut[0][4 + c][i]
                        if (score > maxScore) {
                            maxScore = score
                            maxClass = c
                        }
                    }

                    if (maxScore > globalMaxScore) {
                        globalMaxScore = maxScore
                        globalMaxClsId = maxClass
                    }

                    if (maxScore >= confidenceThreshold && maxClass != -1) {
                        val leftBox = cx - (w / 2f)
                        val topBox = cy - (h / 2f)
                        val rightBox = cx + (w / 2f)
                        val bottomBox = cy + (h / 2f)

                        val unpadLeft = ((leftBox - padX) / scale).coerceIn(0f, originalWidth.toFloat())
                        val unpadTop = ((topBox - padY) / scale).coerceIn(0f, originalHeight.toFloat())
                        val unpadRight = ((rightBox - padX) / scale).coerceIn(0f, originalWidth.toFloat())
                        val unpadBottom = ((bottomBox - padY) / scale).coerceIn(0f, originalHeight.toFloat())

                        detections.add(
                            DetectionCamera(
                                box = RectF(
                                    unpadLeft / originalWidth.toFloat(),
                                    unpadTop / originalHeight.toFloat(),
                                    unpadRight / originalWidth.toFloat(),
                                    unpadBottom / originalHeight.toFloat()
                                ),
                                confidence = maxScore,
                                classIndex = maxClass,
                                className = detectionClasses[maxClass]
                            )
                        )
                    }
                }
            }

            val maxClassName = if (globalMaxClsId in detectionClasses.indices) detectionClasses[globalMaxClsId] else "N/A"
            Log.d(
                TAG,
                "🔍 Ditemukan ${detections.size} objek lolos threshold ($confidenceThreshold). " +
                        "Skor tertinggi mentah frame ini: $globalMaxScore (class=$maxClassName)"
            )
            return detections.sortedByDescending { it.confidence }.take(MAX_TOTAL_DETECTIONS)

        } catch (e: Exception) {
            Log.e(TAG, "❌ EXCEPTION during YOLO26 detection: ${e.message}", e)
            return emptyList()
        } finally {
            isProcessing = false
        }
    }

    fun close() {
        interpreter?.close()
        if (::paddedBitmap.isInitialized) paddedBitmap.recycle()
        Log.i(TAG, "🔒 YOLO26 Detector closed safely.")
    }
}
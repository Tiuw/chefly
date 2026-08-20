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

        // Hanya tampilkan 20 prediction tertinggi di Logcat
        private const val DEBUG_TOP_K = 20
    }

    private var interpreter: Interpreter? = null

    private val isModelQuantized: Boolean

    private val imgData: ByteBuffer

    private val intValues =
        IntArray(INPUT_SIZE * INPUT_SIZE)

    @Volatile
    private var isProcessing = false

    init {

        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(useNNAPI)
            setUseXNNPACK(true)
        }

        val modelFile =
            FileUtil.loadMappedFile(
                context.applicationContext,
                modelFilename
            )

        val tflite =
            Interpreter(
                modelFile,
                options
            )

        interpreter = tflite

        val inputTensor =
            tflite.getInputTensor(0)

        isModelQuantized =
            inputTensor.dataType() == DataType.UINT8

        val bytesPerChannel =
            if (isModelQuantized) {
                1
            } else {
                4
            }

        imgData =
            ByteBuffer.allocateDirect(
                INPUT_SIZE *
                        INPUT_SIZE *
                        3 *
                        bytesPerChannel
            ).apply {
                order(ByteOrder.nativeOrder())
            }

        // ================================================================
        // INITIALIZATION LOG
        // ================================================================

        Log.d(TAG, "========================================")
        Log.d(TAG, "YOLO26 DETECTOR INITIALIZED")
        Log.d(TAG, "Model: $modelFilename")
        Log.d(
            TAG,
            "Input shape: ${
                inputTensor.shape().contentToString()
            }"
        )
        Log.d(
            TAG,
            "Input type: ${inputTensor.dataType()}"
        )
        Log.d(
            TAG,
            "Model quantized: $isModelQuantized"
        )
        Log.d(
            TAG,
            "Jumlah class: ${detectionClasses.size}"
        )
        Log.d(
            TAG,
            "Classes: $detectionClasses"
        )
        Log.d(
            TAG,
            "Confidence threshold: $CONFIDENCE_THRESHOLD"
        )
        Log.d(
            TAG,
            "Debug Top-K: $DEBUG_TOP_K"
        )
        Log.d(TAG, "========================================")
    }

    /**
     * Inferensi objek menggunakan YOLO26 End-to-End / NMS-Free.
     *
     * Output model:
     *
     * [1, 300, 6]
     *
     * Setiap prediction:
     *
     * [x1, y1, x2, y2, confidence, class_id]
     */
    fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float = CONFIDENCE_THRESHOLD
    ): List<DetectionCamera> {

        val tflite =
            interpreter ?: return emptyList()

        if (isProcessing) {

            Log.w(
                TAG,
                "Deteksi sedang diproses. Request diabaikan."
            )

            return emptyList()
        }

        try {

            isProcessing = true

            val origW =
                bitmap.width

            val origH =
                bitmap.height

            // ============================================================
            // START DETECTION
            // ============================================================

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "START YOLO26 DETECTION")
            Log.d(
                TAG,
                "Original image: ${origW}x${origH}"
            )
            Log.d(
                TAG,
                "Confidence threshold: $confidenceThreshold"
            )
            Log.d(TAG, "========================================")

            // ============================================================
            // 1. LETTERBOX
            // ============================================================

            val scale =
                min(
                    INPUT_SIZE.toFloat() / origW,
                    INPUT_SIZE.toFloat() / origH
                )

            val newW =
                (origW * scale).toInt()

            val newH =
                (origH * scale).toInt()

            val padX =
                (INPUT_SIZE - newW) / 2f

            val padY =
                (INPUT_SIZE - newH) / 2f

            Log.d(
                TAG,
                "Letterbox scale: $scale"
            )

            Log.d(
                TAG,
                "Resized image: ${newW}x${newH}"
            )

            Log.d(
                TAG,
                "Padding X: $padX"
            )

            Log.d(
                TAG,
                "Padding Y: $padY"
            )

            val scaledBitmap =
                Bitmap.createScaledBitmap(
                    bitmap,
                    newW,
                    newH,
                    true
                )

            val letterboxedBitmap =
                Bitmap.createBitmap(
                    INPUT_SIZE,
                    INPUT_SIZE,
                    Bitmap.Config.ARGB_8888
                )

            val canvas =
                Canvas(letterboxedBitmap)

            // Letterbox background = RGB 114
            canvas.drawColor(
                Color.rgb(
                    114,
                    114,
                    114
                )
            )

            canvas.drawBitmap(
                scaledBitmap,
                padX,
                padY,
                Paint(
                    Paint.FILTER_BITMAP_FLAG
                )
            )

            // ============================================================
            // 2. INPUT BUFFER
            // ============================================================

            letterboxedBitmap.getPixels(
                intValues,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
            )

            imgData.rewind()

            for (pixel in intValues) {

                val r =
                    (pixel ushr 16) and 0xFF

                val g =
                    (pixel ushr 8) and 0xFF

                val b =
                    pixel and 0xFF

                if (isModelQuantized) {

                    imgData.put(
                        r.toByte()
                    )

                    imgData.put(
                        g.toByte()
                    )

                    imgData.put(
                        b.toByte()
                    )

                } else {

                    imgData.putFloat(
                        r / 255.0f
                    )

                    imgData.putFloat(
                        g / 255.0f
                    )

                    imgData.putFloat(
                        b / 255.0f
                    )
                }
            }

            // ============================================================
            // 3. OUTPUT TENSOR
            // ============================================================

            val outputTensor =
                tflite.getOutputTensor(0)

            val shape =
                outputTensor.shape()

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "OUTPUT TENSOR")
            Log.d(
                TAG,
                "Output shape: ${shape.contentToString()}"
            )
            Log.d(
                TAG,
                "Output type: ${outputTensor.dataType()}"
            )
            Log.d(TAG, "========================================")

            /*
             * YOLO26 End-to-End NMS-Free:
             *
             * [1, 300, 6]
             *
             * [x1, y1, x2, y2, confidence, class_id]
             */

            val flatOut =
                Array(shape[0]) {

                    Array(shape[1]) {

                        FloatArray(
                            shape[2]
                        )
                    }
                }

            val outputs =
                mutableMapOf<Int, Any>(
                    0 to flatOut
                )

            // ============================================================
            // 4. INFERENCE
            // ============================================================

            val inferenceStart =
                System.currentTimeMillis()

            tflite.runForMultipleInputsOutputs(
                arrayOf(imgData),
                outputs
            )

            val inferenceTime =
                System.currentTimeMillis() -
                        inferenceStart

            Log.d(
                TAG,
                "Inference time: ${inferenceTime} ms"
            )

            // ============================================================
            // 5. VALIDATE OUTPUT FORMAT
            // ============================================================

            val isNmsFree =
                shape.size >= 3 &&
                        (
                                shape[2] == 6 ||
                                        shape[2] == 7
                                )

            if (!isNmsFree) {

                Log.e(
                    TAG,
                    "FORMAT OUTPUT TIDAK DIKENALI!"
                )

                Log.e(
                    TAG,
                    "Shape: ${shape.contentToString()}"
                )

                return emptyList()
            }

            val totalBoxes =
                shape[1]

            // ============================================================
            // 6. AMBIL SEMUA RAW PREDICTION
            // ============================================================

            data class RawPrediction(
                val index: Int,
                val confidence: Float,
                val classId: Int,
                val className: String,
                val x1: Float,
                val y1: Float,
                val x2: Float,
                val y2: Float
            )

            val rawPredictions =
                mutableListOf<RawPrediction>()

            for (b in 0 until totalBoxes) {

                val box =
                    flatOut[0][b]

                val x1 =
                    box[0]

                val y1 =
                    box[1]

                val x2 =
                    box[2]

                val y2 =
                    box[3]

                val score =
                    box[4]

                val clsId =
                    box[5].toInt()

                val className =
                    if (
                        clsId in
                        detectionClasses.indices
                    ) {

                        detectionClasses[clsId]

                    } else {

                        "UNKNOWN_CLASS_$clsId"
                    }

                rawPredictions.add(
                    RawPrediction(
                        index = b + 1,
                        confidence = score,
                        classId = clsId,
                        className = className,
                        x1 = x1,
                        y1 = y1,
                        x2 = x2,
                        y2 = y2
                    )
                )
            }

            // ============================================================
            // 7. TOP-20 RAW PREDICTION
            // ============================================================

            val topPredictions =
                rawPredictions
                    .sortedByDescending {
                        it.confidence
                    }
                    .take(DEBUG_TOP_K)

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "TOP-$DEBUG_TOP_K RAW PREDICTIONS")
            Log.d(TAG, "Total raw output: $totalBoxes")
            Log.d(TAG, "========================================")

            topPredictions.forEachIndexed {
                    rank,
                    prediction ->

                Log.d(
                    TAG,
                    "#${rank + 1} | " +
                            "RAW=${prediction.index} | " +
                            "ClassID=${prediction.classId} | " +
                            "Class=${prediction.className} | " +
                            "Confidence=${
                                "%.4f".format(
                                    prediction.confidence
                                )
                            } " +
                            "(${
                                "%.2f".format(
                                    prediction.confidence * 100
                                )
                            }%) | " +
                            "Box=[" +
                            "${"%.4f".format(prediction.x1)}, " +
                            "${"%.4f".format(prediction.y1)}, " +
                            "${"%.4f".format(prediction.x2)}, " +
                            "${"%.4f".format(prediction.y2)}]"
                )
            }

            Log.d(TAG, "========================================")

            // ============================================================
            // 8. HITUNG BERAPA PREDICTION DI ATAS THRESHOLD
            // ============================================================

            val aboveThreshold =
                rawPredictions.filter {
                    it.confidence >= confidenceThreshold
                }

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(
                TAG,
                "PREDICTION DI ATAS THRESHOLD"
            )
            Log.d(
                TAG,
                "Threshold: ${
                    "%.2f".format(
                        confidenceThreshold * 100
                    )
                }%"
            )
            Log.d(
                TAG,
                "Jumlah: ${aboveThreshold.size}"
            )
            Log.d(TAG, "========================================")

            aboveThreshold
                .sortedByDescending {
                    it.confidence
                }
                .forEachIndexed {
                        index,
                        prediction ->

                    Log.d(
                        TAG,
                        "#${index + 1} | " +
                                "Class=${prediction.className} | " +
                                "Confidence=${
                                    "%.2f".format(
                                        prediction.confidence * 100
                                    )
                                }%"
                    )
                }

            // ============================================================
            // 9. KONVERSI KE DetectionCamera
            // ============================================================

            val detections =
                mutableListOf<DetectionCamera>()

            for (prediction in aboveThreshold) {

                val clsId =
                    prediction.classId

                if (
                    clsId !in
                    detectionClasses.indices
                ) {

                    Log.w(
                        TAG,
                        "ClassID tidak valid: $clsId"
                    )

                    continue
                }

                // ========================================================
                // UNLETTERBOX
                // ========================================================

                val left =
                    (
                            (
                                    prediction.x1 *
                                            INPUT_SIZE -
                                            padX
                                    ) / scale
                            )
                        .coerceIn(
                            0f,
                            origW.toFloat()
                        ) / origW

                val top =
                    (
                            (
                                    prediction.y1 *
                                            INPUT_SIZE -
                                            padY
                                    ) / scale
                            )
                        .coerceIn(
                            0f,
                            origH.toFloat()
                        ) / origH

                val right =
                    (
                            (
                                    prediction.x2 *
                                            INPUT_SIZE -
                                            padX
                                    ) / scale
                            )
                        .coerceIn(
                            0f,
                            origW.toFloat()
                        ) / origW

                val bottom =
                    (
                            (
                                    prediction.y2 *
                                            INPUT_SIZE -
                                            padY
                                    ) / scale
                            )
                        .coerceIn(
                            0f,
                            origH.toFloat()
                        ) / origH

                // ========================================================
                // VALIDASI BOX
                // ========================================================

                if (
                    right > left &&
                    bottom > top
                ) {

                    detections.add(
                        DetectionCamera(
                            box = RectF(
                                left,
                                top,
                                right,
                                bottom
                            ),
                            confidence =
                                prediction.confidence,
                            classIndex =
                                clsId,
                            className =
                                detectionClasses[clsId]
                        )
                    )
                }
            }

            // ============================================================
            // 10. SORT DAN LIMIT
            // ============================================================

            val finalResults =
                detections
                    .sortedByDescending {
                        it.confidence
                    }
                    .take(MAX_DETECTIONS)

            // ============================================================
            // 11. FINAL RESULT
            // ============================================================

            Log.d(TAG, "")
            Log.d(TAG, "========================================")
            Log.d(TAG, "FINAL DETECTION RESULT")
            Log.d(
                TAG,
                "Jumlah detection: ${finalResults.size}"
            )
            Log.d(TAG, "========================================")

            if (finalResults.isEmpty()) {

                Log.d(
                    TAG,
                    "Tidak ada objek yang melewati threshold."
                )

            } else {

                finalResults.forEachIndexed {
                        index,
                        detection ->

                    Log.d(
                        TAG,
                        "#${index + 1} | " +
                                "Kelas=${detection.className} | " +
                                "ClassID=${detection.classIndex} | " +
                                "Confidence=${
                                    "%.2f".format(
                                        detection.confidence * 100
                                    )
                                }% | " +
                                "Box=[" +
                                "${"%.3f".format(detection.box.left)}, " +
                                "${"%.3f".format(detection.box.top)}, " +
                                "${"%.3f".format(detection.box.right)}, " +
                                "${"%.3f".format(detection.box.bottom)}]"
                    )
                }
            }

            Log.d(TAG, "========================================")
            Log.d(TAG, "END DETECTION")
            Log.d(TAG, "")

            return finalResults

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Gagal saat deteksi: ${e.message}",
                e
            )

            return emptyList()

        } finally {

            isProcessing = false
        }
    }

    /**
     * Menutup TensorFlow Lite Interpreter.
     */
    fun close() {

        interpreter?.close()

        interpreter = null

        Log.d(
            TAG,
            "YOLO26 Interpreter ditutup."
        )
    }
}
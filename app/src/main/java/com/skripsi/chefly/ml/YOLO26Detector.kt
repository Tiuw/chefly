
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
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
        private const val DEFAULT_CONFIDENCE = 0.07f
        private const val NMS_IOU_THRESHOLD = 0.45f
        private const val MAX_DETECTIONS = 10
        private const val DEBUG_TOP_K = 20
    }

    private data class Candidate(
        val index: Int,
        val classId: Int,
        val score: Float,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    )

    private var interpreter: Interpreter? = null
    private var isClosed = false

    private lateinit var inputBuffer: ByteBuffer
    private lateinit var inputShape: IntArray

    private var inputType = DataType.FLOAT32
    private var inputIsNCHW = true
    private var inputScale = 0f
    private var inputZeroPoint = 0

    private var outputShape = intArrayOf()
    private var outputType = DataType.FLOAT32

    private val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

    init {
        require(detectionClasses.isNotEmpty()) {
            "detectionClasses tidak boleh kosong."
        }

        val options = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(useNNAPI)
            setUseXNNPACK(true)
        }

        try {
            val modelBuffer = FileUtil.loadMappedFile(
                context.applicationContext,
                modelFilename
            )

            val tflite = Interpreter(modelBuffer, options)
            interpreter = tflite

            val inputTensor = tflite.getInputTensor(0)
            inputShape = inputTensor.shape()
            inputType = inputTensor.dataType()

            require(inputShape.size == 4 && inputShape[0] == 1) {
                "Input model tidak didukung: ${inputShape.contentToString()}"
            }

            val isNCHW =
                inputShape[1] == 3 &&
                        inputShape[2] == INPUT_SIZE &&
                        inputShape[3] == INPUT_SIZE

            val isNHWC =
                inputShape[1] == INPUT_SIZE &&
                        inputShape[2] == INPUT_SIZE &&
                        inputShape[3] == 3

            require(isNCHW || isNHWC) {
                "Input harus [1,3,640,640] atau [1,640,640,3]. " +
                        "Ditemukan ${inputShape.contentToString()}"
            }

            inputIsNCHW = isNCHW

            val inputQuant = inputTensor.quantizationParams()
            inputScale = inputQuant.scale
            inputZeroPoint = inputQuant.zeroPoint

            val bytesPerElement = when (inputType) {
                DataType.FLOAT32 -> 4
                DataType.UINT8, DataType.INT8 -> 1
                else -> error("Tipe input tidak didukung: $inputType")
            }

            inputBuffer = ByteBuffer.allocateDirect(
                INPUT_SIZE * INPUT_SIZE * 3 * bytesPerElement
            ).order(ByteOrder.nativeOrder())

            val outputTensor = tflite.getOutputTensor(0)
            outputShape = outputTensor.shape()
            outputType = outputTensor.dataType()

            Log.d(TAG, "======================================")
            Log.d(TAG, "YOLO26 DETECTOR INITIALIZED")
            Log.d(TAG, "Model: $modelFilename")
            Log.d(TAG, "Input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Input type: $inputType")
            Log.d(TAG, "Input layout: ${if (inputIsNCHW) "NCHW" else "NHWC"}")
            Log.d(TAG, "Output shape: ${outputShape.contentToString()}")
            Log.d(TAG, "Output type: $outputType")
            Log.d(TAG, "Jumlah kelas: ${detectionClasses.size}")
            Log.d(TAG, "Classes: $detectionClasses")
            Log.d(TAG, "======================================")
        } catch (e: Exception) {
            interpreter?.close()
            interpreter = null
            throw IllegalStateException(
                "Gagal menginisialisasi YOLO26Detector: ${e.message}",
                e
            )
        }
    }

    @Synchronized
    fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE
    ): List<DetectionCamera> {
        val tflite = interpreter ?: return emptyList()

        if (isClosed || bitmap.width <= 0 || bitmap.height <= 0) {
            return emptyList()
        }

        var scaledBitmap: Bitmap? = null
        var letterboxBitmap: Bitmap? = null

        try {
            val originalWidth = bitmap.width
            val originalHeight = bitmap.height

            Log.d(TAG, "========== START DETECTION ==========")
            Log.d(TAG, "Image: ${originalWidth}x${originalHeight}")
            Log.d(TAG, "Confidence threshold: $confidenceThreshold")

            // 1. LETTERBOX
            val scale = min(
                INPUT_SIZE.toFloat() / originalWidth,
                INPUT_SIZE.toFloat() / originalHeight
            )

            val resizedWidth = (originalWidth * scale)
                .toInt()
                .coerceIn(1, INPUT_SIZE)

            val resizedHeight = (originalHeight * scale)
                .toInt()
                .coerceIn(1, INPUT_SIZE)

            val padX = (INPUT_SIZE - resizedWidth) / 2f
            val padY = (INPUT_SIZE - resizedHeight) / 2f

            scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                resizedWidth,
                resizedHeight,
                true
            )

            letterboxBitmap = Bitmap.createBitmap(
                INPUT_SIZE,
                INPUT_SIZE,
                Bitmap.Config.ARGB_8888
            )

            Canvas(letterboxBitmap).apply {
                drawColor(Color.rgb(114, 114, 114))
                drawBitmap(
                    scaledBitmap!!,
                    padX,
                    padY,
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
            }

            // 2. BACA PIXEL
            letterboxBitmap.getPixels(
                intValues,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
            )

            // 3. PREPROCESS INPUT RGB
            inputBuffer.clear()

            if (inputIsNCHW) {
                for (channel in 0..2) {
                    for (pixel in intValues) {
                        val value = when (channel) {
                            0 -> (pixel ushr 16) and 0xFF
                            1 -> (pixel ushr 8) and 0xFF
                            else -> pixel and 0xFF
                        }
                        putInputValue(value)
                    }
                }
            } else {
                for (pixel in intValues) {
                    putInputValue((pixel ushr 16) and 0xFF)
                    putInputValue((pixel ushr 8) and 0xFF)
                    putInputValue(pixel and 0xFF)
                }
            }

            inputBuffer.rewind()

            // 4. PERIKSA OUTPUT TENSOR
            val outputTensor = tflite.getOutputTensor(0)
            outputShape = outputTensor.shape()
            outputType = outputTensor.dataType()

            Log.d(TAG, "Output shape: ${outputShape.contentToString()}")
            Log.d(TAG, "Output type: $outputType")

            require(outputType == DataType.FLOAT32) {
                "Output harus FLOAT32, tetapi ditemukan $outputType."
            }

            val shape = outputShape

            require(shape.size == 3 && shape[0] == 1) {
                "Bentuk output tidak didukung: ${shape.contentToString()}"
            }

            // 5. SIAPKAN OUTPUT DAN JALANKAN INFERENCE
            val output = Array(shape[0]) {
                Array(shape[1]) {
                    FloatArray(shape[2])
                }
            }

            val outputs = mutableMapOf<Int, Any>()
            outputs[0] = output

            val startTime = System.currentTimeMillis()

            tflite.runForMultipleInputsOutputs(
                arrayOf<Any>(inputBuffer),
                outputs
            )

            Log.d(
                TAG,
                "Inference time: ${System.currentTimeMillis() - startTime} ms"
            )

            // Tensor hasil inference yang akan digunakan selanjutnya.
            val rawOutput = outputs[0] as Array<Array<FloatArray>>

            // 6. KENALI FORMAT OUTPUT
            // Raw: [1, 4 + jumlah kelas, jumlah kandidat]
            // NMS-free: format [1, jumlah kandidat, 6/7]
            val isRaw =
                shape[1] == 4 + detectionClasses.size &&
                        shape[2] > 0

            val isNmsFree =
                shape[1] == 300 && shape[2] in 6..7

            if (!isRaw && !isNmsFree) {
                Log.e(TAG, "Format output tidak dikenali: ${shape.contentToString()}")
                return emptyList()
            }

            Log.d(
                TAG,
                "Format output: ${if (isRaw) "RAW" else "NMS-FREE"}"
            )

            // 7. DIAGNOSTIK SKOR DAN KOORDINAT PER KELAS
            // Tidak mengubah hasil deteksi; hanya menulis log.
            if (isRaw) {
                val numCandidates = rawOutput[0][0].size

                Log.d(TAG, "========== DIAGNOSTIK PER KELAS ==========")

                for (classId in detectionClasses.indices) {
                    var bestScore = Float.NEGATIVE_INFINITY
                    var bestIndex = -1

                    for (i in 0 until numCandidates) {
                        val score = rawOutput[0][4 + classId][i]

                        if (score.isFinite() && score > bestScore) {
                            bestScore = score
                            bestIndex = i
                        }
                    }

                    if (bestIndex < 0) {
                        Log.d(TAG, "${detectionClasses[classId]}: tidak ada skor valid")
                        continue
                    }

                    Log.d(
                        TAG,
                        "${detectionClasses[classId]} | " +
                                "maxScore=${format(bestScore)} | " +
                                "candidate=$bestIndex | " +
                                "cx=${format(rawOutput[0][0][bestIndex])}, " +
                                "cy=${format(rawOutput[0][1][bestIndex])}, " +
                                "w=${format(rawOutput[0][2][bestIndex])}, " +
                                "h=${format(rawOutput[0][3][bestIndex])}"
                    )
                }

                Log.d(TAG, "========== AKHIR DIAGNOSTIK ==========")
            }

            val candidates = mutableListOf<Candidate>()

            // 8. PARSING KANDIDAT
            if (isNmsFree) {
                // Format ini mengasumsikan baris [x1,y1,x2,y2,score,classId].
                for (i in 0 until shape[1]) {
                    val row = rawOutput[0][i]

                    val score = row[4]
                    val classId = row[5].toInt()

                    if (!score.isFinite() || score < confidenceThreshold) continue
                    if (classId !in detectionClasses.indices) continue

                    candidates.add(
                        Candidate(
                            index = i,
                            classId = classId,
                            score = score,
                            x1 = row[0],
                            y1 = row[1],
                            x2 = row[2],
                            y2 = row[3]
                        )
                    )
                }
            } else {
                val numberOfCandidates = shape[2]
                val numberOfClasses = detectionClasses.size

                // Koordinat YOLO raw bisa berupa pixel atau normalisasi 0..1.
                var maxCoordinate = 0f

                for (i in 0 until numberOfCandidates) {
                    for (c in 0..3) {
                        val value = rawOutput[0][c][i]
                        if (value.isFinite()) {
                            maxCoordinate = max(maxCoordinate, abs(value))
                        }
                    }
                }

                val coordinatesNormalized = maxCoordinate <= 2f
                val coordinateScale = if (coordinatesNormalized) {
                    INPUT_SIZE.toFloat()
                } else {
                    1f
                }

                Log.d(
                    TAG,
                    "Coordinate mode: ${
                        if (coordinatesNormalized) "NORMALIZED" else "PIXEL"
                    }"
                )

                for (i in 0 until numberOfCandidates) {
                    val cx = rawOutput[0][0][i] * coordinateScale
                    val cy = rawOutput[0][1][i] * coordinateScale
                    val width = rawOutput[0][2][i] * coordinateScale
                    val height = rawOutput[0][3][i] * coordinateScale

                    if (!cx.isFinite() || !cy.isFinite()) continue
                    if (!width.isFinite() || !height.isFinite()) continue
                    if (width <= 0f || height <= 0f) continue

                    var bestScore = Float.NEGATIVE_INFINITY
                    var bestClass = -1

                    for (c in 0 until numberOfClasses) {
                        val score = rawOutput[0][4 + c][i]

                        if (score.isFinite() && score > bestScore) {
                            bestScore = score
                            bestClass = c
                        }
                    }

                    if (bestClass !in detectionClasses.indices) continue
                    if (!bestScore.isFinite() || bestScore < confidenceThreshold) continue

                    val x1 = cx - width / 2f
                    val y1 = cy - height / 2f
                    val x2 = cx + width / 2f
                    val y2 = cy + height / 2f

                    // Lewati kotak yang sama sekali berada di luar input.
                    if (x2 <= 0f || y2 <= 0f ||
                        x1 >= INPUT_SIZE || y1 >= INPUT_SIZE
                    ) {
                        continue
                    }

                    candidates.add(
                        Candidate(
                            index = i,
                            classId = bestClass,
                            score = bestScore,
                            x1 = x1,
                            y1 = y1,
                            x2 = x2,
                            y2 = y2
                        )
                    )
                }
            }

            Log.d(TAG, "Kandidat lolos threshold: ${candidates.size}")

            candidates
                .sortedByDescending { it.score }
                .take(DEBUG_TOP_K)
                .forEachIndexed { rank, candidate ->
                    Log.d(
                        TAG,
                        "#${rank + 1} index=${candidate.index}, " +
                                "class=${detectionClasses[candidate.classId]}, " +
                                "score=${format(candidate.score)}, " +
                                "box=[${format(candidate.x1)}, ${format(candidate.y1)}, " +
                                "${format(candidate.x2)}, ${format(candidate.y2)}]"
                    )
                }

            // 9. NMS PER KELAS UNTUK OUTPUT RAW.
            // Output NMS-free dianggap sudah melalui NMS oleh model.
            val selected = if (isNmsFree) {
                candidates.sortedByDescending { it.score }
            } else {
                classWiseNms(candidates, NMS_IOU_THRESHOLD)
            }

            // 10. UNLETTERBOX DAN NORMALISASI KE GAMBAR ASLI
            val detections = mutableListOf<DetectionCamera>()

            for (candidate in selected) {
                val leftPixels = (
                        (candidate.x1 - padX) / scale
                        ).coerceIn(0f, originalWidth.toFloat())

                val topPixels = (
                        (candidate.y1 - padY) / scale
                        ).coerceIn(0f, originalHeight.toFloat())

                val rightPixels = (
                        (candidate.x2 - padX) / scale
                        ).coerceIn(0f, originalWidth.toFloat())

                val bottomPixels = (
                        (candidate.y2 - padY) / scale
                        ).coerceIn(0f, originalHeight.toFloat())

                if (rightPixels <= leftPixels || bottomPixels <= topPixels) continue

                detections.add(
                    DetectionCamera(
                        box = RectF(
                            leftPixels / originalWidth,
                            topPixels / originalHeight,
                            rightPixels / originalWidth,
                            bottomPixels / originalHeight
                        ),
                        confidence = candidate.score,
                        classIndex = candidate.classId,
                        className = detectionClasses[candidate.classId]
                    )
                )

                if (detections.size >= MAX_DETECTIONS) break
            }

            Log.d(TAG, "========== FINAL RESULT ==========")
            Log.d(TAG, "Jumlah deteksi: ${detections.size}")

            detections.forEachIndexed { index, detection ->
                Log.d(
                    TAG,
                    "#${index + 1} ${detection.className} " +
                            "confidence=${format(detection.confidence)} " +
                            "box=${detection.box}"
                )
            }

            Log.d(TAG, "========== END DETECTION ==========")

            return detections
        } catch (e: Exception) {
            Log.e(TAG, "Gagal saat deteksi: ${e.message}", e)
            return emptyList()
        } finally {
            if (scaledBitmap != null && scaledBitmap !== bitmap) {
                scaledBitmap.recycle()
            }
            letterboxBitmap?.recycle()
        }
    }

    private fun putInputValue(pixelValue: Int) {
        val normalized = pixelValue / 255f

        when (inputType) {
            DataType.FLOAT32 -> inputBuffer.putFloat(normalized)

            DataType.UINT8 -> {
                val quantized = if (inputScale > 0f) {
                    (normalized / inputScale + inputZeroPoint)
                        .toInt()
                        .coerceIn(0, 255)
                } else {
                    pixelValue
                }
                inputBuffer.put(quantized.toByte())
            }

            DataType.INT8 -> {
                val quantized = if (inputScale > 0f) {
                    (normalized / inputScale + inputZeroPoint)
                        .toInt()
                        .coerceIn(-128, 127)
                } else {
                    pixelValue - 128
                }
                inputBuffer.put(quantized.toByte())
            }

            else -> error("Tipe input tidak didukung: $inputType")
        }
    }

    private fun classWiseNms(
        candidates: List<Candidate>,
        iouThreshold: Float
    ): List<Candidate> {
        val selected = mutableListOf<Candidate>()

        for (classId in detectionClasses.indices) {
            val remaining = candidates
                .filter { it.classId == classId }
                .sortedByDescending { it.score }
                .toMutableList()

            while (remaining.isNotEmpty()) {
                val best = remaining.removeAt(0)
                selected.add(best)

                remaining.removeAll { candidate ->
                    intersectionOverUnion(best, candidate) > iouThreshold
                }
            }
        }

        return selected.sortedByDescending { it.score }
    }

    private fun intersectionOverUnion(
        a: Candidate,
        b: Candidate
    ): Float {
        val left = max(a.x1, b.x1)
        val top = max(a.y1, b.y1)
        val right = min(a.x2, b.x2)
        val bottom = min(a.y2, b.y2)

        val intersection =
            max(0f, right - left) * max(0f, bottom - top)

        val areaA =
            max(0f, a.x2 - a.x1) * max(0f, a.y2 - a.y1)

        val areaB =
            max(0f, b.x2 - b.x1) * max(0f, b.y2 - b.y1)

        val union = areaA + areaB - intersection

        return if (union > 0f) intersection / union else 0f
    }

    private fun format(value: Float): String =
        String.format(Locale.US, "%.4f", value)

    @Synchronized
    fun close() {
        if (isClosed) return

        interpreter?.close()
        interpreter = null
        isClosed = true

        Log.d(TAG, "YOLO26 Interpreter ditutup.")
    }
}

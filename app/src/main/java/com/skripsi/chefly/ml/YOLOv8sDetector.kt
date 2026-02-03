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
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * YOLOv8s TFLite Detector with proper NMS and detection filtering
 */
class YOLOv8sDetector(
    private val context: Context,
    model: String,
    private val detectionClasses: List<String>,
    private val useNNAPI: Boolean = false
) {
    companion object {
        private const val TAG = "YOLODetector"

        // Detection thresholds - tuned for quality over quantity
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f  // Higher threshold for cleaner results
        private const val NMS_IOU_THRESHOLD = 0.4f             // Aggressive NMS to remove duplicates
        private const val MAX_DETECTIONS_PER_CLASS = 3         // Limit detections per ingredient type
        private const val MAX_TOTAL_DETECTIONS = 10            // Maximum total detections to show
        private const val MIN_BOX_SIZE = 20f                   // Minimum box size in pixels
    }

    private var interpreter: Interpreter? = null
    private var inputImageWidth = 640
    private var inputImageHeight = 640
    private var isModelQuantized = false
    private var inputIsNCHW = false
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        try {
            val modelFile = loadModelFile(model)
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(useNNAPI)
            }
            interpreter = Interpreter(modelFile, options)

            val inputShape = interpreter!!.getInputTensor(0).shape()
            parseInputShape(inputShape)

            isModelQuantized = interpreter!!.getInputTensor(0).dataType() == DataType.UINT8
            Log.d(TAG, "Model loaded: ${inputImageWidth}x${inputImageHeight}, Quantized: $isModelQuantized, NCHW=$inputIsNCHW")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}")
        }
    }

    private fun parseInputShape(inputShape: IntArray) {
        if (inputShape.size == 4) {
            if (inputShape[1] == 3) {
                inputIsNCHW = true
                inputImageHeight = inputShape[2]
                inputImageWidth = inputShape[3]
            } else {
                inputIsNCHW = false
                inputImageHeight = inputShape[1]
                inputImageWidth = inputShape[2]
            }
        }
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        return try {
            FileUtil.loadMappedFile(context, filename)
        } catch (e: Exception) {
            val assetFileDescriptor = context.assets.openFd(filename)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }

    private fun preprocessImage(bitmap: Bitmap): Pair<ByteBuffer, Triple<Float, Float, Float>> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = minOf(
            inputImageWidth.toFloat() / originalWidth,
            inputImageHeight.toFloat() / originalHeight
        )

        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        val resizedBitmap = bitmap.scale(newWidth, newHeight, false)
        val paddedBitmap = Bitmap.createBitmap(inputImageWidth, inputImageHeight, Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        canvas.drawColor(android.graphics.Color.rgb(114, 114, 114))

        val padX = (inputImageWidth - newWidth) / 2f
        val padY = (inputImageHeight - newHeight) / 2f

        canvas.drawBitmap(resizedBitmap, padX, padY, null)

        val byteBuffer = ByteBuffer.allocateDirect(
            if (isModelQuantized) inputImageWidth * inputImageHeight * 3
            else inputImageWidth * inputImageHeight * 3 * 4
        )
        byteBuffer.order(ByteOrder.nativeOrder())

        val intValues = IntArray(inputImageWidth * inputImageHeight)
        paddedBitmap.getPixels(intValues, 0, inputImageWidth, 0, 0, inputImageWidth, inputImageHeight)

        fillByteBuffer(byteBuffer, intValues)

        byteBuffer.rewind()
        return Pair(byteBuffer, Triple(scale, padX, padY))
    }

    private fun fillByteBuffer(byteBuffer: ByteBuffer, intValues: IntArray) {
        if (inputIsNCHW) {
            // Channel-first layout
            for (channel in 0..2) {
                for (pixel in intValues) {
                    val value = when (channel) {
                        0 -> (pixel shr 16) and 0xFF  // R
                        1 -> (pixel shr 8) and 0xFF   // G
                        else -> pixel and 0xFF         // B
                    }
                    if (isModelQuantized) {
                        byteBuffer.put(value.toByte())
                    } else {
                        byteBuffer.putFloat(value / 255.0f)
                    }
                }
            }
        } else {
            // NHWC layout
            for (pixel in intValues) {
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                if (isModelQuantized) {
                    byteBuffer.put(r.toByte())
                    byteBuffer.put(g.toByte())
                    byteBuffer.put(b.toByte())
                } else {
                    byteBuffer.putFloat(r / 255.0f)
                    byteBuffer.putFloat(g / 255.0f)
                    byteBuffer.putFloat(b / 255.0f)
                }
            }
        }
    }

    fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): List<DetectionCamera> {
        val interpreter = this.interpreter ?: return emptyList()

        try {
            val (inputBuffer, scaleInfo) = preprocessImage(bitmap)
            val (scale, padX, padY) = scaleInfo

            val outputTensor = interpreter.getOutputTensor(0)
            val outputShape = outputTensor.shape()
            val outputDataType = outputTensor.dataType()

            Log.d(TAG, "Output shape: ${outputShape.joinToString()}, type: $outputDataType")

            // Parse output shape
            val (numBoxes, attrCount, attrIsLast) = parseOutputShape(outputShape)
            if (numBoxes <= 0 || attrCount <= 0) {
                Log.e(TAG, "Invalid output shape")
                return emptyList()
            }

            // Run inference and get output array
            val outputArray = runInference(interpreter, inputBuffer, outputTensor, numBoxes, attrCount, attrIsLast)

            // Process detections
            val rawDetections = processDetections(
                outputArray, numBoxes, attrCount, confidenceThreshold,
                scale, padX, padY, bitmap.width, bitmap.height
            )

            Log.d(TAG, "Raw detections before NMS: ${rawDetections.size}")

            // Apply class-aware NMS
            val nmsResults = applyClassAwareNMS(rawDetections)

            // Limit total detections
            val finalResults = nmsResults.take(MAX_TOTAL_DETECTIONS)

            Log.d(TAG, "Final detections: ${finalResults.size}")
            if (finalResults.isNotEmpty()) {
                val summary = finalResults.groupingBy { it.className }.eachCount()
                Log.d(TAG, "Detected: ${summary.entries.joinToString { "${it.key}(${it.value})" }}")
            }

            return finalResults

        } catch (e: Exception) {
            Log.e(TAG, "Detection error: ${e.message}", e)
            return emptyList()
        }
    }

    private fun parseOutputShape(outputShape: IntArray): Triple<Int, Int, Boolean> {
        val expectedAttr = 4 + detectionClasses.size  // 4 bbox + num classes = 25 for your 21 classes

        Log.d(TAG, "Expected attributes: $expectedAttr (4 bbox + ${detectionClasses.size} classes)")
        Log.d(TAG, "Output shape from model: ${outputShape.joinToString()}")

        return when (outputShape.size) {
            3 -> {
                val diffLast = kotlin.math.abs(outputShape[2] - expectedAttr)
                val diffFirst = kotlin.math.abs(outputShape[1] - expectedAttr)

                val result = if (diffLast <= diffFirst) {
                    Triple(outputShape[1], outputShape[2], true)  // [1, numBoxes, attrCount]
                } else {
                    Triple(outputShape[2], outputShape[1], false) // [1, attrCount, numBoxes]
                }

                Log.d(TAG, "Parsed: numBoxes=${result.first}, attrCount=${result.second}, attrIsLast=${result.third}")

                // IMPORTANT: Check for model mismatch
                val actualClasses = result.second - 4
                if (actualClasses != detectionClasses.size) {
                    Log.w(TAG, "⚠️ MODEL MISMATCH! Model has $actualClasses classes, but labels.txt has ${detectionClasses.size} classes!")
                    Log.w(TAG, "⚠️ This will cause incorrect detections. Please use a model trained on your ${detectionClasses.size} ingredients.")
                }

                result
            }
            2 -> Triple(outputShape[0], outputShape[1], true)
            else -> Triple(0, 0, false)
        }
    }

    private fun runInference(
        interpreter: Interpreter,
        inputBuffer: ByteBuffer,
        outputTensor: org.tensorflow.lite.Tensor,
        numBoxes: Int,
        attrCount: Int,
        attrIsLast: Boolean
    ): Array<FloatArray> {
        val outputArray = Array(attrCount) { FloatArray(numBoxes) }
        val outputDataType = outputTensor.dataType()

        inputBuffer.rewind()

        if (outputDataType == DataType.FLOAT32) {
            if (attrIsLast) {
                val flatOut = Array(1) { Array(numBoxes) { FloatArray(attrCount) } }
                val outputs: MutableMap<Int, Any> = mutableMapOf(0 to flatOut)
                interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
                for (b in 0 until numBoxes) {
                    for (a in 0 until attrCount) {
                        outputArray[a][b] = flatOut[0][b][a]
                    }
                }
            } else {
                val flatOut = Array(1) { Array(attrCount) { FloatArray(numBoxes) } }
                val outputs: MutableMap<Int, Any> = mutableMapOf(0 to flatOut)
                interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
                for (a in 0 until attrCount) {
                    for (b in 0 until numBoxes) {
                        outputArray[a][b] = flatOut[0][a][b]
                    }
                }
            }
        } else {
            // Quantized model
            val quantParams = outputTensor.quantizationParams()
            val scaleQ = quantParams.scale
            val zeroPointQ = quantParams.zeroPoint

            if (attrIsLast) {
                val flatOut = Array(1) { Array(numBoxes) { ByteArray(attrCount) } }
                val outputs: MutableMap<Int, Any> = mutableMapOf(0 to flatOut)
                interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
                for (b in 0 until numBoxes) {
                    for (a in 0 until attrCount) {
                        val quantized = (flatOut[0][b][a].toInt() and 0xFF)
                        outputArray[a][b] = (quantized - zeroPointQ) * scaleQ
                    }
                }
            } else {
                val flatOut = Array(1) { Array(attrCount) { ByteArray(numBoxes) } }
                val outputs: MutableMap<Int, Any> = mutableMapOf(0 to flatOut)
                interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
                for (a in 0 until attrCount) {
                    for (b in 0 until numBoxes) {
                        val quantized = (flatOut[0][a][b].toInt() and 0xFF)
                        outputArray[a][b] = (quantized - zeroPointQ) * scaleQ
                    }
                }
            }
        }

        return outputArray
    }

    private fun processDetections(
        outputArray: Array<FloatArray>,
        numBoxes: Int,
        attrCount: Int,
        confidenceThreshold: Float,
        scale: Float,
        padX: Float,
        padY: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): List<DetectionCamera> {
        val detections = mutableListOf<DetectionCamera>()
        val numClasses = detectionClasses.size

        // Determine if model has objectness score
        val hasObjectness = (attrCount == 5 + numClasses)
        val classStartIndex = if (hasObjectness) 5 else 4

        // Check if coordinates are absolute (>2 means pixels, not normalized)
        val coordsAreAbsolute = (0 until minOf(4, attrCount)).any { a ->
            (0 until minOf(100, numBoxes)).any { b ->
                kotlin.math.abs(outputArray[a][b]) > 2.0f
            }
        }

        for (b in 0 until numBoxes) {
            // Get raw coordinates
            var xCenter = outputArray.getOrNull(0)?.getOrNull(b) ?: continue
            var yCenter = outputArray.getOrNull(1)?.getOrNull(b) ?: continue
            var width = outputArray.getOrNull(2)?.getOrNull(b) ?: continue
            var height = outputArray.getOrNull(3)?.getOrNull(b) ?: continue

            // Skip invalid boxes
            if (width <= 0 || height <= 0) continue

            // Convert to pixels if normalized
            if (!coordsAreAbsolute) {
                xCenter *= inputImageWidth
                yCenter *= inputImageHeight
                width *= inputImageWidth
                height *= inputImageHeight
            }

            // Get objectness score if present
            val objScore = if (hasObjectness) {
                outputArray.getOrNull(4)?.getOrNull(b) ?: 1f
            } else 1f

            // Find best class - ONLY consider classes we have labels for
            var maxScore = 0f
            var maxClassIdx = -1

            // Calculate how many classes are in the model output
            val modelNumClasses = attrCount - classStartIndex

            // Only iterate through classes we have labels for
            val classesToCheck = minOf(modelNumClasses, numClasses)

            for (c in 0 until classesToCheck) {
                val classScore = (outputArray.getOrNull(classStartIndex + c)?.getOrNull(b) ?: 0f) * objScore
                if (classScore > maxScore) {
                    maxScore = classScore
                    maxClassIdx = c
                }
            }

            // Skip if no valid class found or confidence too low
            if (maxClassIdx < 0 || maxClassIdx >= numClasses) continue
            if (maxScore < confidenceThreshold) continue

            // Convert from center format to corner format
            val x1 = xCenter - width / 2f
            val y1 = yCenter - height / 2f

            // Map to original image coordinates
            val origX = (x1 - padX) / scale
            val origY = (y1 - padY) / scale
            val origW = width / scale
            val origH = height / scale

            // Clamp to image bounds
            val left = origX.coerceIn(0f, bitmapWidth.toFloat())
            val top = origY.coerceIn(0f, bitmapHeight.toFloat())
            val right = (origX + origW).coerceIn(0f, bitmapWidth.toFloat())
            val bottom = (origY + origH).coerceIn(0f, bitmapHeight.toFloat())

            val finalW = right - left
            val finalH = bottom - top

            // Skip tiny boxes
            if (finalW < MIN_BOX_SIZE || finalH < MIN_BOX_SIZE) continue

            // Only add detection if class index is valid
            if (maxClassIdx < 0 || maxClassIdx >= detectionClasses.size) continue

            val className = detectionClasses[maxClassIdx]

            detections.add(
                DetectionCamera(
                    box = RectF(left, top, right, bottom),
                    confidence = maxScore,
                    classIndex = maxClassIdx,
                    className = className
                )
            )
        }

        return detections
    }

    /**
     * Apply class-aware Non-Maximum Suppression
     * This ensures we don't have multiple boxes for the same object
     * and limits detections per class
     */
    private fun applyClassAwareNMS(detections: List<DetectionCamera>): List<DetectionCamera> {
        if (detections.isEmpty()) return emptyList()

        // First pass: Class-aware NMS (suppress within same class)
        val byClass = detections.groupBy { it.classIndex }
        val classNmsResults = mutableListOf<DetectionCamera>()

        for ((_, classDetections) in byClass) {
            // Sort by confidence (highest first)
            val sorted = classDetections.sortedByDescending { it.confidence }
            val kept = mutableListOf<DetectionCamera>()
            val suppressed = BooleanArray(sorted.size)

            for (i in sorted.indices) {
                if (suppressed[i]) continue
                if (kept.size >= MAX_DETECTIONS_PER_CLASS) break

                kept.add(sorted[i])

                // Suppress overlapping boxes within same class
                for (j in i + 1 until sorted.size) {
                    if (suppressed[j]) continue

                    val iou = calculateIoU(sorted[i].box, sorted[j].box)
                    if (iou > NMS_IOU_THRESHOLD) {
                        suppressed[j] = true
                    }
                }
            }

            classNmsResults.addAll(kept)
        }

        // Second pass: Global NMS (suppress across all classes for highly overlapping boxes)
        val globalSorted = classNmsResults.sortedByDescending { it.confidence }
        val globalKept = mutableListOf<DetectionCamera>()
        val globalSuppressed = BooleanArray(globalSorted.size)

        for (i in globalSorted.indices) {
            if (globalSuppressed[i]) continue

            globalKept.add(globalSorted[i])

            // Suppress highly overlapping boxes (even from different classes)
            for (j in i + 1 until globalSorted.size) {
                if (globalSuppressed[j]) continue

                val iou = calculateIoU(globalSorted[i].box, globalSorted[j].box)
                // Use higher threshold for global NMS (0.7) - only suppress if almost identical
                if (iou > 0.7f) {
                    globalSuppressed[j] = true
                }
            }
        }

        Log.d(TAG, "NMS: ${detections.size} -> class NMS: ${classNmsResults.size} -> global NMS: ${globalKept.size}")

        // Sort final results by confidence
        return globalKept.sortedByDescending { it.confidence }
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = maxOf(box1.left, box2.left)
        val intersectTop = maxOf(box1.top, box2.top)
        val intersectRight = minOf(box1.right, box2.right)
        val intersectBottom = minOf(box1.bottom, box2.bottom)

        if (intersectRight <= intersectLeft || intersectBottom <= intersectTop) {
            return 0f
        }

        val intersectArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()
        val unionArea = box1Area + box2Area - intersectArea

        return if (unionArea > 0) intersectArea / unionArea else 0f
    }

    fun close() {
        interpreter?.close()
        cameraExecutor.shutdown()
    }
}


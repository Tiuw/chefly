package com.skripsi.chefly.ml

import android.content.Context
import android.graphics.Bitmap
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

        private const val INPUT_SIZE = 640

        private const val CONFIDENCE_THRESHOLD = 0.50f

        private const val MAX_TOTAL_DETECTIONS = 10

        /**
         * Padding mengikuti preprocessing YOLO:
         * RGB(114,114,114)
         */
        private const val LETTERBOX_COLOR = 114

        /**
         * Hanya untuk debug.
         */
        private const val DEBUG_OUTPUT_COUNT = 10
    }

    private var interpreter: Interpreter? = null

    private var isModelQuantized = false

    private var inputShape: IntArray? = null

    private lateinit var imgData: ByteBuffer

    private lateinit var intValues: IntArray

    private lateinit var paddedBitmap: Bitmap

    private lateinit var canvas: Canvas

    @Volatile
    private var isProcessing = false

    private var frameCount = 0


    // =========================================================
    // INITIALIZATION
    // =========================================================

    init {

        try {

            Log.d(
                TAG,
                "=================================================="
            )

            Log.d(
                TAG,
                "INITIALIZING YOLO26 DETECTOR"
            )

            Log.d(
                TAG,
                "MODEL FILE = $modelFilename"
            )

            val modelFile = FileUtil.loadMappedFile(
                context,
                modelFilename
            )

            val options = Interpreter.Options().apply {

                setNumThreads(4)

                setUseNNAPI(useNNAPI)

                setUseXNNPACK(true)
            }

            interpreter = Interpreter(
                modelFile,
                options
            )


            // =================================================
            // INPUT TENSOR
            // =================================================

            val inputTensor =
                interpreter!!.getInputTensor(0)

            inputShape =
                inputTensor.shape()

            isModelQuantized =
                inputTensor.dataType() == DataType.UINT8


            // =================================================
            // OUTPUT TENSOR
            // =================================================

            val outputTensor =
                interpreter!!.getOutputTensor(0)


            // =================================================
            // BYTES PER CHANNEL
            // =================================================

            val bytesPerChannel =
                if (isModelQuantized) {
                    1
                } else {
                    4
                }


            // =================================================
            // INPUT BUFFER
            // =================================================

            imgData = ByteBuffer.allocateDirect(
                INPUT_SIZE *
                        INPUT_SIZE *
                        3 *
                        bytesPerChannel
            ).apply {

                order(
                    ByteOrder.nativeOrder()
                )
            }


            // =================================================
            // PIXEL BUFFER
            // =================================================

            intValues = IntArray(
                INPUT_SIZE *
                        INPUT_SIZE
            )


            // =================================================
            // 640 x 640 BITMAP
            // =================================================

            paddedBitmap = Bitmap.createBitmap(
                INPUT_SIZE,
                INPUT_SIZE,
                Bitmap.Config.ARGB_8888
            )

            canvas = Canvas(
                paddedBitmap
            )


            // =================================================
            // MODEL INFORMATION
            // =================================================

            Log.d(
                TAG,
                "INPUT SHAPE = ${
                    inputTensor
                        .shape()
                        .contentToString()
                }"
            )

            Log.d(
                TAG,
                "INPUT TYPE = ${
                    inputTensor.dataType()
                }"
            )

            Log.d(
                TAG,
                "OUTPUT SHAPE = ${
                    outputTensor
                        .shape()
                        .contentToString()
                }"
            )

            Log.d(
                TAG,
                "OUTPUT TYPE = ${
                    outputTensor.dataType()
                }"
            )

            Log.d(
                TAG,
                "INPUT QUANTIZED = $isModelQuantized"
            )

            Log.d(
                TAG,
                "LETTERBOX COLOR = $LETTERBOX_COLOR"
            )

            Log.d(
                TAG,
                "XNNPACK = true"
            )

            Log.d(
                TAG,
                "NNAPI = $useNNAPI"
            )

            Log.d(
                TAG,
                "=================================================="
            )


        } catch (e: Exception) {

            Log.e(
                TAG,
                "FATAL: Gagal memuat model YOLO26",
                e
            )
        }
    }


    // =========================================================
    // DETECT OBJECTS
    // =========================================================

    fun detectObjects(
        bitmap: Bitmap,
        confidenceThreshold: Float =
            CONFIDENCE_THRESHOLD
    ): List<DetectionCamera> {

        val tfliteInterpreter =
            interpreter ?: return emptyList()


        if (isProcessing) {
            return emptyList()
        }


        frameCount++

        try {

            isProcessing = true


            // =================================================
            // 1. ORIGINAL IMAGE INFORMATION
            // =================================================

            val originalWidth =
                bitmap.width

            val originalHeight =
                bitmap.height


            Log.d(
                TAG,
                "=================================================="
            )

            Log.d(
                TAG,
                "FRAME #$frameCount"
            )

            Log.d(
                TAG,
                "ORIGINAL IMAGE = " +
                        "${originalWidth}x${originalHeight}"
            )


            // =================================================
            // 2. CALCULATE LETTERBOX SCALE
            // =================================================

            val scale = minOf(
                INPUT_SIZE.toFloat() /
                        originalWidth.toFloat(),

                INPUT_SIZE.toFloat() /
                        originalHeight.toFloat()
            )


            val newWidth =
                (originalWidth * scale)
                    .toInt()

            val newHeight =
                (originalHeight * scale)
                    .toInt()


            val padX =
                (INPUT_SIZE - newWidth) / 2f

            val padY =
                (INPUT_SIZE - newHeight) / 2f


            Log.d(
                TAG,
                "LETTERBOX"
            )

            Log.d(
                TAG,
                "scale   = $scale"
            )

            Log.d(
                TAG,
                "newSize = ${newWidth}x${newHeight}"
            )

            Log.d(
                TAG,
                "padX    = $padX"
            )

            Log.d(
                TAG,
                "padY    = $padY"
            )

            Log.d(
                TAG,
                "padding = $LETTERBOX_COLOR"
            )


            // =================================================
            // 3. RESIZE
            // =================================================

            val resizedBitmap =
                bitmap.scale(
                    newWidth,
                    newHeight,
                    true
                )


            // =================================================
            // 4. LETTERBOX
            // =================================================

            canvas.drawColor(
                Color.rgb(
                    LETTERBOX_COLOR,
                    LETTERBOX_COLOR,
                    LETTERBOX_COLOR
                )
            )


            canvas.drawBitmap(
                resizedBitmap,
                padX,
                padY,
                null
            )


            // =================================================
            // 5. GET PIXELS
            // =================================================

            paddedBitmap.getPixels(
                intValues,
                0,
                INPUT_SIZE,
                0,
                0,
                INPUT_SIZE,
                INPUT_SIZE
            )


            // =================================================
            // 6. PREPARE INPUT BUFFER
            // =================================================

            imgData.rewind()


            // =================================================
            // DEBUG VARIABLES
            // =================================================

            var minInput =
                Float.MAX_VALUE

            var maxInput =
                -Float.MAX_VALUE

            var sumInput =
                0.0

            var sumSquaredInput =
                0.0


            // =================================================
            // 7. BITMAP -> FLOAT32
            // =================================================

            for (pixel in intValues) {

                val r =
                    (pixel shr 16) and 0xFF

                val g =
                    (pixel shr 8) and 0xFF

                val b =
                    pixel and 0xFF


                if (isModelQuantized) {

                    // =========================================
                    // UINT8
                    // =========================================

                    imgData.put(
                        r.toByte()
                    )

                    imgData.put(
                        g.toByte()
                    )

                    imgData.put(
                        b.toByte()
                    )


                    // Debug statistik untuk uint8
                    minInput =
                        minOf(
                            minInput,
                            r.toFloat(),
                            g.toFloat(),
                            b.toFloat()
                        )

                    maxInput =
                        maxOf(
                            maxInput,
                            r.toFloat(),
                            g.toFloat(),
                            b.toFloat()
                        )

                    sumInput += r
                    sumInput += g
                    sumInput += b

                    sumSquaredInput +=
                        r.toDouble() * r

                    sumSquaredInput +=
                        g.toDouble() * g

                    sumSquaredInput +=
                        b.toDouble() * b

                } else {

                    // =========================================
                    // FLOAT32
                    // =========================================

                    val rf =
                        r / 255.0f

                    val gf =
                        g / 255.0f

                    val bf =
                        b / 255.0f


                    imgData.putFloat(rf)
                    imgData.putFloat(gf)
                    imgData.putFloat(bf)


                    // =========================================
                    // STATISTICS
                    // =========================================

                    minInput =
                        minOf(
                            minInput,
                            rf,
                            gf,
                            bf
                        )

                    maxInput =
                        maxOf(
                            maxInput,
                            rf,
                            gf,
                            bf
                        )

                    sumInput += rf
                    sumInput += gf
                    sumInput += bf

                    sumSquaredInput +=
                        rf.toDouble() * rf

                    sumSquaredInput +=
                        gf.toDouble() * gf

                    sumSquaredInput +=
                        bf.toDouble() * bf
                }
            }


            imgData.rewind()


            // =================================================
            // 8. INPUT STATISTICS
            // =================================================

            val totalInputValues =
                INPUT_SIZE *
                        INPUT_SIZE *
                        3


            val meanInput =
                sumInput /
                        totalInputValues


            val variance =
                (
                        sumSquaredInput /
                                totalInputValues
                        ) -
                        (
                                meanInput *
                                        meanInput
                                )


            val stdInput =
                kotlin.math.sqrt(
                    maxOf(
                        0.0,
                        variance
                    )
                )


            Log.d(
                TAG,
                "=================================================="
            )

            Log.d(
                TAG,
                "INPUT STATISTICS"
            )

            Log.d(
                TAG,
                "shape = [1,640,640,3]"
            )

            Log.d(
                TAG,
                "dtype = ${
                    if (isModelQuantized)
                        "UINT8"
                    else
                        "FLOAT32"
                }"
            )

            Log.d(
                TAG,
                "min   = $minInput"
            )

            Log.d(
                TAG,
                "max   = $maxInput"
            )

            Log.d(
                TAG,
                "mean  = $meanInput"
            )

            Log.d(
                TAG,
                "std   = $stdInput"
            )

            Log.d(
                TAG,
                "sum   = $sumInput"
            )


            // =================================================
            // 9. DEBUG PIXEL VALUES
            // =================================================
            //
            // Ini untuk dibandingkan dengan Python.
            //
            // Python:
            //
            // print(input_data[0,0,0])
            // print(input_data[0,100,100])
            // print(input_data[0,320,320])
            // print(input_data[0,639,639])
            //
            // =================================================

            fun debugPixel(
                y: Int,
                x: Int
            ) {

                val index =
                    (y * INPUT_SIZE + x) * 3


                val r =
                    intValues[
                        y * INPUT_SIZE + x
                    ]

                val red =
                    (r shr 16) and 0xFF

                val green =
                    (r shr 8) and 0xFF

                val blue =
                    r and 0xFF


                if (isModelQuantized) {

                    Log.d(
                        TAG,
                        "PIXEL [$y,$x] " +
                                "RGB=($red,$green,$blue) " +
                                "INPUT=($red,$green,$blue)"
                    )

                } else {

                    val rf =
                        red / 255.0f

                    val gf =
                        green / 255.0f

                    val bf =
                        blue / 255.0f


                    Log.d(
                        TAG,
                        "PIXEL [$y,$x] " +
                                "RGB=($red,$green,$blue) " +
                                "INPUT=($rf,$gf,$bf) " +
                                "INDEX=$index"
                    )
                }
            }


            debugPixel(
                0,
                0
            )

            debugPixel(
                100,
                100
            )

            debugPixel(
                320,
                320
            )

            debugPixel(
                639,
                639
            )


            // =================================================
            // 10. INPUT BUFFER SIZE
            // =================================================

            Log.d(
                TAG,
                "INPUT BUFFER"
            )

            Log.d(
                TAG,
                "capacity = ${imgData.capacity()}"
            )

            Log.d(
                TAG,
                "position  = ${imgData.position()}"
            )

            Log.d(
                TAG,
                "remaining = ${imgData.remaining()}"
            )


            // =================================================
            // 11. INFERENCE
            // =================================================

            val startTime =
                System.currentTimeMillis()


            val outputTensor =
                tfliteInterpreter
                    .getOutputTensor(0)


            val shape =
                outputTensor.shape()


            Log.d(
                TAG,
                "OUTPUT TENSOR"
            )

            Log.d(
                TAG,
                "shape = ${
                    shape.contentToString()
                }"
            )

            Log.d(
                TAG,
                "dtype = ${
                    outputTensor.dataType()
                }"
            )


            /*
             * YOLO26:
             *
             * [1,300,6]
             *
             * x1
             * y1
             * x2
             * y2
             * confidence
             * class
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


            tfliteInterpreter
                .runForMultipleInputsOutputs(
                    arrayOf(imgData),
                    outputs
                )


            val inferenceTime =
                System.currentTimeMillis() -
                        startTime


            Log.d(
                TAG,
                "=================================================="
            )

            Log.d(
                TAG,
                "INFERENCE"
            )

            Log.d(
                TAG,
                "time = ${inferenceTime}ms"
            )


            // =================================================
            // 12. RAW OUTPUT DEBUG
            // =================================================

            Log.d(
                TAG,
                "RAW OUTPUT FIRST $DEBUG_OUTPUT_COUNT"
            )


            val debugCount =
                minOf(
                    DEBUG_OUTPUT_COUNT,
                    shape[1]
                )


            for (i in 0 until debugCount) {

                Log.d(
                    TAG,
                    "OUTPUT[$i] = ${
                        flatOut[0][i]
                            .contentToString()
                    }"
                )
            }


            // =================================================
            // 13. OUTPUT RANGE
            // =================================================

            var minOutput =
                Float.MAX_VALUE

            var maxOutput =
                -Float.MAX_VALUE


            for (i in 0 until shape[1]) {

                for (j in 0 until shape[2]) {

                    val value =
                        flatOut[0][i][j]

                    minOutput =
                        minOf(
                            minOutput,
                            value
                        )

                    maxOutput =
                        maxOf(
                            maxOutput,
                            value
                        )
                }
            }


            Log.d(
                TAG,
                "OUTPUT RANGE"
            )

            Log.d(
                TAG,
                "min = $minOutput"
            )

            Log.d(
                TAG,
                "max = $maxOutput"
            )


            // =================================================
            // 14. POST PROCESSING
            // =================================================

            val detections =
                mutableListOf<DetectionCamera>()


            val maxClasses =
                detectionClasses.size


            val isNmsFreeEndToEnd =
                shape.size >= 3 &&
                        (
                                shape[2] == 6 ||
                                        shape[2] == 7
                                )


            Log.d(
                TAG,
                "OUTPUT FORMAT = ${
                    if (isNmsFreeEndToEnd)
                        "YOLO26 NMS-FREE"
                    else
                        "CONVENTIONAL YOLO"
                }"
            )


            var globalMaxScore =
                -1f

            var globalMaxClsId =
                -1


            // =================================================
            // YOLO26 NMS-FREE
            // =================================================

            if (isNmsFreeEndToEnd) {

                val totalBoxes =
                    shape[1]


                for (b in 0 until totalBoxes) {

                    val boxArray =
                        flatOut[0][b]


                    val x1 =
                        boxArray[0]

                    val y1 =
                        boxArray[1]

                    val x2 =
                        boxArray[2]

                    val y2 =
                        boxArray[3]

                    val score =
                        boxArray[4]

                    val clsId =
                        boxArray[5].toInt()


                    // =========================================
                    // GLOBAL MAX
                    // =========================================

                    if (
                        score >
                        globalMaxScore
                    ) {

                        globalMaxScore =
                            score

                        globalMaxClsId =
                            clsId
                    }


                    // =========================================
                    // CONFIDENCE FILTER
                    // =========================================

                    if (
                        score <
                        confidenceThreshold
                    ) {
                        continue
                    }


                    // =========================================
                    // CLASS VALIDATION
                    // =========================================

                    if (
                        clsId < 0 ||
                        clsId >= maxClasses
                    ) {
                        Log.w(
                            TAG,
                            "INVALID CLASS ID = $clsId"
                        )

                        continue
                    }


                    // =========================================
                    // 0..1 -> 0..640
                    // =========================================

                    val leftBox =
                        x1 *
                                INPUT_SIZE

                    val topBox =
                        y1 *
                                INPUT_SIZE

                    val rightBox =
                        x2 *
                                INPUT_SIZE

                    val bottomBox =
                        y2 *
                                INPUT_SIZE


                    // =========================================
                    // REMOVE LETTERBOX
                    // =========================================

                    val unpadLeft =
                        (
                                (leftBox - padX) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalWidth.toFloat()
                            )


                    val unpadTop =
                        (
                                (topBox - padY) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalHeight.toFloat()
                            )


                    val unpadRight =
                        (
                                (rightBox - padX) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalWidth.toFloat()
                            )


                    val unpadBottom =
                        (
                                (bottomBox - padY) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalHeight.toFloat()
                            )


                    // =========================================
                    // NORMALIZE TO ORIGINAL IMAGE
                    // =========================================

                    val normLeft =
                        unpadLeft /
                                originalWidth.toFloat()


                    val normTop =
                        unpadTop /
                                originalHeight.toFloat()


                    val normRight =
                        unpadRight /
                                originalWidth.toFloat()


                    val normBottom =
                        unpadBottom /
                                originalHeight.toFloat()


                    // =========================================
                    // VALID BOX
                    // =========================================

                    if (
                        normRight <= normLeft ||
                        normBottom <= normTop
                    ) {
                        continue
                    }


                    // =========================================
                    // ADD DETECTION
                    // =========================================

                    detections.add(

                        DetectionCamera(

                            box = RectF(
                                normLeft,
                                normTop,
                                normRight,
                                normBottom
                            ),

                            confidence =
                                score,

                            classIndex =
                                clsId,

                            className =
                                detectionClasses[
                                    clsId
                                ]
                        )
                    )
                }


            } else {

                // =================================================
                // FALLBACK CONVENTIONAL YOLO
                // =================================================

                val numAnchors =
                    shape[2]


                for (i in 0 until numAnchors) {

                    val cx =
                        flatOut[0][0][i]

                    val cy =
                        flatOut[0][1][i]

                    val w =
                        flatOut[0][2][i]

                    val h =
                        flatOut[0][3][i]


                    var maxScore =
                        0f

                    var maxClass =
                        -1


                    for (
                    c in 0 until maxClasses
                    ) {

                        val score =
                            flatOut[0][4 + c][i]


                        if (
                            score >
                            maxScore
                        ) {

                            maxScore =
                                score

                            maxClass =
                                c
                        }
                    }


                    if (
                        maxScore >
                        globalMaxScore
                    ) {

                        globalMaxScore =
                            maxScore

                        globalMaxClsId =
                            maxClass
                    }


                    if (
                        maxScore <
                        confidenceThreshold ||
                        maxClass == -1
                    ) {
                        continue
                    }


                    val leftBox =
                        cx - w / 2f

                    val topBox =
                        cy - h / 2f

                    val rightBox =
                        cx + w / 2f

                    val bottomBox =
                        cy + h / 2f


                    val unpadLeft =
                        (
                                (leftBox - padX) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalWidth.toFloat()
                            )


                    val unpadTop =
                        (
                                (topBox - padY) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalHeight.toFloat()
                            )


                    val unpadRight =
                        (
                                (rightBox - padX) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalWidth.toFloat()
                            )


                    val unpadBottom =
                        (
                                (bottomBox - padY) /
                                        scale
                                )
                            .coerceIn(
                                0f,
                                originalHeight.toFloat()
                            )


                    detections.add(

                        DetectionCamera(

                            box = RectF(

                                unpadLeft /
                                        originalWidth.toFloat(),

                                unpadTop /
                                        originalHeight.toFloat(),

                                unpadRight /
                                        originalWidth.toFloat(),

                                unpadBottom /
                                        originalHeight.toFloat()
                            ),

                            confidence =
                                maxScore,

                            classIndex =
                                maxClass,

                            className =
                                detectionClasses[
                                    maxClass
                                ]
                        )
                    )
                }
            }


            // =================================================
            // 15. GLOBAL MAX CLASS
            // =================================================

            val maxClassName =
                if (
                    globalMaxClsId in
                    detectionClasses.indices
                ) {

                    detectionClasses[
                        globalMaxClsId
                    ]

                } else {

                    "N/A"
                }


            Log.d(
                TAG,
                "=================================================="
            )

            Log.d(
                TAG,
                "FINAL RESULT"
            )

            Log.d(
                TAG,
                "detections = ${
                    detections.size
                }"
            )

            Log.d(
                TAG,
                "threshold = $confidenceThreshold"
            )

            Log.d(
                TAG,
                "global max score = $globalMaxScore"
            )

            Log.d(
                TAG,
                "global max class = " +
                        "$globalMaxClsId ($maxClassName)"
            )


            // =================================================
            // 16. PRINT DETECTIONS
            // =================================================

            for (
            (index, detection)
            in detections.withIndex()
            ) {

                Log.d(
                    TAG,
                    "DETECTION[$index] " +
                            "class=${detection.className} " +
                            "id=${detection.classIndex} " +
                            "confidence=${detection.confidence} " +
                            "box=${detection.box}"
                )
            }


            // =================================================
            // RETURN
            // =================================================

            return detections
                .sortedByDescending {
                    it.confidence
                }
                .take(
                    MAX_TOTAL_DETECTIONS
                )


        } catch (e: Exception) {

            Log.e(
                TAG,
                "EXCEPTION during YOLO26 detection",
                e
            )

            return emptyList()

        } finally {

            isProcessing = false
        }
    }


    // =========================================================
    // CLOSE
    // =========================================================

    fun close() {

        try {

            interpreter?.close()

            interpreter = null


            if (
                ::paddedBitmap.isInitialized &&
                !paddedBitmap.isRecycled
            ) {

                paddedBitmap.recycle()
            }


            Log.d(
                TAG,
                "YOLO26 Detector closed safely."
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error closing YOLO26 Detector",
                e
            )
        }
    }
}
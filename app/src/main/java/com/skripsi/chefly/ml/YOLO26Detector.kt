package com.skripsi.chefly.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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

        private const val LETTERBOX_COLOR = 114

        private const val DEBUG_OUTPUT_COUNT = 10

        private const val PYTHON_INPUT_ASSET = "python_input.bin"

        private const val INPUT_COMPARE_TOLERANCE = 0.000001f
    }

    // =========================================================
    // CONTEXT
    // =========================================================

    private val detectorContext: Context =
        context.applicationContext


    // =========================================================
    // TFLITE
    // =========================================================

    private var interpreter: Interpreter? = null

    private var isModelQuantized = false

    private var inputShape: IntArray? = null

    private lateinit var imgData: ByteBuffer


    // =========================================================
    // IMAGE
    // =========================================================

    private lateinit var intValues: IntArray


    // =========================================================
    // DEBUG INPUT
    // =========================================================

    private lateinit var androidInputValues: FloatArray


    // =========================================================
    // PROCESSING
    // =========================================================

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

            val modelFile =
                FileUtil.loadMappedFile(
                    detectorContext,
                    modelFilename
                )


            // =================================================
            // TFLITE OPTIONS
            // =================================================

            val options =
                Interpreter.Options().apply {

                    setNumThreads(4)

                    setUseNNAPI(
                        useNNAPI
                    )

                    setUseXNNPACK(
                        true
                    )
                }


            interpreter =
                Interpreter(
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
                inputTensor.dataType() ==
                        DataType.UINT8


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

            imgData =
                ByteBuffer.allocateDirect(
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
            // ANDROID INPUT ARRAY
            // =================================================

            androidInputValues =
                FloatArray(
                    INPUT_SIZE *
                            INPUT_SIZE *
                            3
                )


            // =================================================
            // PIXEL BUFFER
            // =================================================

            intValues =
                IntArray(
                    INPUT_SIZE *
                            INPUT_SIZE
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
                "PYTHON INPUT ASSET = $PYTHON_INPUT_ASSET"
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
                "PREPROCESSING = OpenCV INTER_LINEAR equivalent"
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
    // OPENCV INTER_LINEAR RESIZE
    // =========================================================
    //
    // Tujuan fungsi ini adalah menggantikan:
    //
    // cv2.resize(
    //     img,
    //     (new_w, new_h),
    //     interpolation=cv2.INTER_LINEAR
    // )
    //
    // Kita tidak menggunakan Bitmap.scale() karena implementasi
    // filtering Android tidak harus menghasilkan piksel yang sama
    // dengan OpenCV.
    //
    // =========================================================

    private fun resizeBilinearOpenCvEquivalent(
        source: Bitmap,
        destinationWidth: Int,
        destinationHeight: Int
    ): IntArray {

        val sourceWidth =
            source.width

        val sourceHeight =
            source.height


        val sourcePixels =
            IntArray(
                sourceWidth *
                        sourceHeight
            )


        source.getPixels(
            sourcePixels,
            0,
            sourceWidth,
            0,
            0,
            sourceWidth,
            sourceHeight
        )


        val destinationPixels =
            IntArray(
                destinationWidth *
                        destinationHeight
            )


        // =====================================================
        // SCALE SEPERTI OpenCV
        // =====================================================

        val scaleX =
            sourceWidth.toDouble() /
                    destinationWidth.toDouble()

        val scaleY =
            sourceHeight.toDouble() /
                    destinationHeight.toDouble()


        // =====================================================
        // RESIZE
        // =====================================================

        for (dy in 0 until destinationHeight) {

            /*
             * OpenCV INTER_LINEAR menggunakan pemetaan
             * koordinat berdasarkan rasio source/destination.
             *
             * Secara konsep:
             *
             * fx = (dx + 0.5) * scaleX - 0.5
             * fy = (dy + 0.5) * scaleY - 0.5
             */

            val fy =
                (
                        dy + 0.5
                        ) *
                        scaleY -
                        0.5


            var y0 =
                floor(fy).toInt()

            var yAlpha =
                fy - y0.toDouble()


            // =================================================
            // BORDER REPLICATION
            // =================================================

            if (y0 < 0) {

                y0 = 0
                yAlpha = 0.0
            }


            val y1 =
                min(
                    y0 + 1,
                    sourceHeight - 1
                )


            if (y0 >= sourceHeight - 1) {

                y0 =
                    sourceHeight - 1

                yAlpha = 0.0
            }


            val row0 =
                y0 *
                        sourceWidth

            val row1 =
                y1 *
                        sourceWidth


            for (dx in 0 until destinationWidth) {

                val fx =
                    (
                            dx + 0.5
                            ) *
                            scaleX -
                            0.5


                var x0 =
                    floor(fx).toInt()

                var xAlpha =
                    fx - x0.toDouble()


                // =============================================
                // BORDER REPLICATION
                // =============================================

                if (x0 < 0) {

                    x0 = 0
                    xAlpha = 0.0
                }


                val x1 =
                    min(
                        x0 + 1,
                        sourceWidth - 1
                    )


                if (x0 >= sourceWidth - 1) {

                    x0 =
                        sourceWidth - 1

                    xAlpha = 0.0
                }


                // =============================================
                // SOURCE PIXELS
                // =============================================

                val p00 =
                    sourcePixels[
                        row0 + x0
                    ]

                val p01 =
                    sourcePixels[
                        row0 + x1
                    ]

                val p10 =
                    sourcePixels[
                        row1 + x0
                    ]

                val p11 =
                    sourcePixels[
                        row1 + x1
                    ]


                // =============================================
                // CHANNELS
                // =============================================

                val a00 =
                    (p00 ushr 24) and 0xFF

                val r00 =
                    (p00 ushr 16) and 0xFF

                val g00 =
                    (p00 ushr 8) and 0xFF

                val b00 =
                    p00 and 0xFF


                val a01 =
                    (p01 ushr 24) and 0xFF

                val r01 =
                    (p01 ushr 16) and 0xFF

                val g01 =
                    (p01 ushr 8) and 0xFF

                val b01 =
                    p01 and 0xFF


                val a10 =
                    (p10 ushr 24) and 0xFF

                val r10 =
                    (p10 ushr 16) and 0xFF

                val g10 =
                    (p10 ushr 8) and 0xFF

                val b10 =
                    p10 and 0xFF


                val a11 =
                    (p11 ushr 24) and 0xFF

                val r11 =
                    (p11 ushr 16) and 0xFF

                val g11 =
                    (p11 ushr 8) and 0xFF

                val b11 =
                    p11 and 0xFF


                // =============================================
                // BILINEAR INTERPOLATION
                // =============================================

                val topR =
                    r00 * (1.0 - xAlpha) +
                            r01 * xAlpha

                val bottomR =
                    r10 * (1.0 - xAlpha) +
                            r11 * xAlpha

                val finalR =
                    topR * (1.0 - yAlpha) +
                            bottomR * yAlpha


                val topG =
                    g00 * (1.0 - xAlpha) +
                            g01 * xAlpha

                val bottomG =
                    g10 * (1.0 - xAlpha) +
                            g11 * xAlpha

                val finalG =
                    topG * (1.0 - yAlpha) +
                            bottomG * yAlpha


                val topB =
                    b00 * (1.0 - xAlpha) +
                            b01 * xAlpha

                val bottomB =
                    b10 * (1.0 - xAlpha) +
                            b11 * xAlpha

                val finalB =
                    topB * (1.0 - yAlpha) +
                            bottomB * yAlpha


                val topA =
                    a00 * (1.0 - xAlpha) +
                            a01 * xAlpha

                val bottomA =
                    a10 * (1.0 - xAlpha) +
                            a11 * xAlpha

                val finalA =
                    topA * (1.0 - yAlpha) +
                            bottomA * yAlpha


                // =============================================
                // ROUND TO UINT8
                // =============================================

                val red =
                    finalR
                        .coerceIn(0.0, 255.0)
                        .toInt()

                val green =
                    finalG
                        .coerceIn(0.0, 255.0)
                        .toInt()

                val blue =
                    finalB
                        .coerceIn(0.0, 255.0)
                        .toInt()

                val alpha =
                    finalA
                        .coerceIn(0.0, 255.0)
                        .toInt()


                destinationPixels[
                    dy *
                            destinationWidth +
                            dx
                ] =
                    (
                            (alpha shl 24) or
                                    (red shl 16) or
                                    (green shl 8) or
                                    blue
                            )
            }
        }


        return destinationPixels
    }


    // =========================================================
    // LETTERBOX
    // =========================================================
    //
    // Python:
    //
    // canvas = np.full(
    //     (640,640,3),
    //     114,
    //     dtype=np.uint8
    // )
    //
    // canvas[top:top+new_h,
    //        left:left+new_w] = resized
    //
    // =========================================================

    private fun createPythonEquivalentLetterbox(
        bitmap: Bitmap,
        newWidth: Int,
        newHeight: Int,
        padX: Float,
        padY: Float
    ): IntArray {

        val outputPixels =
            IntArray(
                INPUT_SIZE *
                        INPUT_SIZE
            )


        // =====================================================
        // FILL SELURUH CANVAS DENGAN 114
        // =====================================================

        val grayColor =
            (
                    0xFF000000.toInt() or
                            (LETTERBOX_COLOR shl 16) or
                            (LETTERBOX_COLOR shl 8) or
                            LETTERBOX_COLOR
                    )


        java.util.Arrays.fill(
            outputPixels,
            grayColor
        )


        // =====================================================
        // RESIZE
        // =====================================================

        val resizedPixels =
            resizeBilinearOpenCvEquivalent(
                bitmap,
                newWidth,
                newHeight
            )


        // =====================================================
        // PYTHON:
        //
        // left = int(pad_x)
        // top  = int(pad_y)
        // =====================================================

        val left =
            padX.toInt()

        val top =
            padY.toInt()


        Log.d(
            TAG,
            "INTEGER PADDING"
        )

        Log.d(
            TAG,
            "left = $left"
        )

        Log.d(
            TAG,
            "top  = $top"
        )


        // =====================================================
        // COPY RESIZED IMAGE INTO CANVAS
        // =====================================================

        for (y in 0 until newHeight) {

            val destinationY =
                top + y


            if (
                destinationY < 0 ||
                destinationY >= INPUT_SIZE
            ) {
                continue
            }


            val sourceRow =
                y * newWidth

            val destinationRow =
                destinationY *
                        INPUT_SIZE


            for (x in 0 until newWidth) {

                val destinationX =
                    left + x


                if (
                    destinationX < 0 ||
                    destinationX >= INPUT_SIZE
                ) {
                    continue
                }


                outputPixels[
                    destinationRow +
                            destinationX
                ] =
                    resizedPixels[
                        sourceRow + x
                    ]
            }
        }


        return outputPixels
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
            // 1. ORIGINAL IMAGE
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
            // 2. LETTERBOX SCALE
            // =================================================

            val scale =
                minOf(
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
            // 3. PYTHON-EQUIVALENT RESIZE + LETTERBOX
            // =================================================
            //
            // Python:
            //
            // resized = cv2.resize(
            //     img,
            //     (new_w, new_h),
            //     interpolation=cv2.INTER_LINEAR
            // )
            //
            // canvas = np.full(
            //     (640,640,3),
            //     114,
            //     dtype=np.uint8
            // )
            //
            // left = int(pad_x)
            // top = int(pad_y)
            //
            // canvas[
            //     top:top+new_h,
            //     left:left+new_w
            // ] = resized
            //
            // =================================================

            val letterboxedPixels =
                createPythonEquivalentLetterbox(
                    bitmap = bitmap,
                    newWidth = newWidth,
                    newHeight = newHeight,
                    padX = padX,
                    padY = padY
                )


            // =================================================
            // 4. SAVE PIXELS
            // =================================================

            System.arraycopy(
                letterboxedPixels,
                0,
                intValues,
                0,
                letterboxedPixels.size
            )


            // =================================================
            // 5. PREPARE INPUT
            // =================================================

            imgData.rewind()


            var minInput =
                Float.MAX_VALUE

            var maxInput =
                -Float.MAX_VALUE

            var sumInput =
                0.0

            var sumSquaredInput =
                0.0


            // =================================================
            // 6. PIXEL -> FLOAT32
            // =================================================

            var inputIndex =
                0


            for (pixel in intValues) {

                val r =
                    (pixel ushr 16) and 0xFF

                val g =
                    (pixel ushr 8) and 0xFF

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


                    androidInputValues[
                        inputIndex++
                    ] =
                        r.toFloat()

                    androidInputValues[
                        inputIndex++
                    ] =
                        g.toFloat()

                    androidInputValues[
                        inputIndex++
                    ] =
                        b.toFloat()


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

                    sumInput +=
                        r

                    sumInput +=
                        g

                    sumInput +=
                        b

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

                    /*
                     * Python:
                     *
                     * rgb.astype(np.float32) / 255.0
                     */

                    val rf =
                        r.toFloat() /
                                255.0f

                    val gf =
                        g.toFloat() /
                                255.0f

                    val bf =
                        b.toFloat() /
                                255.0f


                    // =========================================
                    // TFLITE BUFFER
                    // =========================================

                    imgData.putFloat(
                        rf
                    )

                    imgData.putFloat(
                        gf
                    )

                    imgData.putFloat(
                        bf
                    )


                    // =========================================
                    // DEBUG ARRAY
                    // =========================================

                    androidInputValues[
                        inputIndex++
                    ] =
                        rf

                    androidInputValues[
                        inputIndex++
                    ] =
                        gf

                    androidInputValues[
                        inputIndex++
                    ] =
                        bf


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

                    sumInput +=
                        rf

                    sumInput +=
                        gf

                    sumInput +=
                        bf

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
            // 7. INPUT STATISTICS
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
                sqrt(
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
                "ANDROID INPUT STATISTICS"
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
            // 8. PIXEL DEBUG
            // =================================================

            fun debugPixel(
                y: Int,
                x: Int
            ) {

                val pixelIndex =
                    y * INPUT_SIZE + x

                val pixel =
                    intValues[pixelIndex]

                val red =
                    (pixel ushr 16) and 0xFF

                val green =
                    (pixel ushr 8) and 0xFF

                val blue =
                    pixel and 0xFF


                val index =
                    pixelIndex * 3


                if (isModelQuantized) {

                    Log.d(
                        TAG,
                        "ANDROID PIXEL [$y,$x] " +
                                "RGB=($red,$green,$blue) " +
                                "INPUT=($red,$green,$blue) " +
                                "INDEX=$index"
                    )

                } else {

                    val rf =
                        red.toFloat() /
                                255.0f

                    val gf =
                        green.toFloat() /
                                255.0f

                    val bf =
                        blue.toFloat() /
                                255.0f


                    Log.d(
                        TAG,
                        "ANDROID PIXEL [$y,$x] " +
                                "RGB=($red,$green,$blue) " +
                                "INPUT=($rf,$gf,$bf) " +
                                "INDEX=$index"
                    )
                }
            }


            debugPixel(0, 0)

            debugPixel(100, 100)

            debugPixel(320, 320)

            debugPixel(639, 639)


            // =================================================
            // 9. COMPARE PYTHON INPUT
            // =================================================

            comparePythonInput()


            // =================================================
            // 10. INPUT BUFFER
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
            // 11. OUTPUT TENSOR
            // =================================================

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


            // =================================================
            // 12. OUTPUT ARRAY
            // =================================================

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


            // =================================================
            // 13. INFERENCE
            // =================================================

            val startTime =
                System.currentTimeMillis()


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
            // 14. RAW OUTPUT
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
            // 15. OUTPUT RANGE
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
            // 16. POST PROCESSING
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
            // 17. YOLO26 NMS-FREE
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


                    if (
                        score >
                        globalMaxScore
                    ) {

                        globalMaxScore =
                            score

                        globalMaxClsId =
                            clsId
                    }


                    if (
                        score <
                        confidenceThreshold
                    ) {
                        continue
                    }


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


                    if (
                        normRight <= normLeft ||
                        normBottom <= normTop
                    ) {
                        continue
                    }


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
                // 18. FALLBACK CONVENTIONAL YOLO
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
            // 19. GLOBAL MAX
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
            // 20. DETECTIONS
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
            // 21. RETURN
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
    // OPEN PYTHON INPUT
    // =========================================================

    private fun contextAssetsOpenPythonInput():
            ByteArray {

        return try {

            detectorContext
                .assets
                .open(
                    PYTHON_INPUT_ASSET
                )
                .use { inputStream ->

                    inputStream.readBytes()
                }

        } catch (e: Exception) {

            Log.w(
                TAG,
                "PYTHON INPUT TIDAK TERSEDIA: " +
                        PYTHON_INPUT_ASSET,
                e
            )

            ByteArray(0)
        }
    }


    // =========================================================
    // COMPARE PYTHON INPUT
    // =========================================================

    private fun comparePythonInput() {

        try {

            val pythonInputBytes =
                contextAssetsOpenPythonInput()


            val expectedBytes =
                INPUT_SIZE *
                        INPUT_SIZE *
                        3 *
                        4


            Log.d(
                TAG,
                "=================================================="
            )

            Log.d(
                TAG,
                "PYTHON INPUT VS ANDROID INPUT"
            )

            Log.d(
                TAG,
                "python bytes = ${pythonInputBytes.size}"
            )

            Log.d(
                TAG,
                "expected bytes = $expectedBytes"
            )


            if (
                pythonInputBytes.isEmpty()
            ) {

                Log.w(
                    TAG,
                    "PYTHON INPUT TIDAK TERSEDIA"
                )

                return
            }


            if (
                pythonInputBytes.size !=
                expectedBytes
            ) {

                Log.w(
                    TAG,
                    "python_input.bin SIZE TIDAK SESUAI"
                )

                Log.w(
                    TAG,
                    "actual = ${pythonInputBytes.size}"
                )

                Log.w(
                    TAG,
                    "expected = $expectedBytes"
                )

                return
            }


            if (isModelQuantized) {

                Log.w(
                    TAG,
                    "PYTHON INPUT COMPARISON " +
                            "DILEWATI: MODEL UINT8"
                )

                return
            }


            val pythonBuffer =
                ByteBuffer
                    .wrap(
                        pythonInputBytes
                    )
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )


            var pythonMin =
                Float.MAX_VALUE

            var pythonMax =
                -Float.MAX_VALUE

            var pythonSum =
                0.0

            var sumDifference =
                0.0

            var maxDifference =
                0.0

            var differentCount =
                0


            val totalValues =
                androidInputValues.size


            // =================================================
            // COMPARE EVERY FLOAT
            // =================================================

            for (
            i in 0 until totalValues
            ) {

                val pythonValue =
                    pythonBuffer.float

                val androidValue =
                    androidInputValues[i]


                pythonMin =
                    minOf(
                        pythonMin,
                        pythonValue
                    )

                pythonMax =
                    maxOf(
                        pythonMax,
                        pythonValue
                    )

                pythonSum +=
                    pythonValue.toDouble()


                val difference =
                    abs(
                        pythonValue -
                                androidValue
                    )


                sumDifference +=
                    difference.toDouble()


                maxDifference =
                    maxOf(
                        maxDifference,
                        difference.toDouble()
                    )


                if (
                    difference >
                    INPUT_COMPARE_TOLERANCE
                ) {

                    differentCount++
                }
            }


            // =================================================
            // PYTHON STATISTICS
            // =================================================

            val pythonMean =
                pythonSum /
                        totalValues


            val meanDifference =
                sumDifference /
                        totalValues


            // =================================================
            // RESULT
            // =================================================

            Log.d(
                TAG,
                "PYTHON MIN  = $pythonMin"
            )

            Log.d(
                TAG,
                "PYTHON MAX  = $pythonMax"
            )

            Log.d(
                TAG,
                "PYTHON MEAN = $pythonMean"
            )

            Log.d(
                TAG,
                "ANDROID MIN = ${
                    androidInputValues.minOrNull()
                }"
            )

            Log.d(
                TAG,
                "ANDROID MAX = ${
                    androidInputValues.maxOrNull()
                }"
            )

            Log.d(
                TAG,
                "ANDROID MEAN = ${
                    androidInputValues.average()
                }"
            )

            Log.d(
                TAG,
                "MEAN ABS DIFF = $meanDifference"
            )

            Log.d(
                TAG,
                "MAX ABS DIFF = $maxDifference"
            )

            Log.d(
                TAG,
                "DIFFERENT VALUES = " +
                        "$differentCount / $totalValues"
            )


            // =================================================
            // PERCENTAGE DIFFERENT
            // =================================================

            val differentPercentage =
                (
                        differentCount.toDouble() /
                                totalValues.toDouble()
                        ) *
                        100.0


            Log.d(
                TAG,
                "DIFFERENT PERCENTAGE = " +
                        "$differentPercentage%"
            )


            // =================================================
            // OVERALL RESULT
            // =================================================

            if (
                maxDifference <=
                INPUT_COMPARE_TOLERANCE
            ) {

                Log.d(
                    TAG,
                    "INPUT COMPARISON = IDENTICAL"
                )

            } else {

                Log.w(
                    TAG,
                    "INPUT COMPARISON = DIFFERENT"
                )
            }


            // =================================================
            // SAMPLE COMPARISON
            // =================================================

            comparePythonSample(
                pythonInputBytes,
                0
            )

            comparePythonSample(
                pythonInputBytes,
                192300
            )

            comparePythonSample(
                pythonInputBytes,
                615360
            )

            comparePythonSample(
                pythonInputBytes,
                1228797
            )


            Log.d(
                TAG,
                "=================================================="
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "GAGAL MEMBANDINGKAN python_input.bin",
                e
            )
        }
    }


    // =========================================================
    // COMPARE PYTHON SAMPLE
    // =========================================================

    private fun comparePythonSample(
        pythonInputBytes: ByteArray,
        floatIndex: Int
    ) {

        try {

            val totalValues =
                INPUT_SIZE *
                        INPUT_SIZE *
                        3


            if (
                floatIndex < 0 ||
                floatIndex >= totalValues
            ) {

                Log.w(
                    TAG,
                    "SAMPLE INDEX INVALID = $floatIndex"
                )

                return
            }


            val byteOffset =
                floatIndex * 4


            if (
                byteOffset + 4 >
                pythonInputBytes.size
            ) {

                Log.w(
                    TAG,
                    "SAMPLE BYTE OFFSET INVALID = " +
                            "$byteOffset"
                )

                return
            }


            val pythonBuffer =
                ByteBuffer
                    .wrap(
                        pythonInputBytes
                    )
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )


            pythonBuffer.position(
                byteOffset
            )


            val pythonValue =
                pythonBuffer.float


            val androidValue =
                androidInputValues[
                    floatIndex
                ]


            val difference =
                abs(
                    pythonValue -
                            androidValue
                )


            Log.d(
                TAG,
                "SAMPLE INDEX = $floatIndex " +
                        "PYTHON=$pythonValue " +
                        "ANDROID=$androidValue " +
                        "DIFF=$difference"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "GAGAL MEMBACA PYTHON SAMPLE " +
                        "INDEX=$floatIndex",
                e
            )
        }
    }


    // =========================================================
    // CLOSE
    // =========================================================

    fun close() {

        try {

            interpreter?.close()

            interpreter = null


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
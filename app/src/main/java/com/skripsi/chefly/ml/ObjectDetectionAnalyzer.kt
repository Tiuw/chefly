package com.skripsi.chefly.ml

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.YuvImage
import android.graphics.Rect
import android.graphics.Matrix
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import kotlin.collections.map

class ObjectDetectionAnalyzer(
    private val detector: YOLOv8sDetector,
    private val tracker: LiveObjectTracker,
    private val onResults: (List<TrackedObjectCamera>, Long, Size) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        /***
         * Throttle analysis to avoid overloading (analyze every 100ms)
         */
        if (currentTimestamp - lastAnalyzedTimestamp >= 100) {
            lastAnalyzedTimestamp = currentTimestamp

            val bitmap = imageProxyToBitmap(imageProxy)
            bitmap?.let { bmp ->
                val detections = detector.detectObjects(bmp, 0.3f)
                val trackedObjects = tracker.updateTracking(detections)

                // No OCR/ML Kit here to keep analyzer lightweight and dependency-free.
                val updatedObjects = trackedObjects.map { obj -> obj }

                val imageSize = Size(bmp.width.toFloat(), bmp.height.toFloat())
                onResults(updatedObjects, currentTimestamp, imageSize)
            }
        }

        imageProxy.close()
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer // Y
            val uBuffer = imageProxy.planes[1].buffer // U
            val vBuffer = imageProxy.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage =
                YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)
            val imageBytes = out.toByteArray()

            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            // Rotate according to camera orientation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            bitmap
        } catch (e: Exception) {
            Log.e("ObjectDetectionAnalyzer", "Error converting ImageProxy to Bitmap", e)
            null
        }
    }
}

package com.skripsi.chefly.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.skripsi.chefly.data.DetectedIngredient
import com.skripsi.chefly.ml.YOLOv8sDetector
import com.skripsi.chefly.ui.RecipeViewModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.collections.map
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

// Fun color palette for ingredients
private val ingredientColors = listOf(
    Color(0xFFFF6B6B), // Red
    Color(0xFF4ECDC4), // Teal
    Color(0xFFFFE66D), // Yellow
    Color(0xFF95E1D3), // Mint
    Color(0xFFF38181), // Coral
    Color(0xFFAA96DA), // Purple
    Color(0xFFFCBF49), // Orange
    Color(0xFF2EC4B6), // Cyan
    Color(0xFFE71D36), // Bright Red
    Color(0xFF7209B7), // Deep Purple
)

@Composable
fun CameraScreen(
    viewModel: RecipeViewModel,
    onSearchRecipes: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        CameraPreviewScreen(viewModel = viewModel, onSearchRecipes = onSearchRecipes)
    } else {
        PermissionDeniedScreen(onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) })
    }
}

@Composable
fun CameraPreviewScreen(
    viewModel: RecipeViewModel,
    onSearchRecipes: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = (context as? LifecycleOwner)
        ?: throw IllegalStateException("Context is not a LifecycleOwner. Make sure CameraPreviewScreen is called from an Activity/ComponentActivity.")
    var detections by remember { mutableStateOf<List<DetectedIngredient>>(emptyList()) }

    // Try to load labels from assets/labels.txt; fall back to built-in list
    val labels = remember {
        val defaultLabels = listOf(
            "Ayam","Bawang Merah","Bawang Putih","Bayam","Cabai Hijau","Cabai Merah",
            "Daging Kambing","Daging Sapi","Daun Bawang","Ikan","Kacang Panjang","Kangkung",
            "Kol","Nasi","Tahu","Telur","Tempe","Terong","Tomat","Udang","Wortel"
        )

        try {
            val stream = context.assets.open("labels.txt")
            val text = stream.bufferedReader().use { it.readText() }
            val parsed = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (parsed.isEmpty()) defaultLabels else parsed
        } catch (_: Exception) {
            defaultLabels
        }
    }

    // Instantiate detector with model filename placed under app/src/main/assets (packaged into assets automatically)
    val detector = remember { YOLOv8sDetector(context, "yolov8s.tflite", labels, useNNAPI = false) }

    // States for upload-image feature
    val coroutineScope = rememberCoroutineScope()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageDetections by remember { mutableStateOf<List<DetectedIngredient>>(emptyList()) }
    var debugMessage by remember { mutableStateOf<String?>(null) }

    // Flag to pause camera analysis when processing uploaded image
    var isProcessingUploadedImage by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Set flag immediately to stop camera detection from interfering
            isProcessingUploadedImage = true
            try {
                // Read EXIF orientation first (open a separate stream)
                val exifStream = context.contentResolver.openInputStream(it)
                var rotationNeeded = 0
                exifStream?.use { stream ->
                    try {
                        val exif = ExifInterface(stream)
                        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        rotationNeeded = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270
                            else -> 0
                        }
                    } catch (e: Exception) {
                        Log.w("CameraScreen", "Failed to read EXIF: ${e.message}")
                    }
                }

                val stream = context.contentResolver.openInputStream(it)
                var bmpOriginal = BitmapFactory.decodeStream(stream)
                stream?.close()

                if (bmpOriginal != null) {
                    if (rotationNeeded != 0) {
                        val matrix = android.graphics.Matrix().apply { postRotate(rotationNeeded.toFloat()) }
                        bmpOriginal = Bitmap.createBitmap(bmpOriginal, 0, 0, bmpOriginal.width, bmpOriginal.height, matrix, true)
                    }

                    // downscale large images to avoid OOM and speed up detection
                    val maxDim = 1024
                    val scaledBmp = if (kotlin.math.max(bmpOriginal.width, bmpOriginal.height) > maxDim) {
                        val ratio = maxDim.toFloat() / kotlin.math.max(bmpOriginal.width, bmpOriginal.height)
                        val newW = (bmpOriginal.width * ratio).toInt().coerceAtLeast(1)
                        val newH = (bmpOriginal.height * ratio).toInt().coerceAtLeast(1)
                        Bitmap.createScaledBitmap(bmpOriginal, newW, newH, true)
                    } else bmpOriginal

                    selectedBitmap = scaledBmp

                    // run detection off main thread and log inference time
                    coroutineScope.launch(Dispatchers.Default) {
                        // Small delay to ensure camera analysis has stopped (prevents race condition with detector)
                        delay(100)

                        val t0 = System.currentTimeMillis()
                        // Use lower threshold (0.35f) for uploaded images to match manual detect behavior
                        var dets = detector.detectObjects(scaledBmp, 0.35f)
                        val t1 = System.currentTimeMillis()
                        Log.d("CameraScreen", "Detection on scaled image took ${t1 - t0} ms; found ${dets.size} detections")

                        // If we got no detections on scaled image, try original image as a fallback with even lower threshold
                        if (dets.isEmpty() && scaledBmp != bmpOriginal) {
                            val t2 = System.currentTimeMillis()
                            try {
                                dets = detector.detectObjects(bmpOriginal, 0.3f)
                                val t3 = System.currentTimeMillis()
                                Log.d("CameraScreen", "Fallback detection on original image took ${t3 - t2} ms; found ${dets.size} detections")
                            } catch (e: Exception) {
                                Log.e("CameraScreen", "Fallback detection error: ${e.message}", e)
                            }
                        }

                        val mapped = dets.map { d ->
                            DetectedIngredient(
                                label = d.className,
                                confidence = d.confidence,
                                boundingBox = android.graphics.RectF(d.box.left, d.box.top, d.box.right, d.box.bottom)
                            )
                        }

                        withContext(Dispatchers.Main) {
                            imageDetections = mapped
                            viewModel.updateDetectedIngredients(mapped.map { it.label })
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Error loading picked image: ${e.message}", e)
            }
        }
    }

    // Helper: manual detect button (lower threshold) for debugging uploaded images
    fun triggerManualDetectOnSelected() {
        val bmp = selectedBitmap ?: return
        coroutineScope.launch(Dispatchers.Default) {
            val t0 = System.currentTimeMillis()
            val dets = try {
                detector.detectObjects(bmp, 0.35f) // Lower threshold for manual re-detect
            } catch (e: Exception) {
                Log.e("CameraScreen", "Manual detection error: ${e.message}", e)
                emptyList()
            }
            val t1 = System.currentTimeMillis()
            val mapped = dets.map { d ->
                DetectedIngredient(
                    label = d.className,
                    confidence = d.confidence,
                    boundingBox = android.graphics.RectF(d.box.left, d.box.top, d.box.right, d.box.bottom)
                )
            }
            withContext(Dispatchers.Main) {
                imageDetections = mapped
                viewModel.updateDetectedIngredients(mapped.map { it.label })
                debugMessage = "Manual detect: found ${mapped.size} (in ${t1 - t0} ms): ${mapped.joinToString(",") { it.label }}"
            }
            // clear message after 3 seconds
            coroutineScope.launch {
                delay(3000)
                withContext(Dispatchers.Main) { debugMessage = null }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // transient debug snackbar-like overlay
        debugMessage?.let { msg ->
            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .align(Alignment.TopCenter)
                .zIndex(10f)) {
                Surface(
                    color = Color(0xFF4ECDC4).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (selectedBitmap != null) {
            // Show picked image and overlay detections
            val bmp = selectedBitmap!!
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    // Skip camera detection if we're processing an uploaded image
                                    if (isProcessingUploadedImage || selectedBitmap != null) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    val bmp = imageProxyToBitmap(imageProxy)
                                    if (bmp != null) {
                                        // imageProxyToBitmap already rotates the bitmap according to rotationDegrees
                                        val dets = detector.detectObjects(bmp) // Uses default 0.5 threshold

                                        // Map DetectionCamera -> DetectedIngredient (simple mapping)
                                        val mapped = dets.map { d ->
                                            DetectedIngredient(
                                                label = d.className,
                                                confidence = d.confidence,
                                                boundingBox = android.graphics.RectF(
                                                    d.box.left, d.box.top, d.box.right, d.box.bottom
                                                )
                                            )
                                        }

                                        detections = mapped
                                        val ingredients = mapped.map { it.label }
                                        viewModel.updateDetectedIngredients(ingredients)
                                    }

                                    imageProxy.close()
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) {
                            // ignore
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay for detection boxes (use imageDetections if selected, else camera detections)
        val currentDetections = if (selectedBitmap != null) imageDetections else detections

        // Animated scanning line effect
        val infiniteTransition = rememberInfiniteTransition(label = "scan")
        val scanLineY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scanLine"
        )

        // Pulsing animation for detection boxes
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Draw scanning line when no detections
            if (currentDetections.isEmpty() && selectedBitmap == null) {
                val lineY = scanLineY * canvasH
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF4ECDC4).copy(alpha = 0.8f),
                            Color(0xFF44CF6C).copy(alpha = 0.9f),
                            Color(0xFF4ECDC4).copy(alpha = 0.8f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, lineY),
                    end = Offset(canvasW, lineY),
                    strokeWidth = 4f
                )
                // Glow effect
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF4ECDC4).copy(alpha = 0.3f),
                            Color(0xFF44CF6C).copy(alpha = 0.4f),
                            Color(0xFF4ECDC4).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, lineY),
                    end = Offset(canvasW, lineY),
                    strokeWidth = 20f
                )
            }

            if (selectedBitmap != null) {
                val bmp = selectedBitmap!!
                val bmpW = bmp.width.toFloat()
                val bmpH = bmp.height.toFloat()
                val scale = kotlin.math.min(canvasW / bmpW, canvasH / bmpH)
                val offsetX = (canvasW - bmpW * scale) / 2f
                val offsetY = (canvasH - bmpH * scale) / 2f

                currentDetections.forEachIndexed { index, detection ->
                    val box = detection.boundingBox
                    val boxColor = ingredientColors[index % ingredientColors.size]

                    val left = offsetX + box.left * scale
                    val top = offsetY + box.top * scale
                    val right = offsetX + box.right * scale
                    val bottom = offsetY + box.bottom * scale

                    val rectLeft = left.coerceIn(0f, canvasW)
                    val rectTop = top.coerceIn(0f, canvasH)
                    val rectRight = right.coerceIn(0f, canvasW)
                    val rectBottom = bottom.coerceIn(0f, canvasH)

                    val width = rectRight - rectLeft
                    val height = rectBottom - rectTop
                    val cornerLength = minOf(width, height) * 0.2f

                    // Draw glowing background
                    drawRoundRect(
                        color = boxColor.copy(alpha = 0.15f),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Draw animated corner brackets
                    val strokeWidth = 4f * pulseScale
                    // Top-left corner
                    drawLine(boxColor, Offset(rectLeft, rectTop + cornerLength), Offset(rectLeft, rectTop), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectLeft, rectTop), Offset(rectLeft + cornerLength, rectTop), strokeWidth, StrokeCap.Round)
                    // Top-right corner
                    drawLine(boxColor, Offset(rectRight - cornerLength, rectTop), Offset(rectRight, rectTop), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectRight, rectTop), Offset(rectRight, rectTop + cornerLength), strokeWidth, StrokeCap.Round)
                    // Bottom-left corner
                    drawLine(boxColor, Offset(rectLeft, rectBottom - cornerLength), Offset(rectLeft, rectBottom), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectLeft, rectBottom), Offset(rectLeft + cornerLength, rectBottom), strokeWidth, StrokeCap.Round)
                    // Bottom-right corner
                    drawLine(boxColor, Offset(rectRight - cornerLength, rectBottom), Offset(rectRight, rectBottom), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectRight, rectBottom), Offset(rectRight, rectBottom - cornerLength), strokeWidth, StrokeCap.Round)
                }
            } else {
                currentDetections.forEachIndexed { index, detection ->
                    val box = detection.boundingBox
                    val boxColor = ingredientColors[index % ingredientColors.size]

                    val rectLeft = box.left
                    val rectTop = box.top
                    val rectRight = box.right
                    val rectBottom = box.bottom

                    val width = rectRight - rectLeft
                    val height = rectBottom - rectTop
                    val cornerLength = minOf(width, height) * 0.2f

                    // Draw glowing background
                    drawRoundRect(
                        color = boxColor.copy(alpha = 0.15f),
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Draw animated corner brackets
                    val strokeWidth = 4f * pulseScale
                    // Top-left corner
                    drawLine(boxColor, Offset(rectLeft, rectTop + cornerLength), Offset(rectLeft, rectTop), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectLeft, rectTop), Offset(rectLeft + cornerLength, rectTop), strokeWidth, StrokeCap.Round)
                    // Top-right corner
                    drawLine(boxColor, Offset(rectRight - cornerLength, rectTop), Offset(rectRight, rectTop), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectRight, rectTop), Offset(rectRight, rectTop + cornerLength), strokeWidth, StrokeCap.Round)
                    // Bottom-left corner
                    drawLine(boxColor, Offset(rectLeft, rectBottom - cornerLength), Offset(rectLeft, rectBottom), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectLeft, rectBottom), Offset(rectLeft + cornerLength, rectBottom), strokeWidth, StrokeCap.Round)
                    // Bottom-right corner
                    drawLine(boxColor, Offset(rectRight - cornerLength, rectBottom), Offset(rectRight, rectBottom), strokeWidth, StrokeCap.Round)
                    drawLine(boxColor, Offset(rectRight, rectBottom), Offset(rectRight, rectBottom - cornerLength), strokeWidth, StrokeCap.Round)
                }
            }
        }

        // Detection labels overlay (rendered as Compose elements for better text)
        if (currentDetections.isNotEmpty()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val canvasW = constraints.maxWidth.toFloat()
                val canvasH = constraints.maxHeight.toFloat()

                currentDetections.forEachIndexed { index, detection ->
                    val boxColor = ingredientColors[index % ingredientColors.size]
                    val box = detection.boundingBox

                    // Calculate label position
                    val labelX: Float
                    val labelY: Float

                    if (selectedBitmap != null) {
                        val bmp = selectedBitmap!!
                        val bmpW = bmp.width.toFloat()
                        val bmpH = bmp.height.toFloat()
                        val scale = kotlin.math.min(canvasW / bmpW, canvasH / bmpH)
                        val offsetX = (canvasW - bmpW * scale) / 2f
                        val offsetY = (canvasH - bmpH * scale) / 2f

                        labelX = offsetX + box.left * scale
                        labelY = offsetY + box.top * scale
                    } else {
                        labelX = box.left
                        labelY = box.top
                    }

                    // Label badge
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (labelX / LocalDensity.current.density).dp,
                                y = ((labelY - 28f) / LocalDensity.current.density).dp
                            )
                    ) {
                        Surface(
                            color = boxColor,
                            shape = RoundedCornerShape(4.dp),
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = detection.label.replaceFirstChar { it.uppercase() },
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${(detection.confidence * 100).toInt()}%",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fun floating action buttons with icons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 200.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upload button with animation
            val uploadScale by animateFloatAsState(
                targetValue = if (selectedBitmap != null) 0.9f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "uploadScale"
            )

            FloatingActionButton(
                onClick = { pickImageLauncher.launch("image/*") },
                modifier = Modifier
                    .scale(uploadScale)
                    .shadow(8.dp, CircleShape),
                containerColor = Color(0xFF4ECDC4),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Outlined.PhotoLibrary,
                    contentDescription = "Upload Image",
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(
                visible = selectedBitmap != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = {
                            selectedBitmap = null
                            imageDetections = emptyList()
                            isProcessingUploadedImage = false
                            viewModel.updateDetectedIngredients(emptyList())
                        },
                        modifier = Modifier.size(48.dp),
                        containerColor = Color(0xFFFF6B6B),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FloatingActionButton(
                        onClick = { triggerManualDetectOnSelected() },
                        modifier = Modifier.size(48.dp),
                        containerColor = Color(0xFFFFE66D),
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-detect", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Animated detection info panel
        AnimatedVisibility(
            visible = true,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f),
                                Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Header with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    // Animated ingredient icon
                    val iconRotation by rememberInfiniteTransition(label = "iconSpin").animateFloat(
                        initialValue = 0f,
                        targetValue = if (currentDetections.isNotEmpty()) 0f else 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "iconRotate"
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentDetections.isNotEmpty())
                                    Brush.linearGradient(listOf(Color(0xFF4ECDC4), Color(0xFF44CF6C)))
                                else
                                    Brush.linearGradient(listOf(Color(0xFF667eea), Color(0xFF764ba2)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (currentDetections.isNotEmpty()) Icons.Default.Restaurant else Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(if (currentDetections.isEmpty()) iconRotation else 0f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (currentDetections.isNotEmpty()) "Found ${currentDetections.size} Ingredient${if (currentDetections.size > 1) "s" else ""}!" else "Scanning...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        AnimatedVisibility(visible = currentDetections.isEmpty()) {
                            Text(
                                text = "Point camera at ingredients to detect them",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Animated ingredient chips
                AnimatedVisibility(
                    visible = currentDetections.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(currentDetections.size) { index ->
                                val detection = currentDetections[index]
                                val chipColor = ingredientColors[index % ingredientColors.size]

                                // Animate each chip entrance
                                val chipScale by animateFloatAsState(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "chipScale$index"
                                )

                                Surface(
                                    modifier = Modifier
                                        .scale(chipScale)
                                        .animateItem(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = chipColor.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        chipColor.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Colored dot indicator
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(chipColor)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = detection.label.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Confidence badge
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(chipColor.copy(alpha = 0.3f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${(detection.confidence * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = chipColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fun search button with gradient
                        Button(
                            onClick = onSearchRecipes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF667eea),
                                                Color(0xFF764ba2),
                                                Color(0xFFf093fb)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Find Delicious Recipes",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
            android.graphics.YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
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
    } catch (_: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDeniedScreen(onRequestPermission: () -> Unit) {
    // Bouncing camera icon animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFFf093fb)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated camera icon with glow
            Box(
                modifier = Modifier
                    .offset(y = (-bounceOffset).dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Camera Access Needed",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To detect ingredients and find amazing recipes, we need access to your camera. Don't worry, we respect your privacy!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Fun permission button
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF764ba2)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Enable Camera",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fun emoji hint
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🥕", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🍅", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🥦", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("🍳", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("✨", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Scan ingredients, discover recipes!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

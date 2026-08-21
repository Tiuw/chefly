package com.skripsi.chefly.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skripsi.chefly.data.model.DetectedIngredient
import com.skripsi.chefly.ui.theme.*
import com.skripsi.chefly.ui.viewmodel.CameraViewModel
import com.skripsi.chefly.util.toDatabaseKey
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CameraScreen(
    onAddMoreClick: () -> Unit,
    onNavigateToResult: (List<String>) -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var activeTabUiState by remember { mutableIntStateOf(0) } // 0 = Kamera, 1 = Galeri
    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .build()
    }

    val capturedImage by viewModel.capturedImage.collectAsStateWithLifecycle()
    val uploadedImage by viewModel.uploadedImage.collectAsStateWithLifecycle()
    val imageDetections by viewModel.imageDetections.collectAsStateWithLifecycle()
    val isProcessingImage by viewModel.isProcessingImage.collectAsStateWithLifecycle()

    val activeDisplayBitmap = capturedImage ?: uploadedImage

    // Kontrol Lampu Flash / Torch
    LaunchedEffect(isFlashOn, camera) {
        camera?.cameraControl?.enableTorch(isFlashOn)
    }

    // Launcher Galeri
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { btm ->
                    viewModel.setUploadedImage(btm)
                    activeTabUiState = 1
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Error gallery: ${e.message}", e)
            }
        }
    }

    // Permission Check
    var hasCamPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )

    // Fitur UX: Auto expand sheet saat inferensi selesai
    LaunchedEffect(activeDisplayBitmap, imageDetections) {
        if (activeDisplayBitmap != null && imageDetections.isNotEmpty()) {
            delay(150)
            scaffoldState.bottomSheetState.expand()
        } else if (activeDisplayBitmap == null) {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 84.dp,
        sheetContainerColor = PureSurface,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetShadowElevation = 10.dp,
        sheetDragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .background(WhisperBorder, CircleShape)
                )
            }
        },
        sheetContent = {
            DetectedIngredientsSheetContentContent(
                detectedItems = imageDetections,
                onAddMoreClick = {
                    viewModel.saveCurrentDetectionsToRepository()
                    onAddMoreClick()
                },
                onSearchRecipesClick = { selectedList ->
                    viewModel.saveCurrentDetectionsToRepository()
                    val dbKeys = selectedList.map { it.toDatabaseKey() }
                    onNavigateToResult(listOf(dbKeys.joinToString(",")))
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {

            // --- 1. AREA PREVIEW / HASIL (Full Screen) ---
            if (activeTabUiState == 0 && capturedImage == null) {
                // LIVE CAMERA PREVIEW
                if (hasCamPermission) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("CameraScreen", "Binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                }
            } else {
                // STATIC IMAGE (Hasil Jepretan / Galeri)
                activeDisplayBitmap?.let { btm ->
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        Image(
                            bitmap = btm.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        // Bounding Boxes Rendering
                        imageDetections.forEach { detection ->
                            RenderBoundingBox(
                                detection = detection,
                                containerWidth = constraints.maxWidth.toFloat(),
                                containerHeight = constraints.maxHeight.toFloat(),
                                imgTargetW = btm.width.toFloat(),
                                imgTargetH = btm.height.toFloat()
                            )
                        }
                    }
                }
            }

            // --- 2. OVERLAY CONTROLS ---

            // Tombol Flash (Top Left - Khusus Live Camera)
            if (activeTabUiState == 0 && capturedImage == null) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = 16.dp, start = 16.dp)
                        .size(42.dp)
                        .clickable { isFlashOn = !isFlashOn }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Senter",
                            tint = if (isFlashOn) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Tab Switcher (Top Center)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabItemPill("Kamera", Icons.Default.PhotoCamera, activeTabUiState == 0) {
                        activeTabUiState = 0
                        viewModel.resetCapture()
                    }
                    TabItemPill("Galeri", Icons.Default.Collections, activeTabUiState == 1) {
                        activeTabUiState = 1
                        galleryLauncher.launch("image/*")
                    }
                }
            }

            // Tombol Kanan Atas: Buka Ulang Galeri atau Foto Ulang Kamera
            if (activeDisplayBitmap != null) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 16.dp, end = 16.dp)
                        .size(42.dp)
                        .clickable {
                            if (activeTabUiState == 1) {
                                galleryLauncher.launch("image/*")
                            } else {
                                viewModel.resetCapture()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (activeTabUiState == 1) Icons.Default.Collections else Icons.Default.Refresh,
                            contentDescription = if (activeTabUiState == 1) "Ganti Gambar Galeri" else "Foto Ulang",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Shutter Button (Bottom Center)
            if (activeTabUiState == 0 && capturedImage == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .size(76.dp)
                        .border(3.5.dp, Color.White, CircleShape)
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            imageCapture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        var bitmap = image.toBitmap()
                                        if (bitmap != null) {
                                            val rotationDegrees = image.imageInfo.rotationDegrees
                                            if (rotationDegrees != 0) {
                                                val matrix = android.graphics.Matrix().apply {
                                                    postRotate(rotationDegrees.toFloat())
                                                }
                                                bitmap = Bitmap.createBitmap(
                                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                                                )
                                            }
                                            viewModel.processCapturedPhoto(bitmap)
                                        }
                                        image.close()
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CameraScreen", "Gagal mengambil foto: ${exception.message}", exception)
                                    }
                                }
                            )
                        }
                )
            }

            // Loading AI Overlay
            if (isProcessingImage) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Black.copy(0.75f),
                        border = BorderStroke(1.dp, Color.White.copy(0.15f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Terracotta,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Mendeteksi Bahan Makanan...",
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES & FUNCTIONS ---

@Composable
fun TabItemPill(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (isSelected) Terracotta else Color.Transparent,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PureSurface else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) PureSurface else Color.White.copy(alpha = 0.7f),
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectedIngredientsSheetContentContent(
    detectedItems: List<DetectedIngredient>,
    onAddMoreClick: () -> Unit,
    onSearchRecipesClick: (List<String>) -> Unit
) {
    val uniqueIngredients = remember(detectedItems) {
        detectedItems.map { it.label }.distinct()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bahan Terdeteksi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = DeepCharcoal,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = if (uniqueIngredients.isNotEmpty()) "Hasil pindai kamera" else "Arahkan kamera ke bahan makanan",
                    fontSize = 11.5.sp,
                    color = SecondaryText,
                    fontWeight = FontWeight.Medium
                )
            }

            Surface(
                color = CheflySurfaceContainerLow,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${uniqueIngredients.size} BAHAN",
                    color = Terracotta,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            uniqueIngredients.forEach { ingredient ->
                IngredientChip(ingredient)
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, Terracotta.copy(alpha = 0.35f)),
                color = Terracotta.copy(alpha = 0.08f),
                modifier = Modifier.clickable { onAddMoreClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tambah Manual",
                        color = Terracotta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onSearchRecipesClick(uniqueIngredients) },
            enabled = uniqueIngredients.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Terracotta,
                disabledContainerColor = CheflySurfaceContainerLow
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (uniqueIngredients.isNotEmpty()) PureSurface else SecondaryText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (uniqueIngredients.isNotEmpty()) "Cari Resep Cocok" else "Pindai Bahan Dahulu",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uniqueIngredients.isNotEmpty()) PureSurface else SecondaryText
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
fun IngredientChip(name: String) {
    val cleanName = remember(name) {
        name.trim().lowercase().replaceFirstChar { it.uppercase() }
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E3DA)),
        color = Color(0xFFF6F3EE)
    ) {
        Text(
            text = cleanName,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepCharcoal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun RenderBoundingBox(
    detection: DetectedIngredient,
    containerWidth: Float,
    containerHeight: Float,
    imgTargetW: Float,
    imgTargetH: Float
) {
    val box = detection.boundingBox

    val isModelOutputNormalized = box.left in 0.0f..1.0f && box.right in 0.0f..1.0f

    val normLeft = if (isModelOutputNormalized) box.left else box.left / 640f
    val normTop = if (isModelOutputNormalized) box.top else box.top / 640f
    val normRight = if (isModelOutputNormalized) box.right else box.right / 640f
    val normBottom = if (isModelOutputNormalized) box.bottom else box.bottom / 640f

    val scale = minOf(containerWidth / imgTargetW, containerHeight / imgTargetH)
    val fitWidth = imgTargetW * scale
    val fitHeight = imgTargetH * scale

    val offsetX = (containerWidth - fitWidth) / 2f
    val offsetY = (containerHeight - fitHeight) / 2f

    val leftPx = (normLeft * fitWidth) + offsetX
    val topPx = (normTop * fitHeight) + offsetY
    val widthPx = (normRight - normLeft) * fitWidth
    val heightPx = (normBottom - normTop) * fitHeight

    val density = LocalDensity.current
    val leftDp = with(density) { leftPx.toDp() }
    val topDp = with(density) { topPx.toDp() }
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    Box(
        modifier = Modifier
            .offset(x = leftDp, y = topDp)
            .size(width = widthDp, height = heightDp)
            .border(2.dp, Terracotta, RoundedCornerShape(6.dp))
    ) {
        Surface(
            color = Terracotta,
            modifier = Modifier.align(Alignment.TopStart),
            shape = RoundedCornerShape(topStart = 4.dp, bottomEnd = 6.dp)
        ) {
            Text(
                text = "${detection.label} ${(detection.confidence * 100).toInt()}%",
                color = PureSurface,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun ImageProxy.toBitmap(): Bitmap? {
    try {
        if (this.format == ImageFormat.YUV_420_888) {
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val imageBytes = out.toByteArray()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } else if (this.format == 1 || this.format == 0x2a || this.format == ImageFormat.UNKNOWN) {
            val buffer = planes[0].buffer
            buffer.rewind()
            val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }
    } catch (e: Exception) {
        Log.e("Chefly_toBitmap", "Gagal konversi ImageProxy ke Bitmap", e)
    }
    return null
}
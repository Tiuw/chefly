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
import androidx.compose.foundation.*
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
import com.skripsi.chefly.ui.theme.DeepCharcoal
import com.skripsi.chefly.ui.theme.SoftSage
import com.skripsi.chefly.ui.theme.Terracotta
import com.skripsi.chefly.ui.theme.WhisperBorder
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

    // FITUR UX: Otomatis drag Bottom Sheet ke atas setelah foto berhasil di-capture/di-upload & dideteksi
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
        sheetPeekHeight = 80.dp,
        sheetContainerColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetDragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.LightGray, CircleShape)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Geser ke atas untuk lihat bahan",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        sheetContent = {
            DetectedIngredientsSheetContentContent(
                detectedItems = imageDetections,
                onAddMoreClick = {
                    // 🟢 Simpan hasil deteksi ke repository sebelum navigasi ke tambah bahan
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
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {

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
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
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

            // Tab Switcher (Top Center)
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
                    .background(Color.Black.copy(0.6f), CircleShape)
                    .padding(4.dp)
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

            // Shutter Button (Bottom Center - Muncul saat Live Kamera)
            if (activeTabUiState == 0 && capturedImage == null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp)
                        .size(80.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
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

            // Tombol Kanan Atas: Buka Ulang Galeri atau Foto Ulang Kamera
            if (activeDisplayBitmap != null) {
                IconButton(
                    onClick = {
                        if (activeTabUiState == 1) {
                            galleryLauncher.launch("image/*")
                        } else {
                            viewModel.resetCapture()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 40.dp, end = 20.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (activeTabUiState == 1) Icons.Default.Collections else Icons.Default.Refresh,
                        contentDescription = if (activeTabUiState == 1) "Ganti Gambar Galeri" else "Foto Ulang",
                        tint = Color.White
                    )
                }
            }

            // Loading AI Overlay
            if (isProcessingImage) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("AI YOLO26 sedang mendeteksi...", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES & FUNCTIONS ---

@Composable
fun TabItemPill(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) Terracotta else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectedIngredientsSheetContentContent(
    detectedItems: List<DetectedIngredient>,
    onAddMoreClick: () -> Unit,
    onSearchRecipesClick: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Bahan Terdeteksi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepCharcoal)
            Surface(color = Color(0xFFFFF1ED), shape = RoundedCornerShape(8.dp)) {
                Text(
                    text = "${detectedItems.size} ITEM",
                    color = Terracotta,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val uniqueIngredients = detectedItems.map { it.label }.distinct()

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            uniqueIngredients.forEach { ingredient ->
                IngredientChip(ingredient)
            }

            Surface(
                shape = CircleShape,
                border = BorderStroke(1.dp, Terracotta),
                color = Color(0xFFFFF1ED),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAddMoreClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, null, tint = Terracotta, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Tambah Lagi", color = Terracotta, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSearchRecipesClick(uniqueIngredients) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Cari Resep", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun IngredientChip(name: String) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(1.dp, WhisperBorder),
        color = Color.White
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = SoftSage, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(name, fontSize = 14.sp)
        }
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
            .border(2.dp, Terracotta, RoundedCornerShape(4.dp))
    ) {
        Surface(
            color = Terracotta,
            modifier = Modifier.align(Alignment.TopStart),
            shape = RoundedCornerShape(bottomEnd = 4.dp)
        ) {
            Text(
                text = "${detection.label} ${(detection.confidence * 100).toInt()}%",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
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
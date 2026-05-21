package com.skripsi.chefly.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skripsi.chefly.data.model.DetectedIngredient
import com.skripsi.chefly.ui.viewmodel.CameraViewModel
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.Executors

// Import Eksplisit Ikon Extended Material
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PublishedWithChanges
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Collections
import com.skripsi.chefly.ui.theme.DeepCharcoal
import com.skripsi.chefly.ui.theme.SoftSage
import com.skripsi.chefly.ui.theme.WhisperBorder
import com.skripsi.chefly.util.toDatabaseKey

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CameraScreen(
    onAddMoreClick: () -> Unit,
    onNavigateToResult: (List<String>) -> Unit, // Menerima callback data list string bahan dasar hasil deteksi
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastAnalyzedTimestamp by remember { mutableStateOf(0L) }

    // State internal untuk UI Tab (0 = Kamera, 1 = Galeri)
    var activeTabUiState by remember { mutableStateOf(0) }

    // State penampung instance camera provider agar bisa dilepas bersih saat pindah page
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // LOCK NAVIGASI FLAG: Menandakan apakah halaman ini masih aktif dibuka user atau tidak
    var isScreenActive by remember { mutableStateOf(true) }

    // Ambil data State dari ViewModel
    val cameraDetections by viewModel.detections.collectAsStateWithLifecycle()
    val uploadedImage by viewModel.uploadedImage.collectAsStateWithLifecycle()
    val imageDetections by viewModel.imageDetections.collectAsStateWithLifecycle()
    val isProcessingImage by viewModel.isProcessingImage.collectAsStateWithLifecycle()

    // Sinkronisasikan deteksi yang dikirim ke Sheet dengan Tab UI yang aktif
    val activeDetectionsForSheet = if (activeTabUiState == 0) cameraDetections else imageDetections
    val isScanningDotActive = (activeTabUiState == 1 && uploadedImage != null)

    // Launcher untuk mengambil gambar dari galeri HP
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { btm ->
                    viewModel.setUploadedImage(btm)
                    viewModel.processUploadedImage(btm)
                    activeTabUiState = 1
                }
            } catch (e: Exception) {
                Log.e("CameraScreen", "Gagal memuat gambar dari galeri", e)
            }
        }
    }

    // FIX MUTLAK NAVIGASI NAVBAR: Putus total hubungan thread kamera saat pindah menu
    DisposableEffect(Unit) {
        onDispose {
            isScreenActive = false // Ubah status screen menjadi tidak aktif untuk menolak frame baru
            try {
                cameraProviderInstance?.unbindAll()
                Log.d("CameraScreen", "🔒 Sinyal CameraX berhasil dilepas total (Navbar Lancar).")
            } catch (e: Exception) {
                Log.e("CameraScreen", "Gagal unbind camera provider", e)
            }
            cameraExecutor.shutdown()
            viewModel.clearCameraDetections()
            viewModel.clearUploadedImageDetections()
        }
    }

    var hasCamPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCamPermission = granted }
    )

    LaunchedEffect(Unit) {
        viewModel.initializeDetector(context)
        if (!hasCamPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = Color.White,
        sheetContentColor = DeepCharcoal,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetShadowElevation = 10.dp,
        sheetPeekHeight = 220.dp,
        sheetContent = {
            DetectedIngredientsSheetContentContent(
                detectedItems = activeDetectionsForSheet,
                onAddMoreClick = {
                    viewModel.saveCurrentDetectionsToRepository(isGallery = (activeTabUiState == 1))
                    onAddMoreClick()
                },
                onSearchRecipesClick = { selectedList ->
                    viewModel.saveCurrentDetectionsToRepository(isGallery = (activeTabUiState == 1))

                    // 1. 🟢 GUNAKAN FUNGSI EKSTENSI: Map setiap item ke database key bersih kamu
                    val dbNormalizedIngredients = selectedList.map { it.toDatabaseKey() }

                    // 2. Gabungkan menjadi satu string CSV teks biasa dengan pembatas koma
                    val searchString = dbNormalizedIngredients.joinToString(", ")

                    // 3. Encode URI dan lempar ke rute query RecipeScreen utama
                    val encodedQuery = Uri.encode(searchString)

                    // 4. Trigger navigasi lewat callback onNavigateToResult (atau langsung via navController)
                    onNavigateToResult(listOf(searchString))
                }
            )
        },
        topBar = { CameraTopBar() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerWidth = constraints.maxWidth.toFloat()
                val containerHeight = constraints.maxHeight.toFloat()

                if (activeTabUiState == 0) {
                    // --- MODE 1: KAMERA LIVE ---
                    if (hasCamPermission) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { previewView ->
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()
                                    cameraProviderInstance = cameraProvider

                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                        .build()

                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val currentTimestamp = System.currentTimeMillis()

                                        if (isScreenActive && currentTimestamp - lastAnalyzedTimestamp >= 3500L) {
                                            lastAnalyzedTimestamp = currentTimestamp
                                            var bitmap = imageProxy.toBitmap()
                                            if (bitmap != null) {
                                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                                if (rotationDegrees != 0) {
                                                    val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                                }
                                                if (isScreenActive) {
                                                    viewModel.processCameraFrame(bitmap)
                                                }
                                            }
                                        }
                                        imageProxy.close()
                                    }

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    try {
                                        cameraProvider.unbindAll()
                                        if (isScreenActive) {
                                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CameraScreen", "Gagal memuat lifecycle CameraX", e)
                                    }
                                }, ContextCompat.getMainExecutor(context))
                            }
                        )

                        // Render Bounding Box Kamera Live
                        cameraDetections.forEach { detection ->
                            RenderBoundingBox(
                                detection = detection,
                                containerWidth = containerWidth,
                                containerHeight = containerHeight,
                                imgTargetW = 480f,
                                imgTargetH = 640f,
                                isKameraLive = true
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Butuh izin kamera untuk pemindaian real-time.", color = Color.White, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    // --- MODE 2: GALERI UPLOAD ---
                    if (uploadedImage != null) {
                        val bitmap = uploadedImage!!
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Render Bounding Box Gambar Statis Galeri
                        imageDetections.forEach { detection ->
                            RenderBoundingBox(
                                detection = detection,
                                containerWidth = containerWidth,
                                containerHeight = containerHeight,
                                imgTargetW = bitmap.width.toFloat(),
                                imgTargetH = bitmap.height.toFloat(),
                                isKameraLive = false
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(24.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Belum ada gambar terpilih", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Terracotta)
                            ) {
                                Text("Pilih Gambar")
                            }
                        }
                    }

                    if (isProcessingImage) {
                        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Terracotta)
                        }
                    }
                }
            }

            // --- MENU PILIHAN TAB ATAS ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val activeTabModifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Terracotta)
                    .padding(horizontal = 16.dp, vertical = 8.dp)

                val inactiveTabModifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        if (activeTabUiState == 0) {
                            try { cameraProviderInstance?.unbindAll() } catch (_: Exception) {}
                        }
                        activeTabUiState = if (activeTabUiState == 0) 1 else 0
                    }

                Row(modifier = if (activeTabUiState == 0) activeTabModifier else inactiveTabModifier, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kamera", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(modifier = if (activeTabUiState == 1) activeTabModifier else inactiveTabModifier, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Collections, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Galeri", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (activeTabUiState == 1 && uploadedImage != null) {
                SmallFloatingActionButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                        .border(1.dp, Color.White.copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.PublishedWithChanges, null, modifier = Modifier.size(18.dp))
                }
            }

            // --- INDIKATOR STATUS AI AKTIF ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 236.dp, end = 16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScanningDot(isActiveAI = isScanningDotActive)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (activeTabUiState == 0) "PEMINDAIAN AI NMS-FREE AKTIF" else "INFERENSI CITRA YOLO26",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ScanningDot(isActiveAI: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    val dotColor = if (isActiveAI) SoftSage else Terracotta
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = alpha))
    )
}

@Composable
fun DetectionBox(label: String, modifier: Modifier) {
    Box(modifier = modifier.border(2.dp, Terracotta, RoundedCornerShape(8.dp))) {
        Surface(
            color = Terracotta,
            shape = RoundedCornerShape(bottomEnd = 8.dp),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.5f))
        )

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

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(40.dp))

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
        Spacer(modifier = Modifier.height(24.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTopBar() {
    CenterAlignedTopAppBar(
        title = { Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 24.sp) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun RenderBoundingBox(
    detection: DetectedIngredient,
    containerWidth: Float,
    containerHeight: Float,
    imgTargetW: Float,
    imgTargetH: Float,
    isKameraLive: Boolean
) {
    val box = detection.boundingBox

    val leftPx: Float
    val topPx: Float
    val widthPx: Float
    val heightPx: Float

    val isModelOutputNormalized = box.left in 0.0f..1.0f && box.right in 0.0f..1.0f

    val normLeft = if (isModelOutputNormalized) box.left else box.left / 640f
    val normTop = if (isModelOutputNormalized) box.top else box.top / 640f
    val normRight = if (isModelOutputNormalized) box.right else box.right / 640f
    val normBottom = if (isModelOutputNormalized) box.bottom else box.bottom / 640f

    if (isKameraLive) {
        val scale = maxOf(containerWidth / imgTargetW, containerHeight / imgTargetH)
        val scaledWidth = imgTargetW * scale
        val scaledHeight = imgTargetH * scale

        val offsetX = (containerWidth - scaledWidth) / 2f
        val offsetY = (containerHeight - scaledHeight) / 2f

        leftPx = (normLeft * scaledWidth) + offsetX
        topPx = (normTop * scaledHeight) + offsetY
        widthPx = (normRight - normLeft) * scaledWidth
        heightPx = (normBottom - normTop) * scaledHeight
    } else {
        val scale = minOf(containerWidth / imgTargetW, containerHeight / imgTargetH)
        val fitWidth = imgTargetW * scale
        val fitHeight = imgTargetH * scale

        val offsetX = (containerWidth - fitWidth) / 2f
        val offsetY = (containerHeight - fitHeight) / 2f

        leftPx = (normLeft * fitWidth) + offsetX
        topPx = (normTop * fitHeight) + offsetY
        widthPx = (normRight - normLeft) * fitWidth
        heightPx = (normBottom - normTop) * fitHeight
    }

    val density = LocalDensity.current
    val leftDp = with(density) { leftPx.toDp() }
    val topDp = with(density) { topPx.toDp() }
    val widthDp = with(density) { widthPx.toDp() }
    val heightDp = with(density) { heightPx.toDp() }

    DetectionBox(
        label = "${detection.label} (${(detection.confidence * 100).toInt()}%)",
        modifier = Modifier
            .offset(x = leftDp, y = topDp)
            .size(width = widthDp, height = heightDp)
    )
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
        }
        else if (this.format == 1 || this.format == 0x2a || this.format == ImageFormat.UNKNOWN) {
            val buffer = planes[0].buffer
            buffer.rewind()
            val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            return bitmap
        }
    } catch (e: Exception) {
        Log.e("Chefly_toBitmap", "Gagal konversi ImageProxy ke Bitmap: ${e.message}", e)
    }
    return null
}
package com.skripsi.chefly.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// --- Palette Warna ---
val DeepCharcoal = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onAddMoreClick: () -> Unit

) {
    // State untuk mengontrol Bottom Sheet
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = Color.White,
        sheetContentColor = DeepCharcoal,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetShadowElevation = 10.dp,
        sheetPeekHeight = 220.dp, // Tinggi sheet saat tertutup sebagian
        sheetContent = {
            DetectedIngredientsSheetContentContent(onAddMoreClick = onAddMoreClick)
        },
        topBar = {
            CameraTopBar()
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // 1. Background Viewfinder (Simulasi Kamera)
            AsyncImage(
                model = "https://images.unsplash.com/photo-1556910103-1c02745aae4d", // Ganti dengan Camera Preview nantinya
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.8f
            )

            // 2. Detection Bounding Boxes
            // Tomat
            DetectionBox(
                label = "Tomat",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 60.dp, y = 100.dp),
                size = 120.dp
            )

            // Selasih
            DetectionBox(
                label = "Selasih",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-40).dp, y = (-20).dp),
                size = 100.dp
            )

            // 3. AI Active Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScanningDot()
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "PEMINDAIAN AI AKTIF",
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
fun ScanningDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Terracotta.copy(alpha = alpha))
    )
}

@Composable
fun DetectionBox(label: String, modifier: Modifier, size: Dp) {
    Box(modifier = modifier) {
        // Label di atas box
        Surface(
            color = Terracotta,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            modifier = Modifier.offset(y = (-24).dp)
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        // Border Deteksi
        Box(
            modifier = Modifier
                .size(size)
                .border(2.dp, Terracotta, RoundedCornerShape(8.dp))
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectedIngredientsSheetContentContent(
    onAddMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // --- HANDLE (Garis kecil di atas sheet) ---
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.5f))
        )

        // --- HEADER SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bahan Terdeteksi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepCharcoal
            )

            // Badge Jumlah Item
            Surface(
                color = Color(0xFFFFF1ED),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "3 ITEM",
                    color = Terracotta,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CHIPS GRID (FlowRow agar otomatis pindah baris) ---
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Data dummy atau bisa diambil dari List State
            val ingredients = listOf("Tomat", "Selasih", "Bawang Putih")

            ingredients.forEach { ingredient ->
                IngredientChip(ingredient)
            }

            // --- TOMBOL TAMBAH LAGI ---
            Surface(
                shape = CircleShape,
                border = BorderStroke(1.dp, Terracotta),
                color = Color(0xFFFFF1ED),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAddMoreClick() } // Memanggil fungsi navigasi
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Terracotta,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tambah Lagi",
                        color = Terracotta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // --- ACTION BUTTON (CARI RESEP) ---
        Button(
            onClick = { /* Implementasi Pencarian Resep */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Cari Resep",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Memberikan padding bawah ekstra agar nyaman di layar full screen
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun IngredientChip(name: String) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(1.dp, Color(0x1A000000)),
        color = Color.White
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF8FAF9B), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(name, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
    )
}
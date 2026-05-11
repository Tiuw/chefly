package com.skripsi.chefly.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// --- Palette Warna ---
val DeepCharcoal = Color(0xFF1A1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen() {
    Scaffold(
        topBar = { ScanTopBar() },
        bottomBar = { ScanBottomNav() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(DeepCharcoal)
        ) {
            // 1. Viewfinder (Simulasi Kamera)
            AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD4LPwpVOM1OS0kOmNvWkw1gbcXtGUZZU3J3-GkQtAwfqLhZtuPhgpF3X8H5AZcBobSxEB7Yqx8_SHdEMnyV4hiWteiaqT4B84egndPxc8SegPdO8QdxLf98TGIa8IVQMib2f4drTttygzq50MR-PhLKwHrZ1wFj-OSTe4QsO0qb5Fb4xJ7nd0sOLp2Lxsv-XUkjVFardMV972w2w1liVbmnGTcl7VyiMDLllRqdkxhaedMgZdQjy7HU4EOsxdhunKwntGa9B_RtRI",
                contentDescription = "Camera Viewfinder",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder Overlay Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            // 2. AI Bounding Boxes (Simulasi Deteksi YOLO)
            DetectionBox(label = "Tomat", top = 0.2f, left = 0.15f, width = 120, height = 120)
            DetectionBox(label = "Selasih", top = 0.45f, left = 0.55f, width = 100, height = 80)
            DetectionBox(label = "Bawang Putih", top = 0.65f, left = 0.25f, width = 70, height = 70)

            // 3. Status AI (Pojok kanan atas)
            AIStatusBadge()

            // 4. Bottom Sheet (Simulasi Draggable)
            ScanBottomSheet(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun DetectionBox(label: String, top: Float, left: Float, width: Int, height: Int) {
    // Kita gunakan Box induk sebagai kanvas kamera
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                // Menggunakan absoluteOffset untuk posisi presisi (x, y)
                // left dan top adalah normalisasi (0.0 - 1.0) dari koordinat YOLO
                .absoluteOffset(
                    x = (left * 350).dp, // Sesuaikan pengali dengan lebar layar preview
                    y = (top * 600).dp   // Sesuaikan pengali dengan tinggi layar preview
                )
        ) {
            // Label Objek (Label di atas kotak)
            Surface(
                color = Terracotta,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Kotak Deteksi (Bounding Box)
            Box(
                modifier = Modifier
                    .size(width.dp, height.dp)
                    .border(
                        width = 2.dp,
                        color = Terracotta,
                        shape = RoundedCornerShape(
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp,
                            topEnd = 8.dp
                        )
                    )
            )
        }
    }
}

@Composable
fun AIStatusBadge() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.TopEnd) {
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Terracotta.copy(alpha = alpha), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("PEMINDAIAN AI AKTIF", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScanBottomSheet(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .size(40.dp, 4.dp)
                    .background(Color.LightGray, CircleShape)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bahan Terdeteksi", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Surface(color = Color(0xFFFFF1ED), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "3 ITEM",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Terracotta, fontWeight = FontWeight.Bold, fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chips Grid
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DetectedChip("Tomat")
                DetectedChip("Selasih")
                DetectedChip("Bawang Putih")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Search Action */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                shape = CircleShape
            ) {
                Icon(Icons.Default.RestaurantMenu, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cari Resep", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetectedChip(label: String) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(1.dp, WhisperBorder),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, null, tint = SoftSage, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanTopBar() {
    TopAppBar(
        title = { Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = {}) { Icon(Icons.Default.Menu, null, tint = Terracotta) }
        },
        actions = {
            Surface(modifier = Modifier.size(32.dp), shape = CircleShape) {
                AsyncImage(model = "https://lh3.googleusercontent.com/aida-public/AB6AXuD__2jgtirZ6ue_yohrR4E5QGW8BQJSd1pQGJwcAee9FVqpXBW5Y_R4l4T0kkmIJ1zBf_1Il_S_lLI6oaNF8-2u-59Fsj7DWQk85-K8-65V1HH0wwen_mjPoHCQ7eanDGBdRe9Q87xKqgHyq5tsZeXCtkXb-YSJZLZTlilROhw_kQ2A15v6Muf-DoZonppSkgCs7Qg2hml1nshPb5X1iVzJerMhLtbG36gl6078Ysbr9Q0apfabKUqNCMZPbFAIe5MlV-aGPqQ-qv4", contentDescription = null)
            }
            Spacer(modifier = Modifier.width(16.dp))
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun ScanBottomNav() {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Beranda") })
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.CenterFocusStrong, null) },
            label = { Text("Pindai") },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = Terracotta, indicatorColor = Color(0xFFFFF1ED))
        )
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.RestaurantMenu, null) }, label = { Text("Resep") })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(Icons.Default.Bookmark, null) }, label = { Text("Tersimpan") })
    }
}

@Preview(showBackground = true, device = "spec:width=430dp,height=932dp")
@Composable
fun PreviewCameraScan() {
    CameraScanScreen()
}
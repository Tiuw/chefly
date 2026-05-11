package com.skripsi.chefly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.navigation.NavHostController
import coil.compose.AsyncImage

// --- Palette Warna Berdasarkan Tailwind Config ---
val Terracotta = Color(0xFFE36C47)
val WarmIvory = Color(0xFFFAF7F2)
val SoftSage = Color(0xFF8FAF9B)
val ErrorCoral = Color(0xFFEF4444)
val OnSurfaceVariant = Color(0xFF57423C)
val WhisperBorder = Color(0xFFCBD5E1).copy(alpha = 0.3f)
val SurfaceContainerLow = Color(0xFFFFF1ED)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Chefly", color = Terracotta, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp()}) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Terracotta)
                    }
                },
                actions = {
                    // Avatar Profil User
                    Surface(
                        modifier = Modifier.size(32.dp).padding(end = 8.dp),
                        shape = CircleShape,
                        color = Color.LightGray
                    ) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCGxUl6Kmn2Zx27Ez8rgBxD-MBxDpEDDIH0jlX_Q5_crxFm-Wnns3Udo1Miee4Ex9Fuie-ttAb6cHAOKCtxU4AIPnPXNHDT-ShORQxf6CKYg4Yaw-OFGkBtFFFhn80iLvvYGLAI1sIHgIPD1Nvpg_a9h1Xj4MEdnpzdEvxARE3lD2u7RvxaQpZAml2p6SoXlQihfEhiL6t1RYbIswCXC20WnsUse7hKfvH6tNNEd8EwjIwBdqHGV-ekeHjXlFZOfH-1tLEzZlGbntY",
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Hero Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBN4dxPs-rE09OpGPVUEF79QbzVDC8JwcsUeujvZ8ZkWQnfQVSrVK0gyF8EucKffjp9dYF_-Xa46KHzVwF3cw-aE1anSHBzCIOEn7JK8oUkFq5gyWPjenprHHezwgqVYuObGMwUUljo7LYah9yawSORFVUVMYimHQMPKHwe1VnCST61BNhEe1LAKVdHVyvIvs8auCPWlpw9MKrd3b_G5yh7V8k9U1YlfRBXgO8kTk_g8vmc9QaXr1OPYYup2_GflkbAvccmUdoBEto",
                    contentDescription = "Roasted Summer Vegetables",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Overlay Gradasi (hero-gradient)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 200f
                            )
                        )
                )
                Text(
                    text = "Sayuran Panggang Musim Panas",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            // --- Content Canvas ---
            Column(modifier = Modifier.padding(16.dp)) {

                // Match Bar (Kecocokan Bahan)
                IngredientMatchBar(85)

                Spacer(modifier = Modifier.height(24.dp))

                // Bagian Bahan-bahan
                IngredientsSection()

                // Divider (Border-t)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    thickness = 1.dp,
                    color = WhisperBorder
                )

                // Bagian Instruksi
                InstructionsSection()

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun IngredientMatchBar(percentage: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "KECOCOKAN BAHAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Text(
                "$percentage%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Terracotta
            )
        }
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = Terracotta,
            trackColor = WhisperBorder
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IngredientsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Bahan yang diperlukan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            IngredientChip("2 Paprika", isCheck = true)
            IngredientChip("1 Sukini", isCheck = true)
            IngredientChip("Tomat Ceri", isCheck = true)
            IngredientChip("Thyme Segar", isCheck = false)
            IngredientChip("Minyak Zaitun", isCheck = true)
        }
    }
}

@Composable
fun IngredientChip(name: String, isCheck: Boolean) {
    Surface(
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (isCheck) WhisperBorder else ErrorCoral.copy(alpha = 0.3f)
        ),
        color = if (isCheck) Color.White else ErrorCoral.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isCheck) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                contentDescription = null,
                tint = if (isCheck) SoftSage else ErrorCoral,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = name,
                fontSize = 14.sp,
                color = if (isCheck) OnSurfaceVariant else ErrorCoral
            )
        }
    }
}

@Composable
fun InstructionsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Instruksi",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        InstructionStep("01", "Panaskan oven ke suhu 400°F (200°C). Siapkan loyang besar beralaskan kertas roti agar mudah dibersihkan.")
        InstructionStep("02", "Potong paprika dan sukini menjadi potongan 1 inci yang seragam. Biarkan tomat ceri utuh agar pecah saat dipanggang.")
    }
}

@Composable
fun InstructionStep(number: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Lingkaran Nomor (surface-container-low)
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = SurfaceContainerLow
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    color = Terracotta,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Teks Deskripsi
        Text(
            text = description,
            fontSize = 15.sp,
            color = OnSurfaceVariant,
            lineHeight = 22.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
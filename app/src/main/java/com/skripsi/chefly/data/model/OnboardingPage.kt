package com.skripsi.chefly.data.model

import com.skripsi.chefly.R

data class OnboardingData(
    val title: String,
    val description: String,
    val imageRes: Int?,
    val label1: String = "",
    val label2: String = ""
)

val onboardingPages = listOf(
    // Halaman 1: Ada Gambar & Ada Label Deteksi
    OnboardingData(
        title = "Cek bahan sekejap mata!",
        description = "Scan bahanmu, biar AI yang kerja mencari resep terbaik untukmu hari ini.",
        imageRes = R.drawable.onboarding_1, // Gambar ponsel scan
        label1 = "PAPRIKA",
        label2 = "WORTEL"
    ),
    // Halaman 2: Ada Gambar & Tanpa Label
    OnboardingData(
        title = "Cari jodohnya resep lokal.",
        description = "Cocokkan bahan, temukan rasa yang pas.",
        imageRes = R.drawable.onboarding_2, // Gambar piring rendang
        label1 = "",
        label2 = ""
    ),
    // Halaman 3: Tanpa Gambar & Tanpa Label (Menggunakan Ilustrasi Icon)
    OnboardingData(
        title = "Masak bebas tanpa kuota!",
        description = "Offline terus, inspirasi nggak pernah putus. Akses resep favoritmu kapan saja.",
        imageRes = null, // Tidak menggunakan resource gambar drawable
        label1 = "",
        label2 = ""
    )
)
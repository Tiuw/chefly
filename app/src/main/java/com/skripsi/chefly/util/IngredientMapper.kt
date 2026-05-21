package com.skripsi.chefly.util

fun String.toDatabaseKey(): String {
    // 1. Bersihkan spasi di awal/akhir dan ubah ke lowercase untuk standardisasi
    // 2. Handle kasus khusus (Sinonim / Perbedaan Kata)
    return when (val cleanedLabel = this.trim().lowercase()) {
        "ayam" -> "daging_ayam"
        "biji kemiri" -> "kemiri"
        "kol" -> "kubis"
        "cabai merah" -> "cabe_merah"
        "cabai hijau" -> "cabe_hijau"

        // 3. Handle kasus umum (Tinggal ganti spasi menjadi underscore)
        // Contoh: "bawang merah" otomatis menjadi "bawang_merah"
        // Contoh: "daging sapi" otomatis menjadi "daging_sapi"
        else -> cleanedLabel.replace(" ", "_")
    }
}
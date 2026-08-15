package com.skripsi.chefly.util

fun String.toDatabaseKey(): String {
    return when (val cleanedLabel = this.trim().lowercase()) {
        "ayam" -> "daging_ayam"
        "sapi" -> "daging_sapi" // 🟢 Tambahkan ini
        "kambing" -> "daging_kambing" // 🟢 Tambahkan ini
        "biji kemiri" -> "kemiri"
        "kol" -> "kubis"
        "cabai merah" -> "cabe_merah"
        "cabai hijau" -> "cabe_hijau"
        else -> cleanedLabel.replace(" ", "_")
    }
}

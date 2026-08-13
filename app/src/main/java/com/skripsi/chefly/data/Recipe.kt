package com.skripsi.chefly.data

data class Recipe(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val ingredients: String, // Memetakan dari ui_ingredients di Room
    val steps: String,       // Memetakan dari ui_steps di Room
    val totalIngredients: Int?,
    val totalSteps: Int?,
    val loves: Int?,
    val similarity: Float = 0f,
    val isFavorite: Boolean = false
) {

    /**
     * PARSER OTOMATIS UNTUK BAHAN (INGREDIENTS)
     */
    val ingredientList: List<String> get() {
        val trimmed = ingredients.trim()
        if (trimmed.isBlank()) return emptyList()

        return when {
            // Jalur 1: Menggunakan Regex untuk memotong sekat JSON array [ "item1", "item2" ]
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                trimmed
                    .replace(Regex("""^\[\s*\"|\"\s*\]$"""), "") // Hapus [" di awal dan "] di akhir
                    .split(Regex("""\"\s*,\s*\""""))             // Belah pada sekat "," dengan toleransi spasi
                    .map { it.replace("\\", "").trim() }
                    .filter { it.isNotBlank() && !it.lowercase().startsWith("bumbu") }
            }
            // Jalur 2: Fallback split koma biasa
            else -> {
                trimmed
                    .split(",")
                    .map { it.trim().replace("\"", "") } // Hapus kutip yang tersisa
                    .filter { it.isNotBlank() }
            }
        }
    }

    /**
     * PARSER OTOMATIS UNTUK LANGKAH (STEPS)
     */
    val stepList: List<String> get() {
        val trimmed = steps.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Ekstrak string mentah dari bungkus JSON array jika ada
        val cleanRawString = if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed.replace(Regex("""^\[\s*\"|\"\s*\]$"""), "")
        } else {
            trimmed
        }

        // 2. Strategi Pemecahan (Splitting) menggunakan Regex yang lebih agresif
        val rawSteps = when {
            // Jika ada sekat JSON murni " , " atau ","
            cleanRawString.contains("\",\"") || cleanRawString.contains("\", \"") -> {
                cleanRawString.split(Regex("""\"\s*,\s*\""""))
            }
            // Jika data menggumpal tapi dipisahkan penomoran internal seperti "2) ", "3) "
            cleanRawString.contains(Regex("""\d+[\)\.]\s""")) -> {
                cleanRawString.split(Regex("""\s*,\s*\"\s*\d+[\)\.]\s*|\s*\"\s*,\s*\"\s*|\s*\d+[\)\.]\s*"""))
            }
            // Fallback split koma biasa
            else -> {
                cleanRawString.split(",")
            }
        }

        // 3. Bersihkan sisa-sisa karakter kotor dan penomoran ganda
        return rawSteps
            .map { it.replace("\\", "").replace("\"", "").trim() }
            .map { cleanStepNumbering(it) }
            .filter { it.isNotBlank() && it.length > 3 }
    }

    /**
     * Regex untuk mengikis penomoran kotor bawaan teks database (misal: '1) Siapkan' -> 'Siapkan')
     * Ini krusial agar tampilan nomor di Jetpack Compose tidak double-numbering.
     */
    private fun cleanStepNumbering(text: String): String {
        return text
            .replace(Regex("^\\s*\"?\\s*\\d+[\u0029\\.]\\s*"), "") // Menghapus format '1)' atau '1.' di awal kalimat
            .trim()
    }
}
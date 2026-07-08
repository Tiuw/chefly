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
     * Mengubah string mentah database menjadi List<String> siap pakai di UI
     */
    val ingredientList: List<String> get() {
        val trimmed = ingredients.trim()
        if (trimmed.isBlank()) return emptyList()

        return when {
            // Jalur 1: Jika data berformat Raw JSON Array -> ["Bahan 1", "Bahan 2"]
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                trimmed
                    .replace(Regex("^\\[\\s*\"|\"\\s*\\]$"), "") // Hapus [" di awal dan "] di akhir array
                    .split("\",\"")                              // Potong HANYA pada sekat pembatas JSON asli
                    .map { it.replace("\\", "").trim() }         // Bersihkan sisa escape character jika ada
                    .filter { it.isNotBlank() && !it.startsWith("Bumbu") }
            }
            // Jalur 2: Jika data berformat string biasa dipisah '--' -> Bahan 1--Bahan 2
            trimmed.contains("--") -> {
                trimmed
                    .split("--")
                    .map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("Bumbu") }
            }
            // Jalur 3: Fallback split koma biasa jika tidak memenuhi kondisi di atas
            else -> {
                trimmed
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
        }
    }

    /**
     * PARSER OTOMATIS UNTUK LANGKAH (STEPS)
     * Memotong baris tanpa merusak tanda koma di dalam kalimat instruksi
     */
    val stepList: List<String> get() {
        val trimmed = steps.trim()
        if (trimmed.isBlank()) return emptyList()

        val rawSteps = when {
            // Jalur 1: Jika instruksi berformat Raw JSON Array -> ["1) ...", "2) ..."]
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                trimmed
                    .replace(Regex("^\\[\\s*\"|\"\\s*\\]$"), "") // Hapus [" di awal dan "] di akhir
                    .split("\",\"")                              // Belah murni pada pembatas objek array JSON
                    .map { it.replace("\\", "").trim() }
            }
            // Jalur 2: Jika instruksi dipisah oleh escape karakter baris baru (\n)
            trimmed.contains("\n") -> {
                trimmed.split("\n").map { it.trim() }
            }
            // Jalur 3: Fallback split koma biasa
            else -> {
                trimmed.split(",").map { it.trim() }
            }
        }

        // Lakukan pembersihan teks akhir dari penomoran ganda sebelum dilempar ke Compose
        return rawSteps
            .filter { it.isNotBlank() }
            .map { cleanStepNumbering(it) }
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
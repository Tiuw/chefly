package com.skripsi.chefly.data

/**
 * BEST PRACTICE: The Domain Model.
 */
data class Recipe(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val ingredients: String,
    val steps: String,
    val totalIngredients: Int? = null,
    val totalSteps: Int? = null,
    val loves: Int? = null, // Digunakan untuk Popularitas/Sorting
    val cookingMethod: String? = null,
    val isFavorite: Boolean = false
) {
    val ingredientList: List<String>
        get() = ingredients
            .split(Regex("[,;:\n\\r\\-\\–]+"))
            .map { raw ->
                raw.trim()
                    .replace("_", " ") // Ganti underscore jadi spasi
                    .replace(Regex("[\\[\\]\"']"), "") // Bersihkan karakter sampah jika ada
                    .lowercase() // Kecilkan semua dulu agar seragam
                    .replaceFirstChar { it.uppercase() } // KAPITALKAN HURUF PERTAMA
            }
            .filter { it.isNotEmpty() }

    val stepList: List<String>
        get() {
            val rawSteps = steps.split(Regex("\\r?\\n|\\.\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            return rawSteps.map { step ->
                step.replace(Regex("^\\d+[\\). ]+\\s*"), "") // Hapus "1) ", "2. ", dll
                    .replace(Regex("^-\\s*"), "") // Hapus dash jika ada
                    .lowercase() // Kecilkan semua dulu
                    .replaceFirstChar { it.uppercase() } // KAPITALKAN HURUF PERTAMA
                    .trim()
            }
        }
}
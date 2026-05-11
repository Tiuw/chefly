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
    val loves: Int? = null,
    val cookingMethod: String? = null
) {
    val ingredientList: List<String>
        get() = ingredients // Langsung pakai ingredients karena data cleaned tidak ada
            .split(Regex("[,;:\n\\r\\-\\–]+"))
            .map { it.trim().replace(Regex("\\s{2,}"), " ") }
            .filter { it.isNotEmpty() }

    val stepList: List<String>
        get() {
            val byNewline = steps.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            if (byNewline.size > 1) return byNewline
            val byNumbered = steps.split(Regex("\\d+\\)\\s*|\\d+\\.\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
            if (byNumbered.size > 1) return byNumbered
            return steps.split(Regex("\\.\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        }
}
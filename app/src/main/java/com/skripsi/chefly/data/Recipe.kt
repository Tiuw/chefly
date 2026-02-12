package com.skripsi.chefly.data

import com.google.gson.annotations.SerializedName

/**
 * Domain model for recipes stored in Room and parsed from JSON assets.
 * Fields are kept nullable where the  has them nullable.
 */
data class Recipe(
    @SerializedName("Title")
    val name: String,

    @SerializedName("Ingredients")
    val rawIngredients: String,

    @SerializedName("Steps")
    val instructions: String,

    @SerializedName("URL")
    val imageUrl: String,

    @SerializedName("Category")
    val category: String,

    @SerializedName("Ingredients Cleaned")
    val ingredientsCleaned: String? = null,

    @SerializedName("Total Ingredients")
    val totalIngredients: Int? = null,

    @SerializedName("Loves")
    val loves: Int? = null,

    @SerializedName("Title Cleaned")
    val titleCleaned: String? = null,

    @SerializedName("Total Steps")
    val totalSteps: Int? = null,

    @SerializedName("PrepTime")
    val prepTime: String? = null,

    @SerializedName("CookTime")
    val cookTime: String? = null,

    @SerializedName("Servings")
    val servings: Int? = null,

    @SerializedName("Id")
    val id: String? = null
) {
    /**
     * Returns the ingredient list. Prefer cleaned ingredients when available,
     * otherwise split the rawIngredients string by common delimiters.
     */
    val ingredientList: List<String>
        get() {
            val source = (ingredientsCleaned?.takeIf { it.isNotBlank() } ?: rawIngredients)
            return source
                .split(Regex("[,;:\n\\r\\-\\–]+"))
                .map { it.trim().replace(Regex("\\s{2,}"), " ") }
                .filter { it.isNotEmpty() }
        }

    /**
     * Returns the instruction steps as a list. Attempts to split numbered steps and newline-delimited steps.
     */
    val instructionList: List<String>
        get() {
            // If the instructions are already newline separated, use those
            val byNewline = instructions.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            if (byNewline.size > 1) return byNewline

            // Try splitting by numbered bullets like `1)` `1.` or `1) `
            val byNumbered = instructions.split(Regex("\\d+\\)\\s*|\\d+\\.\\s*"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (byNumbered.size > 1) return byNumbered

            // Fallback: split by sentence (period) but keep reasonably sized chunks
            return instructions.split(Regex("\\.\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
}

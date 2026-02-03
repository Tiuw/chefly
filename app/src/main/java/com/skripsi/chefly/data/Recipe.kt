package com.skripsi.chefly.data

data class Recipe(
    val id: Int,
    val name: String,
    val description: String,
    val imageUrl: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val prepTime: String,
    val cookTime: String,
    val servings: Int,
    val difficulty: String,
    val category: String
)

data class DetectedIngredient(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF
)


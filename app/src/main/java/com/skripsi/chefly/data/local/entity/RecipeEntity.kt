package com.skripsi.chefly.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skripsi.chefly.data.Recipe

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "category")
    val category: String?,

    @ColumnInfo(name = "image_url")
    val imageUrl: String?,

    @ColumnInfo(name = "loves")
    val loves: Int?, // Tetap untuk Popularitas/Sorting TF-IDF atau Cosine

    @ColumnInfo(name = "title")
    val title: String?,

    @ColumnInfo(name = "total_ingredients")
    val totalIngredients: Int?,

    @ColumnInfo(name = "total_steps")
    val totalSteps: Int?,

    @ColumnInfo(name = "ui_ingredients")
    val uiIngredients: String?,

    @ColumnInfo(name = "ui_steps")
    val uiSteps: String?
) {
    fun toDomain(): Recipe = Recipe(
        id = id,
        name = title ?: "No Title",
        imageUrl = imageUrl ?: "",
        category = category ?: "Uncategorized",
        ingredients = uiIngredients ?: "",
        steps = uiSteps ?: "",
        totalIngredients = totalIngredients,
        totalSteps = totalSteps,
        loves = loves
    )
}
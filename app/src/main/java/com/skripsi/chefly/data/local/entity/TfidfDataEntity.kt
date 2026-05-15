package com.skripsi.chefly.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tfidf_data")
data class TfidfDataEntity(
    @PrimaryKey
    @ColumnInfo(name = "recipe_id")
    val recipeId: String, // Primary Key tetap wajib

    @ColumnInfo(name = "vector_json")
    val vectorJson: String? // Tambahkan '?' agar menjadi Nullable sesuai database Found
)
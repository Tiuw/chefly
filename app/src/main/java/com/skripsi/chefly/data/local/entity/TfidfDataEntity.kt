package com.skripsi.chefly.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tfidf_data",
    // 🪛 SUNTIKKAN BAGIAN INI: Daftarkan index agar cocok dengan database asset
    indices = [
        Index(value = ["recipe_id"], name = "idx_tfidf_data_id", unique = false)
    ]
)
data class TfidfDataEntity(
    @PrimaryKey
    @ColumnInfo(name = "recipe_id")
    val recipeId: String,

    @ColumnInfo(name = "vector_json")
    val vectorJson: String?
)
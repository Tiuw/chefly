package com.skripsi.chefly.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "idf_dictionary")
data class IdfDictionaryEntity(
    @PrimaryKey
    @ColumnInfo(name = "ingredient")
    val ingredient: String,

    @ColumnInfo(name = "idf_weight")
    val idfWeight: Double
)
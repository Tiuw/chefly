package com.skripsi.chefly.data.model

import android.graphics.RectF

data class DetectedIngredient(
    val label: String,
    val confidence: Float,
    val boundingBox: RectF
)


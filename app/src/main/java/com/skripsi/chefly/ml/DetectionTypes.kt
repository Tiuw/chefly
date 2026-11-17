package com.skripsi.chefly.ml

import android.graphics.RectF

/**
 * Minimal types used by detector/tracker/analyzer
 */

data class DetectionCamera(
    val box: RectF,
    val confidence: Float,
    val classIndex: Int,
    val className: String
)


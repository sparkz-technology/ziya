package com.example.ziya

import android.graphics.Color
import android.graphics.RadialGradient
import android.graphics.Shader

class GradientManager {
    val orbColors = intArrayOf(
        Color.parseColor("#2D0B5A"),  // Deep purple
        Color.parseColor("#4B1B9A"),  // Medium purple
        Color.parseColor("#6B3BDA"),  // Light purple
        Color.parseColor("#4527A0")   // Dark purple
    )

    val glowColors = intArrayOf(
        Color.parseColor("#4527A0"),  // Dark purple
        Color.parseColor("#6B3BDA"),  // Light purple
        Color.parseColor("#4B1B9A")   // Medium purple
    )

    fun createOrbGradient(cx: Float, cy: Float, radius: Float): RadialGradient {
        return RadialGradient(
            cx, cy, radius * 1.2f,
            orbColors,
            floatArrayOf(0f, 0.3f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }
}
package com.example.ziya

import android.graphics.*

class ShaderEffects {
    private var gradientShader: Shader? = null
    private var lastWidth = 0
    private var lastHeight = 0
    private var lastColor = 0

    fun updateGradientShader(width: Int, height: Int, centerColor: Int, audioLevel: Float) {
        if (width != lastWidth || height != lastHeight || centerColor != lastColor) {
            val colors = intArrayOf(
                centerColor,
                adjustColor(centerColor, 0.7f),
                adjustColor(centerColor, 0.4f)
            )
            
            val center = PointF(width / 2f, height / 2f)
            val radius = (width.coerceAtLeast(height) / 2f) * (1f + audioLevel * 0.3f)
            
            gradientShader = RadialGradient(
                center.x,
                center.y,
                radius,
                colors,
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
            
            lastWidth = width
            lastHeight = height
            lastColor = centerColor
        }
    }

    private fun adjustColor(color: Int, factor: Float): Int {
        return Color.argb(
            Color.alpha(color),
            (Color.red(color) * factor).toInt().coerceIn(0, 255),
            (Color.green(color) * factor).toInt().coerceIn(0, 255),
            (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        )
    }

    fun getGradientShader(): Shader? = gradientShader
}
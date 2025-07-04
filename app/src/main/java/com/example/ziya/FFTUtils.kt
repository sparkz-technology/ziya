package com.example.ziya

import kotlin.math.abs

object FFTUtils {
    fun calculateFftBands(samples: ShortArray, read: Int, bands: Int): FloatArray {
        val fftData = FloatArray(bands)
        for (i in 0 until bands) {
            var sum = 0f
            var count = 0
            for (j in (i * read / bands) until ((i + 1) * read / bands)) {
                if (j < read) {
                    sum += abs(samples[j].toFloat())
                    count++
                }
            }
            fftData[i] = if (count > 0) sum / count / Short.MAX_VALUE else 0f
        }
        return fftData
    }
}
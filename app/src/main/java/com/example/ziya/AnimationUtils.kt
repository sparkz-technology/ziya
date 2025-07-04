package com.example.ziya

import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import kotlin.math.sin

object AnimationUtils {
    fun createWavePattern(time: Float, frequency: Float, amplitude: Float): Float {
        return sin(time * frequency) * amplitude
    }

    fun createComplexWave(time: Float, audioLevel: Float): Float {
        return createWavePattern(time, 1f, 0.3f * audioLevel) +
               createWavePattern(time * 1.5f, 2f, 0.2f * audioLevel) +
               createWavePattern(time * 2f, 3f, 0.1f * audioLevel)
    }

    fun createSmoothAnimator(
        startValue: Float,
        endValue: Float,
        duration: Long,
        interpolator: Interpolator = AccelerateDecelerateInterpolator(),
        repeatCount: Int = 0,
        repeatMode: Int = ValueAnimator.RESTART,
        onUpdate: (Float) -> Unit
    ): ValueAnimator {
        return ValueAnimator.ofFloat(startValue, endValue).apply {
            this.duration = duration
            this.interpolator = interpolator
            this.repeatCount = repeatCount
            this.repeatMode = repeatMode
            addUpdateListener { animator ->
                onUpdate(animator.animatedValue as Float)
            }
        }
    }
}
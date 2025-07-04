package com.example.ziya

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import android.view.WindowManager

class OrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePath = Path()
    private var phase = 0f
    private var audioLevel = 0.1f
    private var fftBands = FloatArray(32)
    private var randomSeed = 0.0
    private var orbMovedListener: ((Int, Int) -> Unit)? = null

    // For drag/move overlay
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var paramsX = 0
    private var paramsY = 0

    init {
        paint.style = Paint.Style.FILL
        glowPaint.style = Paint.Style.STROKE
        glowPaint.strokeWidth = 30f
        glowPaint.maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        randomSeed = Math.random() * 1000.0
    }

    fun updateAudio(level: Float, fft: FloatArray?) {
        audioLevel = level
        fft?.let {
            System.arraycopy(it, 0, fftBands, 0, min(it.size, fftBands.size))
        }
        invalidate()
    }

    fun setOnOrbMovedListener(listener: (Int, Int) -> Unit) {
        orbMovedListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = width.coerceAtMost(height) / 2f * 0.65f
        val dynamicRadius = baseRadius * (1f + 0.3f * audioLevel)
        val color = Color.HSVToColor(
            floatArrayOf(200f + 60f * audioLevel, 0.8f, 1f)
        )
        paint.color = color
        glowPaint.color = color

        // Draw glow
        canvas.drawCircle(cx, cy, dynamicRadius + 26f, glowPaint)

        // Draw wobbly edge
        edgePath.reset()
        val points = 90
        for (i in 0..points) {
            val angle = (2 * PI * i) / points
            val fftVal = (fftBands.getOrNull((i * fftBands.size) / points) ?: 0f)
            val noise = sin(phase + angle + randomSeed) * 0.08f + fftVal * 0.3f
            val r = dynamicRadius * (1f + noise)
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            if (i == 0) edgePath.moveTo(x, y) else edgePath.lineTo(x, y)
        }
        edgePath.close()
        canvas.drawPath(edgePath, paint)

        // Animate
        phase += 0.03f + 0.08f * audioLevel
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val windowManagerParams = layoutParams as? WindowManager.LayoutParams
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                paramsX = windowManagerParams?.x ?: 0
                paramsY = windowManagerParams?.y ?: 0
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - lastTouchX).toInt()
                val dy = (event.rawY - lastTouchY).toInt()
                orbMovedListener?.invoke(paramsX + dx, paramsY + dy)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
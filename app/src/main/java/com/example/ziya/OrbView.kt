package com.example.ziya

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import kotlin.math.*
import kotlin.random.Random

class OrbView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePath = Path()
    
    private var phase = 0f
    private var audioLevel = 0.1f
    private var fftBands = FloatArray(32)
    private var randomSeed = 0.0
    private var pulseScale = 1f
    private var energyLevel = 0f
    
    private val gradientManager = GradientManager()
    private lateinit var gradientShader: Shader
    
    private var pulseAnimator: ValueAnimator? = null
    private var energyAnimator: ValueAnimator? = null
    
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var velocityX = 0f
    private var velocityY = 0f
    private var lastUpdateTime = 0L
    private var orbMovedListener: ((Int, Int) -> Unit)? = null
    
    private val ripples = mutableListOf<Ripple>()

    private data class Ripple(
        var radius: Float,
        var alpha: Float,
        val maxRadius: Float,
        val centerX: Float,
        val centerY: Float
    )

    init {
        setupPaints()
        startAnimations()
        randomSeed = Random.nextDouble() * 1000.0
    }

    private fun setupPaints() {
        paint.apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        glowPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = 30f
            maskFilter = BlurMaskFilter(60f, BlurMaskFilter.Blur.NORMAL)
            isAntiAlias = true
            alpha = 80
        }

        ripplePaint.apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = 2f
            alpha = 80
        }

        gradientPaint.apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    private fun startAnimations() {
        pulseAnimator = AnimationUtils.createSmoothAnimator(
            1f, 1.05f, 1500,
            repeatCount = ValueAnimator.INFINITE,
            repeatMode = ValueAnimator.REVERSE
        ) { value ->
            pulseScale = value
            invalidate()
        }.apply { start() }

        energyAnimator = AnimationUtils.createSmoothAnimator(
            0f, 1f, 2000,
            interpolator = LinearInterpolator(),
            repeatCount = ValueAnimator.INFINITE
        ) { value ->
            energyLevel = value
            invalidate()
        }.apply { start() }
    }

    fun updateAudio(level: Float, fft: FloatArray?) {
        audioLevel = level
        fft?.let {
            System.arraycopy(it, 0, fftBands, 0, min(it.size, fftBands.size))
        }
        
        if (level > 0.5f) {
            addRipple()
        }
        
        invalidate()
    }

    private fun addRipple() {
        ripples.add(
            Ripple(
                radius = width * 0.2f,
                alpha = 1f,
                maxRadius = width * 0.8f,
                centerX = width / 2f,
                centerY = height / 2f
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = width.coerceAtMost(height) / 2f * 0.65f
        val dynamicRadius = baseRadius * (1f + 0.3f * audioLevel) * pulseScale

        // Update gradient
        gradientShader = gradientManager.createOrbGradient(cx, cy, dynamicRadius)
        gradientPaint.shader = gradientShader

        // Draw visual elements
        drawRipples(canvas)
        drawGlowLayers(canvas, cx, cy, dynamicRadius)
        drawOrbShape(canvas, cx, cy, dynamicRadius)

        // Animate
        phase += 0.02f + 0.08f * audioLevel
        invalidate()
    }

    private fun drawRipples(canvas: Canvas) {
        ripples.removeAll { ripple ->
            ripple.radius += 5f
            ripple.alpha *= 0.95f
            
            ripplePaint.apply {
                color = ColorUtils.setAlphaComponent(
                    gradientManager.orbColors[2],
                    (ripple.alpha * 80).toInt()
                )
                strokeWidth = (1 - ripple.alpha) * 5f
            }
            
            canvas.drawCircle(
                ripple.centerX,
                ripple.centerY,
                ripple.radius,
                ripplePaint
            )
            
            ripple.radius >= ripple.maxRadius || ripple.alpha < 0.1f
        }
    }

    private fun drawGlowLayers(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        for (i in 3 downTo 1) {
            val glowRadius = radius + 15f * i
            glowPaint.apply {
                strokeWidth = 20f * i
                alpha = (80 / i).coerceIn(0, 255)
                color = gradientManager.glowColors[i % gradientManager.glowColors.size]
            }
            canvas.drawCircle(cx, cy, glowRadius, glowPaint)
        }
    }

    private fun drawOrbShape(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        edgePath.reset()
        val points = 180
        
        for (i in 0..points) {
            val angle = (2 * PI * i) / points
            val fftIndex = ((i * fftBands.size) / points).coerceIn(0, fftBands.size - 1)
            
            val baseWave = AnimationUtils.createComplexWave(phase + angle.toFloat(), audioLevel)
            val fftWave = fftBands[fftIndex] * 0.4f
            val energyWave = sin(energyLevel * 2 * PI + angle * 3) * 0.1f
            
            val totalDisplacement = baseWave + fftWave + energyWave
            val r = radius * (1f + totalDisplacement)
            
            val x = (cx + r * cos(angle)).toFloat()
            val y = (cy + r * sin(angle)).toFloat()
            
            if (i == 0) edgePath.moveTo(x, y) else edgePath.lineTo(x, y)
        }
        
        edgePath.close()
        canvas.drawPath(edgePath, gradientPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val currentTime = System.currentTimeMillis()
        val windowManagerParams = layoutParams as? WindowManager.LayoutParams
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                isDragging = true
                lastUpdateTime = currentTime
                velocityX = 0f
                velocityY = 0f
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = event.rawX - lastTouchX
                    val deltaY = event.rawY - lastTouchY
                    
                    val timeDelta = (currentTime - lastUpdateTime).coerceAtLeast(1L)
                    velocityX = deltaX / timeDelta
                    velocityY = deltaY / timeDelta
                    
                    windowManagerParams?.let { params ->
                        val newX = (params.x + deltaX).toInt()
                        val newY = (params.y + deltaY).toInt()
                        orbMovedListener?.invoke(newX, newY)
                    }
                    
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    lastUpdateTime = currentTime
                    
                    if (Random.nextFloat() < 0.1f) {
                        addRipple()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setOnOrbMovedListener(listener: (Int, Int) -> Unit) {
        orbMovedListener = listener
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        energyAnimator?.cancel()
    }
}
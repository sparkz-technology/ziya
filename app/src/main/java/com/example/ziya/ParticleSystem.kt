package com.example.ziya

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.*
import kotlin.random.Random

class ParticleSystem {
    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastEmissionTime = 0L
    private val emissionInterval = 50L // Milliseconds between particle emissions
    
    data class Particle(
        var x: Float,
        var y: Float,
        var velocityX: Float,
        var velocityY: Float,
        var alpha: Float,
        var scale: Float,
        var life: Float,
        var color: Int
    )

    fun update(centerX: Float, centerY: Float, audioLevel: Float, baseRadius: Float) {
        val currentTime = System.currentTimeMillis()
        
        // Emit new particles based on audio level
        if (currentTime - lastEmissionTime > emissionInterval) {
            val particleCount = (audioLevel * 3).toInt().coerceIn(0, 3)
            repeat(particleCount) {
                emitParticle(centerX, centerY, baseRadius)
            }
            lastEmissionTime = currentTime
        }

        // Update existing particles
        particles.removeAll { particle ->
            particle.life -= 0.016f // Assuming 60fps
            particle.alpha = particle.life
            particle.scale *= 0.98f
            particle.x += particle.velocityX
            particle.y += particle.velocityY
            particle.velocityX *= 0.98f
            particle.velocityY *= 0.98f
            particle.life <= 0
        }
    }

    private fun emitParticle(centerX: Float, centerY: Float, baseRadius: Float) {
        val angle = Random.nextFloat() * 2 * PI.toFloat()
        val speed = Random.nextFloat() * 2f + 1f
        val distance = baseRadius * (0.8f + Random.nextFloat() * 0.4f)
        
        particles.add(
            Particle(
                x = centerX + cos(angle) * distance,
                y = centerY + sin(angle) * distance,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed,
                alpha = 1f,
                scale = Random.nextFloat() * 0.5f + 0.5f,
                life = 1f,
                color = Color.WHITE
            )
        )
    }

    fun draw(canvas: Canvas, baseColor: Int) {
        particles.forEach { particle ->
            paint.color = Color.argb(
                (particle.alpha * 255).toInt(),
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            canvas.drawCircle(particle.x, particle.y, 4f * particle.scale, paint)
        }
    }
}
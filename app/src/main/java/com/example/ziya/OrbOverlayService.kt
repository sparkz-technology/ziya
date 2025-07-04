package com.example.ziya

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.*
import androidx.core.app.NotificationCompat
import kotlin.math.min

class OrbOverlayService : Service() {
    private var orbView: OrbView? = null
    private var windowManager: WindowManager? = null
    private var audioAnalyzer: AudioAnalyzer? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val screenWidth: Int
        val screenHeight: Int

        val size: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager?.currentWindowMetrics?.bounds!!
            screenWidth = bounds.width()
            screenHeight = bounds.height()
            size = min(screenWidth, screenHeight) / 4
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION") windowManager?.defaultDisplay?.getMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            size = min(screenWidth, screenHeight) / 4
        }

        orbView = OrbView(this)

        params =
                WindowManager.LayoutParams(
                                size,
                                size,
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                                PixelFormat.TRANSLUCENT
                        )
                        .apply {
                            gravity = Gravity.TOP or Gravity.START
                            x = (screenWidth - size) / 2
                            y = (screenHeight - size) / 2
                        }

        windowManager?.addView(orbView, params)

        audioAnalyzer = AudioAnalyzer { level, fftBands ->
            orbView?.post { orbView?.updateAudio(level, fftBands) }
        }
        audioAnalyzer?.start()

        orbView?.setOnOrbMovedListener { x, y ->
            params.x = x.coerceIn(-size / 2, screenWidth - size / 2)
            params.y = y.coerceIn(-size / 2, screenHeight - size / 2)
            windowManager?.updateViewLayout(orbView, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer?.stop()
        orbView?.let { view -> windowManager?.removeView(view) }
        orbView = null
        windowManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(
                                    "orb_overlay",
                                    "Orb Overlay",
                                    NotificationManager.IMPORTANCE_LOW
                            )
                            .apply {
                                setShowBadge(false)
                                enableLights(false)
                                enableVibration(false)
                            }

            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "orb_overlay")
                .setContentTitle("Orb Assistant Running")
                .setSmallIcon(R.drawable.ic_orb)
                .setOngoing(true)
                .build()
    }
}

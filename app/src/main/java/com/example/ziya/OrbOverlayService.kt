package com.example.ziya

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.app.NotificationCompat

class OrbOverlayService : Service() {
    private var orbView: OrbView? = null
    private var windowManager: WindowManager? = null
    private var audioAnalyzer: AudioAnalyzer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        orbView = OrbView(this)

        val params = WindowManager.LayoutParams(
            400, 400,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER

        windowManager?.addView(orbView, params)

        audioAnalyzer = AudioAnalyzer { level, fftBands ->
            orbView?.post { orbView?.updateAudio(level, fftBands) }
        }
        audioAnalyzer?.start()

        orbView?.setOnOrbMovedListener { x, y ->
            params.x = x
            params.y = y
            windowManager?.updateViewLayout(orbView, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioAnalyzer?.stop()
        if (orbView != null) windowManager?.removeView(orbView)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel("orb_overlay", "Orb Overlay", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
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
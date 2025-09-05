package com.example.ziya

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.util.*

// Import OrbGLSurfaceView if it exists in your project
import com.example.ziya.OrbGLSurfaceView

class OverlayService : Service(), RecognitionListener {

    private lateinit var windowManager: WindowManager
    private lateinit var orbView: OrbGLSurfaceView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    private enum class AssistantState { LISTENING, THINKING, SPEAKING, IDLE }
    private var currentState = AssistantState.IDLE

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "OverlayServiceChannel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        orbView = OrbGLSurfaceView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(orbView, params)

        setupSpeechRecognizer()
        setupTTS()

        // Give a moment for the view to be ready before starting to listen
        Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 500)
    }

    private fun startForegroundService() {
        val channel = NotificationChannel(CHANNEL_ID, "Overlay Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ziya Assistant")
            .setContentText("Running in the background.")
            .setSmallIcon(R.mipmap.ic_launcher) // Use your app's icon
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }
    }

    private fun setupSpeechRecognizer() {
        // Run on main thread as required
        Handler(Looper.getMainLooper()).post {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(this)
        }
    }

    private fun startListening() {
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.startListening(intent)
            currentState = AssistantState.LISTENING
        }
    }

    private fun askGemini(prompt: String) {
        currentState = AssistantState.THINKING
        // TODO: Implement your Gemini API call here using OkHttp/Retrofit
        // For now, we will just echo the response
        val response = "You said: $prompt"
        speakResponse(response)
    }

    private fun speakResponse(text: String) {
        currentState = AssistantState.SPEAKING
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        Handler(Looper.getMainLooper()).postDelayed({
            currentState = AssistantState.IDLE
            startListening()
        }, (text.length * 80).toLong())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::orbView.isInitialized) windowManager.removeView(orbView)
        if (::speechRecognizer.isInitialized) speechRecognizer.destroy()
        if (::tts.isInitialized) tts.shutdown()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {
        val intensity = (rmsdB - 2) / 8
        orbView.updateAudioLevels(intensity.coerceIn(0f, 1f))
        orbView.setOrbState(currentState.ordinal)
    }
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {
        if (currentState != AssistantState.IDLE) { // Avoid loop if already idle
           startListening()
        }
    }
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            askGemini(matches[0])
        } else {
            startListening()
        }
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}


import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.WindowManager
import java.util.*

class OverlayService : Service(), RecognitionListener {

    private lateinit var windowManager: WindowManager
    private lateinit var orbView: OrbGLSurfaceView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    private var currentRmsDb = 0.0f

    // Placeholder for states
    private enum class AssistantState { LISTENING, THINKING, SPEAKING, IDLE }
    private var currentState = AssistantState.IDLE

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        orbView = OrbGLSurfaceView(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(orbView, params)

        setupSpeechRecognizer()
        setupTTS()

        // Start listening immediately for demo purposes
        startListening()
    }

    private fun setupTTS() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
        currentState = AssistantState.LISTENING
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
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        // A simple handler to switch back to idle after speaking
        Handler(Looper.getMainLooper()).postDelayed({
            currentState = AssistantState.IDLE
            startListening() // Listen for the next command
        }, (text.length * 80).toLong()) // Approximate speech duration
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(orbView)
        speechRecognizer.destroy()
        tts.shutdown()
    }

    // --- RecognitionListener Methods ---
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {
        // Update the renderer with the current voice intensity
        // We normalize the dB value to a 0-1 range
        currentRmsDb = rmsdB
        val intensity = (rmsdB - 2) / 8 // Simple normalization
        orbView.updateAudioLevels(intensity.coerceIn(0f, 1f))
        orbView.setOrbState(currentState.ordinal)
    }
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) { startListening() } // Restart on error
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            askGemini(matches[0])
        } else {
            startListening()
        }
    }
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
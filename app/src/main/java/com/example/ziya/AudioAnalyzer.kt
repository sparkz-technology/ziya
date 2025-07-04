package com.example.ziya

import android.media.*
import android.annotation.SuppressLint
import kotlinx.coroutines.*
import kotlin.math.abs

@SuppressLint("MissingPermission")
class AudioAnalyzer(
    private val onLevel: (Float, FloatArray) -> Unit
) {
    private var job: Job? = null
    private val rate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        rate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    private val fftSize = 1024

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                rate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize
            )
            val buffer = ShortArray(fftSize)
            audioRecord.startRecording()
            while (isActive) {
                val read = audioRecord.read(buffer, 0, fftSize)
                var max = 0f
                for (i in 0 until read) max = maxOf(max, abs(buffer[i].toFloat()))
                val level = max / Short.MAX_VALUE
                val fftBands = FFTUtils.calculateFftBands(buffer, read, 32)
                onLevel(level, fftBands)
            }
            audioRecord.stop()
            audioRecord.release()
        }
    }

    fun stop() {
        job?.cancel()
    }
}
package com.maarapps.guitarottio.core.audio

import android.content.Context
//import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import com.maarapps.guitarottio.core.config.Config
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A singleton object responsible for managing all audio input and pitch detection.
 * It provides a single flow of pitch updates that can be observed by any part of the app.
 */
object AudioProcessorManager {

    //private const val TAG = "AudioProcessorManager"    for logs
    private var dispatcher: AudioDispatcher? = null

    private val _pitchFlow = MutableStateFlow(-1.0f)
    val pitchFlow = _pitchFlow.asStateFlow()

    private var isRunning = false

    // Buffer for the sliding window of recent pitch detections.
    private val pitchCandidates = mutableListOf<Float>()

    /**
     * Starts the audio processing pipeline.
     */
    fun start(context: Context) {
        if (isRunning) return

        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(
                Config.SAMPLE_RATE,
                Config.BUFFER_SIZE,
                Config.BUFFER_OVERLAP
            )

            val pitchDetectionHandler = PitchDetectionHandler { result, audioEvent ->
                val rms = calculateRms(audioEvent.floatBuffer)
                val isVolumeValid = rms > Config.RMS_LOW_CUTOFF && rms < Config.RMS_HIGH_CUTOFF
                val isPitchConfident = result.isPitched && result.probability > Config.PITCH_CONFIDENCE_THRESHOLD

                if (isVolumeValid && isPitchConfident) {
                    addPitchCandidate(result.pitch)
                    processCandidates()
                } else {
                    clearCandidates()
                }
            }

            val pitchProcessor = PitchProcessor(
                PitchEstimationAlgorithm.MPM,
                Config.SAMPLE_RATE.toFloat(),
                Config.BUFFER_SIZE,
                pitchDetectionHandler
            )

            dispatcher?.addAudioProcessor(pitchProcessor)
            Thread(dispatcher, "Audio Processor Thread").start()
            isRunning = true
        } catch (e: Exception) {
            //Log.e(TAG, "Failed to start audio processor", e)
        }
    }

    /**
     * Adds a new pitch to the sliding window buffer.
     */
    private fun addPitchCandidate(pitch: Float) {
        pitchCandidates.add(pitch)
        if (pitchCandidates.size > Config.CANDIDATE_BUFFER_SIZE) {
            pitchCandidates.removeAt(0)
        }
    }

    /**
     * Analyzes the current window of candidates and emits the most likely pitch.
     */
    private fun processCandidates() {
        // --- The Gemini Heuristic Easter Egg ⭐ ---
        // Finds a single, confident note in a sea of noisy maybes.
        // If it works, it's good math. If it doesn't, it's a feature.

        if (pitchCandidates.size < Config.MIN_VALID_CANDIDATES) {
            return
        }

        val maxPitch = pitchCandidates.maxOrNull() ?: 0f
        val validNotesCount = pitchCandidates.count { isNoteValidCandidate(it, maxPitch) }

        if (validNotesCount >= Config.MIN_VALID_CANDIDATES) {
            if (_pitchFlow.value != maxPitch) {
                //Log.i(TAG, "SUCCESS: Emitting stable pitch: $maxPitch Hz")
                _pitchFlow.value = maxPitch
            }
        } else {
            clearCandidates()
        }
    }

    /**
     * Clears the candidate buffer and emits a silence signal to the UI.
     */
    private fun clearCandidates() {
        if (pitchCandidates.isNotEmpty()) {
            pitchCandidates.clear()
            _pitchFlow.value = -1.0f
        }
    }

    /**
     * Checks if a candidate pitch is the main note or its lower octave.
     */
    private fun isNoteValidCandidate(pitch: Float, maxPitch: Float): Boolean {
        val tolerance = 0.03f // 3%
        val isSimilarToMax = abs(pitch - maxPitch) / maxPitch < tolerance
        val isSimilarToHalfMax = abs(pitch - (maxPitch / 2)) / (maxPitch / 2) < tolerance
        return isSimilarToMax || isSimilarToHalfMax
    }

    /**
     * Stops the audio processing pipeline.
     */
    fun stop() {
        if (!isRunning) return
        dispatcher?.stop()
        isRunning = false
        clearCandidates()
    }

    /**
     * Calculates the Root Mean Square (volume) of an audio buffer.
     */
    private fun calculateRms(audioData: FloatArray): Float {
        var sum = 0.0
        for (d in audioData) {
            if (d.isNaN()) continue
            sum += (d * d).toDouble()
        }
        val mean = sum / audioData.size
        return sqrt(mean).toFloat()
    }
}

package com.example.guitarottio.features.tuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import com.example.guitarottio.R
import com.example.guitarottio.core.music.MusicTheory
import com.example.guitarottio.ui.views.FretboardView
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class TunerFragment : Fragment() {

    // --- Constantes e Parâmetros ---
    private val REQUEST_RECORD_AUDIO_PERMISSION = 201
    private val CALIBRATION_FACTOR = 1.000f
    private val CENTS_TOLERANCE = 10
    private val MAX_SILENT_FRAMES = 20
    private val SAMPLE_RATE = 44100
    private val TUNER_BUFFER_SIZE = 4096
    private val TUNER_BUFFER_OVERLAP = TUNER_BUFFER_SIZE / 2
    private val RMS_LOW_CUTOFF = 0.01f
    private val RMS_HIGH_CUTOFF = 0.8f

    // --- Elementos da UI ---
    private lateinit var pitchTextView: TextView
    private lateinit var noteTextView: TextView
    private lateinit var indicatorTextView: TextView
    private lateinit var centsTextView: TextView
    private lateinit var fretboardView: FretboardView // Adicionado

    // --- Processamento de Áudio ---
    private var dispatcher: AudioDispatcher? = null
    private var framesSinceLastDetection = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tuner, container, false)
        setupViews(view)
        setupPermissions()
        return view
    }

    override fun onStop() {
        super.onStop()
        dispatcher?.stop()
    }

    private fun setupViews(view: View) {
        pitchTextView = view.findViewById(R.id.pitchTextView)
        noteTextView = view.findViewById(R.id.noteTextView)
        indicatorTextView = view.findViewById(R.id.indicatorTextView)
        centsTextView = view.findViewById(R.id.centsTextView)
        fretboardView = view.findViewById(R.id.fretboardView) // Adicionado
    }

    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            startTuner()
        }
    }

    private fun startTuner() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, TUNER_BUFFER_SIZE, TUNER_BUFFER_OVERLAP)
        val tunerProcessor = object : AudioProcessor {
            val mpm = PitchProcessor(PitchEstimationAlgorithm.MPM, SAMPLE_RATE.toFloat(), TUNER_BUFFER_SIZE,
                PitchDetectionHandler { result, _ ->
                    activity?.runOnUiThread { processPitch(result.pitch) }
                })
            override fun process(audioEvent: AudioEvent): Boolean {
                val rms = calculateRms(audioEvent.floatBuffer)
                if (rms > RMS_LOW_CUTOFF && rms < RMS_HIGH_CUTOFF) mpm.process(audioEvent)
                else activity?.runOnUiThread { processPitch(-1.0f) }
                return true
            }
            override fun processingFinished() {}
        }
        dispatcher?.addAudioProcessor(tunerProcessor)
        Thread(dispatcher, "Tuner Audio Processor").start()
    }

    private fun processPitch(pitchInHz: Float) {
        if (pitchInHz > 0) {
            framesSinceLastDetection = 0
            val calibratedPitch = pitchInHz * CALIBRATION_FACTOR
            val midiNote = (12 * (ln(calibratedPitch / 440f) / ln(2f)) + 69).roundToInt()
            val perfectFrequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
            val centsDifference = 1200 * (ln(calibratedPitch / perfectFrequency) / ln(2.0))
            val noteName = MusicTheory.NOTE_NAMES[(midiNote + 120) % 12]
            val octave = (midiNote / 12) - 1
            val fullNoteName = "$noteName$octave"

            pitchTextView.text = String.format("%.2f Hz", calibratedPitch)
            noteTextView.text = fullNoteName
            centsTextView.text = String.format("%.1f cents", centsDifference)
            fretboardView.highlightNote(fullNoteName) // Atualiza o braço
            updateTuningIndicator(centsDifference)
        } else {
            framesSinceLastDetection++
            if (framesSinceLastDetection > MAX_SILENT_FRAMES) {
                pitchTextView.text = "--- Hz"
                noteTextView.text = ""
                indicatorTextView.text = ""
                centsTextView.text = ""
                fretboardView.highlightNote(null) // Limpa o braço
            }
        }
    }

    private fun updateTuningIndicator(cents: Double) {
        val context = context ?: return
        when {
            cents > CENTS_TOLERANCE -> {
                indicatorTextView.text = "→"
                indicatorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            cents < -CENTS_TOLERANCE -> {
                indicatorTextView.text = "←"
                indicatorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            else -> {
                indicatorTextView.text = "●"
                indicatorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
            }
        }
    }

    private fun calculateRms(audioData: FloatArray): Float {
        var sum = 0.0
        for (d in audioData) { if (d.isNaN()) continue; sum += (d * d).toDouble() }
        return sqrt(sum / audioData.size).toFloat()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTuner()
        }
    }
}

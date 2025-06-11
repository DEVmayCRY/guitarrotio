package com.maarapps.guitarottio.features.tuner

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
import com.maarapps.guitarottio.R
import com.maarapps.guitarottio.core.config.Config
import com.maarapps.guitarottio.core.music.MusicTheory
import com.maarapps.guitarottio.ui.views.FretboardView
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class TunerFragment : Fragment() {

    // --- Constants and Parameters ---
    private val REQUEST_RECORD_AUDIO_PERMISSION = 201

    // --- UI Elements ---
    private lateinit var pitchTextView: TextView
    private lateinit var noteTextView: TextView
    private lateinit var indicatorTextView: TextView
    private lateinit var centsTextView: TextView
    private lateinit var fretboardView: FretboardView

    // --- Audio Processing ---
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
        fretboardView = view.findViewById(R.id.fretboardView)
    }

    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            startTuner()
        }
    }

    private fun startTuner() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(Config.SAMPLE_RATE, Config.BUFFER_SIZE, Config.BUFFER_OVERLAP)
        val tunerProcessor = object : AudioProcessor {
            val mpm = PitchProcessor(PitchEstimationAlgorithm.MPM, Config.SAMPLE_RATE.toFloat(), Config.BUFFER_SIZE,
                PitchDetectionHandler { result, _ ->
                    activity?.runOnUiThread { processPitch(result.pitch) }
                })
            override fun process(audioEvent: AudioEvent): Boolean {
                val rms = calculateRms(audioEvent.floatBuffer)
                if (rms > Config.RMS_LOW_CUTOFF && rms < Config.RMS_HIGH_CUTOFF) mpm.process(audioEvent)
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
            val calibratedPitch = pitchInHz * Config.CALIBRATION_FACTOR
            val midiNote = (12 * (ln(calibratedPitch / 440f) / ln(2f)) + 69).roundToInt()
            val perfectFrequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
            val centsDifference = 1200 * (ln(calibratedPitch / perfectFrequency) / ln(2.0))
            val noteName = MusicTheory.NOTE_NAMES[(midiNote + 120) % 12]
            val octave = (midiNote / 12) - 1
            val fullNoteName = "$noteName$octave"

            pitchTextView.text = String.format("%.2f Hz", calibratedPitch)
            noteTextView.text = fullNoteName
            centsTextView.text = String.format("%.1f cents", centsDifference)
            fretboardView.highlightNote(fullNoteName)
            updateTuningIndicator(centsDifference)
        } else {
            framesSinceLastDetection++
            if (framesSinceLastDetection > Config.MAX_SILENT_FRAMES) {
                pitchTextView.text = "--- Hz"
                noteTextView.text = ""
                indicatorTextView.text = ""
                centsTextView.text = ""
                fretboardView.highlightNote(null)
            }
        }
    }

    private fun updateTuningIndicator(cents: Double) {
        val context = context ?: return
        when {
            cents > Config.CENTS_TOLERANCE -> {
                indicatorTextView.text = "→"
                indicatorTextView.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
            }
            cents < -Config.CENTS_TOLERANCE -> {
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

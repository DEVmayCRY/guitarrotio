package com.maarapps.guitarottio.features.tuner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.maarapps.guitarottio.R
import com.maarapps.guitarottio.core.audio.AudioProcessorManager
import com.maarapps.guitarottio.core.config.Config
import com.maarapps.guitarottio.core.music.MusicTheory
import com.maarapps.guitarottio.ui.views.FretboardView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

class TunerFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var pitchTextView: TextView
    private lateinit var noteTextView: TextView
    private lateinit var indicatorTextView: TextView
    private lateinit var centsTextView: TextView
    private lateinit var fretboardView: FretboardView

    // --- State ---
    private var clearUiJob: Job? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tuner, container, false)
        setupViews(view)
        observePitchUpdates()
        return view
    }

    private fun setupViews(view: View) {
        pitchTextView = view.findViewById(R.id.pitchTextView)
        noteTextView = view.findViewById(R.id.noteTextView)
        indicatorTextView = view.findViewById(R.id.indicatorTextView)
        centsTextView = view.findViewById(R.id.centsTextView)
        fretboardView = view.findViewById(R.id.fretboardView)
    }

    /**
     * Subscribes to the shared audio processor to receive pitch updates.
     */
    private fun observePitchUpdates() {
        lifecycleScope.launch {
            AudioProcessorManager.pitchFlow.collectLatest { pitchInHz ->
                processPitch(pitchInHz)
            }
        }
    }

    /**
     * Processes the detected pitch and updates the tuner UI accordingly.
     */
    private fun processPitch(pitchInHz: Float) {
        clearUiJob?.cancel()

        if (pitchInHz > 0f) {
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
            clearUiJob = lifecycleScope.launch {
                delay(Config.MAX_SILENT_FRAMES)
                clearUi()
            }
        }
    }

    private fun clearUi() {
        //
        pitchTextView.text = "--- Hz"
        noteTextView.text = ""
        indicatorTextView.text = ""
        centsTextView.text = ""
        fretboardView.highlightNote(null)
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
}

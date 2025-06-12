package com.maarapps.guitarottio.features.scales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.maarapps.guitarottio.R
import com.maarapps.guitarottio.core.audio.AudioProcessorManager
import com.maarapps.guitarottio.core.config.Config
import com.maarapps.guitarottio.core.music.MusicTheory
import com.maarapps.guitarottio.ui.views.FretboardView
import com.maarapps.guitarottio.ui.views.PianoView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.math.roundToInt

class ScalesFragment : Fragment() {

    // --- UI Elements ---
    private lateinit var fretboardView: FretboardView
    private lateinit var pianoView: PianoView
    private lateinit var rootNoteSpinner: Spinner
    private lateinit var scaleTypeSpinner: Spinner
    private lateinit var portugueseNoteTextView: TextView
    private lateinit var scientificNoteTextView: TextView
    private lateinit var frequencyTextView: TextView

    // --- State ---
    private var clearUiJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scales, container, false)
        setupViews(view)
        observePitchUpdates()
        return view
    }

    private fun setupViews(view: View) {
        fretboardView = view.findViewById(R.id.fretboardView)
        pianoView = view.findViewById(R.id.pianoView)
        rootNoteSpinner = view.findViewById(R.id.rootNoteSpinner)
        scaleTypeSpinner = view.findViewById(R.id.scaleTypeSpinner)
        portugueseNoteTextView = view.findViewById(R.id.portugueseNoteTextView)
        scientificNoteTextView = view.findViewById(R.id.scientificNoteTextView)
        frequencyTextView = view.findViewById(R.id.frequencyTextView)
        setupSpinners()
    }

    private fun setupSpinners() {
        val context = requireContext()
        val rootAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, MusicTheory.NOTE_NAMES)
        rootAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rootNoteSpinner.adapter = rootAdapter

        val scaleAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, MusicTheory.SCALES.keys.toList())
        scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        scaleTypeSpinner.adapter = scaleAdapter

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = updateScaleHighlights()
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        rootNoteSpinner.onItemSelectedListener = itemSelectedListener
        scaleTypeSpinner.onItemSelectedListener = itemSelectedListener
    }

    private fun updateScaleHighlights() {
        val selectedRoot = rootNoteSpinner.selectedItem.toString()
        val selectedScale = scaleTypeSpinner.selectedItem.toString()
        val scaleNotes = MusicTheory.getScaleNotes(selectedRoot, selectedScale)
        fretboardView.highlightScale(scaleNotes)
        pianoView.highlightScale(scaleNotes)
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
     * Processes the detected pitch and updates the UI elements in this fragment.
     */
    private fun processPitch(pitchInHz: Float) {
        val calibratedPitch = pitchInHz * Config.CALIBRATION_FACTOR
        clearUiJob?.cancel()

        if (calibratedPitch > 0) {
            val midiNote = (12 * (ln(calibratedPitch / 440.0) / ln(2.0)) + 69).roundToInt()
            val noteIndex = (midiNote + 120) % 12
            val noteName = MusicTheory.NOTE_NAMES[noteIndex]
            val octave = (midiNote / 12) - 1
            val fullNoteName = "$noteName$octave"

            fretboardView.highlightNote(fullNoteName)
            pianoView.highlightNote(fullNoteName)

            portugueseNoteTextView.text = MusicTheory.NOTE_NAMES_PT[noteIndex]
            scientificNoteTextView.text = fullNoteName
            frequencyTextView.text = String.format("%.2f Hz", calibratedPitch)

        } else {
            clearUiJob = lifecycleScope.launch {
                delay(Config.MAX_SILENT_FRAMES)
                clearUi()
            }
        }
    }

    private fun clearUi() {
        fretboardView.highlightNote(null)
        pianoView.highlightNote(null)
        portugueseNoteTextView.text = "---"
        scientificNoteTextView.text = ""
        frequencyTextView.text = ""
    }
}

package com.maarapps.guitarottio.features.scales

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm
import com.maarapps.guitarottio.R
import com.maarapps.guitarottio.core.config.Config
import com.maarapps.guitarottio.core.music.MusicTheory
import com.maarapps.guitarottio.ui.views.FretboardView
import com.maarapps.guitarottio.ui.views.PianoView
import kotlin.math.ln
import kotlin.math.roundToInt

class ScalesFragment : Fragment() {

    // --- Constants and Parameters ---
    private val REQUEST_RECORD_AUDIO_PERMISSION = 202

    // --- UI Elements ---
    private lateinit var fretboardView: FretboardView
    private lateinit var pianoView: PianoView
    private lateinit var rootNoteSpinner: Spinner
    private lateinit var scaleTypeSpinner: Spinner
    private lateinit var portugueseNoteTextView: TextView
    private lateinit var scientificNoteTextView: TextView
    private lateinit var frequencyTextView: TextView

    // --- Audio Processing ---
    private var dispatcher: AudioDispatcher? = null
    private var framesSinceLastDetection = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scales, container, false)
        setupViews(view)
        setupPermissions()
        return view
    }

    override fun onStop() {
        super.onStop()
        dispatcher?.stop()
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

    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            startAudioProcessing()
        }
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

    private fun startAudioProcessing() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(Config.SAMPLE_RATE, Config.BUFFER_SIZE, Config.BUFFER_OVERLAP)
        val pitchDetectionHandler = PitchDetectionHandler { result, _ ->
            activity?.runOnUiThread { processPitch(result.pitch) }
        }
        val audioProcessor = PitchProcessor(PitchEstimationAlgorithm.MPM, Config.SAMPLE_RATE.toFloat(), Config.BUFFER_SIZE, pitchDetectionHandler)
        dispatcher?.addAudioProcessor(audioProcessor)
        Thread(dispatcher, "Scales Audio Processor").start()
    }

    private fun processPitch(pitchInHz: Float) {
        if (pitchInHz > 0) {
            framesSinceLastDetection = 0
            val midiNote = (12 * (ln(pitchInHz / 440.0) / ln(2.0)) + 69).roundToInt()
            val noteIndex = (midiNote + 120) % 12
            val noteName = MusicTheory.NOTE_NAMES[noteIndex]
            val octave = (midiNote / 12) - 1
            val fullNoteName = "$noteName$octave"

            fretboardView.highlightNote(fullNoteName)
            pianoView.highlightNote(fullNoteName)

            portugueseNoteTextView.text = MusicTheory.NOTE_NAMES_PT[noteIndex]
            scientificNoteTextView.text = fullNoteName
            frequencyTextView.text = String.format("%.2f Hz", pitchInHz)

        } else {
            framesSinceLastDetection++
            if (framesSinceLastDetection > Config.MAX_SILENT_FRAMES) {
                fretboardView.highlightNote(null)
                pianoView.highlightNote(null)

                portugueseNoteTextView.text = "---"
                scientificNoteTextView.text = ""
                frequencyTextView.text = ""
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioProcessing()
        }
    }
}

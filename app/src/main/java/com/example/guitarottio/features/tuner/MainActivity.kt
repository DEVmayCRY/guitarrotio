package com.example.guitarottio.features.tuner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private enum class Mode { TUNER, SCALES }
    private var currentMode = Mode.TUNER

    // --- Audio & Tuning Parameters ---
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private val CALIBRATION_FACTOR = 1.000f
    private val CENTS_TOLERANCE = 10
    private val MAX_SILENT_FRAMES = 15
    private val SAMPLE_RATE = 44100
    private val TUNER_BUFFER_SIZE = 4096
    private val TUNER_BUFFER_OVERLAP = TUNER_BUFFER_SIZE / 2
    private val RMS_LOW_CUTOFF = 0.01f
    private val RMS_HIGH_CUTOFF = 0.8f

    // --- UI Elements ---
    private lateinit var pitchTextView: TextView
    private lateinit var noteTextView: TextView
    private lateinit var indicatorTextView: TextView
    private lateinit var centsTextView: TextView
    private lateinit var modeSwitch: SwitchMaterial
    private lateinit var fretboardView: FretboardView
    private lateinit var scaleSelectorLayout: LinearLayout
    private lateinit var rootNoteSpinner: Spinner
    private lateinit var scaleTypeSpinner: Spinner

    // --- Audio Processing ---
    private var dispatcher: AudioDispatcher? = null
    private var framesSinceLastDetection = 0
    private var isAudioProcessorRunning = false

    /**
     * Initializes the activity, sets up views, and requests permissions.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupViews()
        setupPermissions()
    }

    /**
     * Called when the activity is no longer visible to the user.
     * Stops the audio dispatcher to release the microphone.
     */
    override fun onStop() {
        super.onStop()
        dispatcher?.stop()
        isAudioProcessorRunning = false
    }

    /**
     * Initializes all UI components from the layout.
     */
    private fun setupViews() {
        pitchTextView = findViewById(R.id.pitchTextView)
        noteTextView = findViewById(R.id.noteTextView)
        indicatorTextView = findViewById(R.id.indicatorTextView)
        centsTextView = findViewById(R.id.centsTextView)
        modeSwitch = findViewById(R.id.modeSwitch)
        fretboardView = findViewById(R.id.fretboardView)
        scaleSelectorLayout = findViewById(R.id.scaleSelectorLayout)
        rootNoteSpinner = findViewById(R.id.rootNoteSpinner)
        scaleTypeSpinner = findViewById(R.id.scaleTypeSpinner)

        setupSpinners()
        setupModeSwitch()
        updateUiForMode()
    }

    /**
     * Checks for microphone permission and requests it if not granted.
     * Starts audio processing if permission is already granted.
     */
    private fun setupPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            startAudioProcessing()
        }
    }

    /**
     * Sets up the listener for the mode switch (Tuner/Scales).
     */
    private fun setupModeSwitch() {
        modeSwitch.setOnCheckedChangeListener { _, isChecked ->
            currentMode = if (isChecked) Mode.SCALES else Mode.TUNER
            updateUiForMode()
        }
    }

    /**
     * Populates the spinners for root note and scale type selection.
     */
    private fun setupSpinners() {
        val rootAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, MusicTheory.NOTE_NAMES)
        rootAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        rootNoteSpinner.adapter = rootAdapter

        val scaleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, MusicTheory.SCALES.keys.toList())
        scaleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        scaleTypeSpinner.adapter = scaleAdapter

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateScaleHighlights()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        rootNoteSpinner.onItemSelectedListener = itemSelectedListener
        scaleTypeSpinner.onItemSelectedListener = itemSelectedListener
    }

    /**
     * Updates the UI visibility based on the selected mode.
     */
    private fun updateUiForMode() {
        if (currentMode == Mode.TUNER) {
            scaleSelectorLayout.visibility = View.GONE
            findViewById<LinearLayout>(R.id.tunerInfoLayout).visibility = View.VISIBLE
            fretboardView.highlightScale(emptyList())
        } else { // SCALES mode
            scaleSelectorLayout.visibility = View.VISIBLE
            findViewById<LinearLayout>(R.id.tunerInfoLayout).visibility = View.GONE
            updateScaleHighlights()
        }
        fretboardView.highlightNote(null)
    }

    /**
     * Gets the selected scale from the spinners and updates the fretboard view.
     */
    private fun updateScaleHighlights() {
        val selectedRoot = rootNoteSpinner.selectedItem.toString()
        val selectedScale = scaleTypeSpinner.selectedItem.toString()
        val scaleNotes = MusicTheory.getScaleNotes(selectedRoot, selectedScale)
        fretboardView.highlightScale(scaleNotes)
    }

    /**
     * Starts the main audio processing thread. It runs continuously and
     * processes audio based on the current mode.
     */
    private fun startAudioProcessing() {
        if (isAudioProcessorRunning) return

        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, TUNER_BUFFER_SIZE, TUNER_BUFFER_OVERLAP)

        val pitchDetectionHandler = PitchDetectionHandler { result, _ ->
            runOnUiThread { processPitch(result.pitch) }
        }

        val tunerProcessor = object : AudioProcessor {
            val mpm = PitchProcessor(PitchEstimationAlgorithm.MPM, SAMPLE_RATE.toFloat(), TUNER_BUFFER_SIZE, pitchDetectionHandler)

            override fun process(audioEvent: AudioEvent): Boolean {
                val rms = calculateRms(audioEvent.floatBuffer)
                if (rms > RMS_LOW_CUTOFF && rms < RMS_HIGH_CUTOFF) {
                    mpm.process(audioEvent)
                } else {
                    runOnUiThread { processPitch(-1.0f) }
                }
                return true
            }
            override fun processingFinished() {}
        }

        dispatcher?.addAudioProcessor(tunerProcessor)
        Thread(dispatcher, "Audio Processor").start()
        isAudioProcessorRunning = true
    }

    /**
     * Processes the detected pitch, updates the fretboard, and updates tuner UI if in TUNER mode.
     */
    private fun processPitch(pitchInHz: Float) {
        if (pitchInHz > 0) {
            framesSinceLastDetection = 0
            val calibratedPitch = pitchInHz * CALIBRATION_FACTOR
            val midiNote = (12 * (ln(calibratedPitch / 440f) / ln(2f)) + 69).roundToInt()
            val noteName = MusicTheory.NOTE_NAMES[(midiNote + 120) % 12]
            val octave = (midiNote / 12) - 1
            val fullNoteName = "$noteName$octave"

            fretboardView.highlightNote(fullNoteName)

            if (currentMode == Mode.TUNER) {
                val perfectFrequency = 440.0 * 2.0.pow((midiNote - 69) / 12.0)
                val centsDifference = 1200 * (ln(calibratedPitch / perfectFrequency) / ln(2.0))

                pitchTextView.text = String.format("%.2f Hz", calibratedPitch)
                noteTextView.text = fullNoteName
                centsTextView.text = String.format("%.1f cents", centsDifference)
                updateTuningIndicator(centsDifference)
            }
        } else {
            framesSinceLastDetection++
            if (framesSinceLastDetection > MAX_SILENT_FRAMES) {
                fretboardView.highlightNote(null)
                if (currentMode == Mode.TUNER) {
                    pitchTextView.text = "--- Hz"
                    noteTextView.text = ""
                    indicatorTextView.text = ""
                    centsTextView.text = ""
                }
            }
        }
    }

    /**
     * Updates the tuning indicator (arrows/dot) based on the cents difference.
     */
    private fun updateTuningIndicator(cents: Double) {
        when {
            cents > CENTS_TOLERANCE -> {
                indicatorTextView.text = "→"
                indicatorTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
            cents < -CENTS_TOLERANCE -> {
                indicatorTextView.text = "←"
                indicatorTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
            else -> {
                indicatorTextView.text = "●"
                indicatorTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            }
        }
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

    /**
     * Handles the result of the permission request.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startAudioProcessing()
        }
    }
}

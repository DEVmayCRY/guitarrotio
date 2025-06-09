package com.example.guitarottio.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.guitarottio.core.music.MusicTheory

/**
 * A custom View that draws a 6-octave piano keyboard, split into two rows.
 * It highlights detected notes and notes belonging to a selected musical scale.
 */
class PianoView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // --- Paint Objects ---
    private val whiteKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val blackKeyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val scaleNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#33b5e5") }
    private val detectedNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ff4444") }
    private val keyStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    // --- Keyboard Layout Constants ---
    private val keysWithBlackKey = setOf(0, 1, 3, 4, 5) // C, C#, D#, E, F, F# have black key to the right
    private val totalOctaves = 4
    private val whiteKeysPerOctave = 7
    private val notesInOctave = 12
    private val startingOctave = 3 // Starting from C3

    // --- State Variables ---
    private var scaleNoteClasses: Set<Int> = emptySet()
    private var detectedNoteMidi: Int? = null

    /**
     * Sets the notes of the selected scale to be highlighted.
     * @param notes A list of note names (e.g., ["C", "D", "E"]).
     */
    fun highlightScale(notes: List<String>) {
        scaleNoteClasses = notes.map { MusicTheory.noteNameToNumber(it) }.toSet()
        invalidate()
    }

    /**
     * Sets the currently detected note to be highlighted.
     * @param note The full note name (e.g., "E4") or null to clear.
     */
    fun highlightNote(note: String?) {
        detectedNoteMidi = note?.let {
            val noteName = it.replace(Regex("[0-9]"), "")
            val octave = it.last().toString().toInt()
            // The MIDI number is calculated as (octave + 1) * 12 + note index.
            // This standardizes the note representation across the system.
            MusicTheory.noteNameToNumber(noteName) + (octave + 1) * 12
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Each row now draws 2 octaves
        drawKeyboardRow(canvas, 0, 2, 0f)
        drawKeyboardRow(canvas, 2, 2, height / 2f)
    }

    /**
     * Draws a single row of piano keys for a specified number of octaves.
     */
    private fun drawKeyboardRow(canvas: Canvas, startOctaveIndex: Int, numOctaves: Int, yOffset: Float) {
        val rowHeight = height / 2f
        val totalWhiteKeysInRow = numOctaves * whiteKeysPerOctave
        val whiteKeyWidth = width.toFloat() / totalWhiteKeysInRow
        val blackKeyWidth = whiteKeyWidth * 0.6f
        val blackKeyHeight = rowHeight * 0.6f

        // 1. Draw white keys and their highlights
        for (i in 0 until totalWhiteKeysInRow) {
            val left = i * whiteKeyWidth
            val right = left + whiteKeyWidth
            val rect = RectF(left, yOffset, right, yOffset + rowHeight)

            // Always draw the base white key and its stroke
            canvas.drawRect(rect, whiteKeyPaint)
            canvas.drawRect(rect, keyStrokePaint)

            val octave = startOctaveIndex + (i / whiteKeysPerOctave)
            val keyInOctave = i % whiteKeysPerOctave
            val noteOffset = when(keyInOctave) { 0 -> 0; 1 -> 2; 2 -> 4; 3 -> 5; 4 -> 7; 5 -> 9; else -> 11 }
            val midiNote = (startingOctave + octave) * notesInOctave + noteOffset
            val noteClass = midiNote % 12

            // Check if the note should be highlighted
            val isScaleNote = scaleNoteClasses.contains(noteClass)
            val isDetectedNote = detectedNoteMidi == midiNote

            val centerX = left + whiteKeyWidth / 2
            val centerY = yOffset + rowHeight * 0.8f // Place dot towards the bottom

            if (isScaleNote) {
                canvas.drawCircle(centerX, centerY, whiteKeyWidth / 4.5f, scaleNotePaint)
            }
            if (isDetectedNote) {
                canvas.drawCircle(centerX, centerY, whiteKeyWidth / 3.5f, detectedNotePaint)
            }
        }

        // 2. Draw black keys and their highlights
        for (i in 0 until totalWhiteKeysInRow -1) {
            val keyInOctave = i % whiteKeysPerOctave
            if (keysWithBlackKey.contains(keyInOctave)) {
                val left = (i + 1) * whiteKeyWidth - (blackKeyWidth / 2)
                val right = left + blackKeyWidth
                val rect = RectF(left, yOffset, right, yOffset + blackKeyHeight)

                // Always draw the base black key
                canvas.drawRect(rect, blackKeyPaint)

                val octave = startOctaveIndex + (i / whiteKeysPerOctave)
                val noteOffset = when(keyInOctave) { 0 -> 1; 1 -> 3; 3 -> 6; 4 -> 8; else -> 10 }
                val midiNote = (startingOctave + octave) * notesInOctave + noteOffset
                val noteClass = midiNote % 12

                // Check if the note should be highlighted
                val isScaleNote = scaleNoteClasses.contains(noteClass)
                val isDetectedNote = detectedNoteMidi == midiNote

                val centerX = left + blackKeyWidth / 2
                val centerY = yOffset + blackKeyHeight * 0.8f

                if (isScaleNote) {
                    canvas.drawCircle(centerX, centerY, blackKeyWidth / 3f, scaleNotePaint)
                }
                if (isDetectedNote) {
                    canvas.drawCircle(centerX, centerY, blackKeyWidth / 2.5f, detectedNotePaint)
                }
            }
        }
    }
}

package com.maarapps.guitarottio.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.maarapps.guitarottio.core.music.MusicTheory

/**
 * A custom View that draws a guitar fretboard and highlights detected notes and musical scales.
 */
class FretboardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // --- Paint objects for drawing ---
    private val fretPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 4f
    }
    private val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 6f
    }
    private val detectedNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ff4444") // Red for the currently played note
    }
    private val scaleNotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33b5e5") // Blue for notes in the selected scale
    }

    // --- Fretboard properties ---
    private val fretCount = 11
    private val stringCount = 6

    // --- State variables ---
    private var detectedNote: String? = null
    private var scaleNotes: List<String> = emptyList()

    /**
     * Sets the currently detected note to be highlighted and triggers a redraw.
     * @param note The full note name (e.g., "E2") or null to clear the highlight.
     */
    fun highlightNote(note: String?) {
        detectedNote = note
        invalidate() // Forces the view to be redrawn
    }

    /**
     * Sets the notes of the selected scale to be highlighted and triggers a redraw.
     * @param notes A list of note names (e.g., ["C", "D", "E"]) to highlight.
     */
    fun highlightScale(notes: List<String>) {
        scaleNotes = notes
        invalidate()
    }

    /**
     * The main drawing method. This is called by the Android system whenever the view needs to be redrawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawFretboardGrid(canvas)

        // Draw scale notes first, so the detected note can be drawn on top.
        if (scaleNotes.isNotEmpty()) {
            drawNotesOnFretboard(canvas, scaleNotes, scaleNotePaint, 18f, true)
        }

        detectedNote?.let {
            drawNotesOnFretboard(canvas, listOf(it), detectedNotePaint, 25f, false)
        }
    }

    /**
     * Draws the basic grid of strings and frets.
     */
    private fun drawFretboardGrid(canvas: Canvas) {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val fretWidth = viewWidth / (fretCount + 1)
        val stringSpacing = viewHeight / stringCount

        for (i in 0 until stringCount) {
            val y = (i * stringSpacing) + (stringSpacing / 2)
            canvas.drawLine(0f, y, viewWidth, y, stringPaint)
        }

        for (i in 1..fretCount) {
            val x = i * fretWidth
            canvas.drawLine(x, 0f, x, viewHeight, fretPaint)
        }
    }

    /**
     * Draws circles on the fretboard for a given list of notes.
     * @param canvas The canvas to draw on.
     * @param notes The list of notes to draw.
     * @param paint The Paint object to use for drawing.
     * @param radius The radius of the note circle.
     * @param matchNoteClassOnly If true, it will match notes regardless of octave (for scales).
     */
    private fun drawNotesOnFretboard(canvas: Canvas, notes: List<String>, paint: Paint, radius: Float, matchNoteClassOnly: Boolean) {
        val noteClassesToMatch = if (matchNoteClassOnly) notes.map { it.replace(Regex("[0-9]"), "") } else null

        for (stringIndex in 0 until stringCount) {
            val openStringNote = MusicTheory.GUITAR_STRINGS[stringIndex]
            val openStringName = openStringNote.replace(Regex("[0-9]"), "")
            val openStringOctave = openStringNote.last().toString().toInt()
            val openStringMidiRoot = MusicTheory.noteNameToNumber(openStringName)

            for (fretIndex in 0..fretCount) {
                val currentNoteMidi = openStringMidiRoot + fretIndex
                val currentNoteName = MusicTheory.NOTE_NAMES[currentNoteMidi % 12]
                val currentOctave = openStringOctave + (currentNoteMidi / 12)
                val currentNoteFullName = "$currentNoteName$currentOctave"

                val noteMatches = if (matchNoteClassOnly) {
                    noteClassesToMatch?.contains(currentNoteName) == true
                } else {
                    notes.contains(currentNoteFullName)
                }

                if (noteMatches) {
                    val fretWidth = width.toFloat() / (fretCount + 1)
                    val stringSpacing = height.toFloat() / stringCount
                    val y = (stringIndex * stringSpacing) + (stringSpacing / 2)
                    // Position the note circle in the middle of the fret space.
                    val x = (fretIndex * fretWidth) - (fretWidth / 2)

                    if (fretIndex == 0) {
                        // For open strings, draw a smaller circle to the left of the nut.
                        canvas.drawCircle(fretWidth / 4, y, radius * 0.8f, paint)
                    } else {
                        canvas.drawCircle(x, y, radius, paint)
                    }
                }
            }
        }
    }
}

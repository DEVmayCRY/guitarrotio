package com.maarapps.guitarottio.core.music

object MusicTheory {

    val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    val NOTE_NAMES_PT = listOf("Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")
    // Standard guitar tuning from the 1st (thinnest) to the 6th (thickest) string.
    val GUITAR_STRINGS = listOf("E4", "B3", "G3", "D3", "A2", "E2")

    // Scale formulas represented by intervals (in semitones) from the root note.
    val SCALES = mapOf(
        "Pentatonic Major" to listOf(0, 2, 4, 7, 9),
        "Pentatonic Minor" to listOf(0, 3, 5, 7, 10),
        "Blues" to listOf(0, 3, 5, 6, 7, 10),
        "Major" to listOf(0, 2, 4, 5, 7, 9, 11),
        "Natural Minor" to listOf(0, 2, 3, 5, 7, 8, 10),
        "Harmonic Minor" to listOf(0, 2, 3, 5, 7, 8, 11),
        "Melodic Minor" to listOf(0, 2, 3, 5, 7, 9, 11),
        "Dorian" to listOf(0, 2, 3, 5, 7, 9, 10),
        "Phrygian" to listOf(0, 1, 3, 5, 7, 8, 10),
        "Lydian" to listOf(0, 2, 4, 6, 7, 9, 11),
        "Mixolydian" to listOf(0, 2, 4, 5, 7, 9, 10),
        "Locrian" to listOf(0, 1, 3, 5, 6, 8, 10),
        "Whole Tone" to listOf(0, 2, 4, 6, 8, 10),
        "Diminished" to listOf(0, 2, 3, 5, 6, 8, 9, 11),
        "Augmented" to listOf(0, 3, 4, 7, 8, 11),
        "Hungarian Minor" to listOf(0, 2, 3, 6, 7, 8, 11),
        "Phrygian Dominant" to listOf(0, 1, 4, 5, 7, 8, 10),
        "Neapolitan Minor" to listOf(0, 1, 3, 5, 7, 8, 11),
        "Neapolitan Major" to listOf(0, 1, 3, 5, 7, 9, 11),
        "Enigmatic" to listOf(0, 1, 4, 6, 8, 10, 11),
        "Double Harmonic" to listOf(0, 1, 4, 5, 7, 8, 11),
        "Persian" to listOf(0, 1, 4, 5, 6, 8, 11),
        "Arabian" to listOf(0, 2, 4, 5, 6, 8, 10),
        "Japanese" to listOf(0, 1, 5, 7, 8) // "Insen" scale variation
    )

    /**
     * Converts a note name (e.g., "C", "F#") to its corresponding MIDI number class (0-11).
     * @param note The name of the note.
     * @return The integer representation of the note class.
     */
    fun noteNameToNumber(note: String): Int {
        return NOTE_NAMES.indexOf(note)
    }

    /**
     * Generates a list of note names for a given root note and scale type.
     * @param rootNote The root note of the scale (e.g., "A").
     * @param scaleName The name of the scale (e.g., "Pentatonic Minor").
     * @return A list of note names belonging to that scale.
     */
    fun getScaleNotes(rootNote: String, scaleName: String): List<String> {
        val rootNumber = noteNameToNumber(rootNote)
        val scaleIntervals = SCALES[scaleName] ?: return emptyList()

        return scaleIntervals.map { interval ->
            NOTE_NAMES[(rootNumber + interval) % 12]
        }
    }
}

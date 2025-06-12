package com.maarapps.guitarottio.core.config

/**
 * A singleton object to hold all application-wide configuration parameters.
 */
object Config {
    // General Audio Settings
    const val SAMPLE_RATE = 44100

    // Tuner-specific Parameters
    const val BUFFER_SIZE = 4096
    const val BUFFER_OVERLAP = BUFFER_SIZE / 2
    const val RMS_LOW_CUTOFF = 0.03f
    const val RMS_HIGH_CUTOFF = 0.8f
    const val PITCH_CONFIDENCE_THRESHOLD = 0.90f

    // Calibration and Display Settings
    const val CALIBRATION_FACTOR = 1.000f
    const val CENTS_TOLERANCE = 10
    const val MAX_SILENT_FRAMES = 2000L

    // Audio Eureka Parameters
    const val CANDIDATE_BUFFER_SIZE = 5
    const val MIN_VALID_CANDIDATES = 2
}

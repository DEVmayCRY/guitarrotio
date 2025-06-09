**

# Android Guitar Tuner & Fretboard Visualizer

A simple, yet powerful, musical utility application for Android, built with Kotlin. This app serves as a high-precision chromatic tuner and a visual tool for learning and exploring musical scales on a guitar fretboard.

This project is currently an MVP (Minimum Viable Product) and serves as a foundation for more advanced features to come.

## Current Features

- Chromatic Tuner Mode:
    

- Real-time pitch detection using the robust MPM (McLeod Pitch Method) algorithm.
    
- Displays the detected frequency (Hz), the closest note name, and octave.
    
- Provides precise feedback in cents to show how sharp or flat the note is.
    
- Visual indicators (arrows) for easy and intuitive tuning.
    
- Volume Cutoffs (RMS) to ignore background noise and prevent distorted signal analysis.
    
- A "persistence" feature to prevent the UI from flickering, especially on low or high notes.
    

- Scales Mode:
    

- Fretboard Visualization: Displays a virtual 12-fret guitar neck.
    
- Scale Highlighting: Select a root note and a scale type from a comprehensive list to see all its notes highlighted on the fretboard.
    
- Live Note Display: The note you are currently playing is highlighted in red on the fretboard, even in Scales mode, providing real-time feedback.
    

## Technology Stack

- Language: Kotlin
    
- Audio Processing: TarsosDSP, a powerful library for real-time audio signal processing in Java and Kotlin.
    
- UI: Android Views with Material Components.
    

## Future Features

This project is under active development. Planned features include:

- Advanced Chord Detection: Implementing a robust FFT-based system to accurately detect major, minor, and other chord types.
    
- Custom Tunings: Allowing users to select from a list of alternate guitar tunings (Drop D, Open G, etc.).
    
- Metronome: A built-in metronome for practice.
    
- UI/UX Improvements: Enhancing the visual design and user experience with custom graphics and animations.
    

## How to Build

1. Clone the repository.
    
2. Open the project in Android Studio.
    
3. The project uses a local .jar dependency for TarsosDSP located in the app/libs folder. Gradle should handle it automatically.
    
4. Build and run on an Android device or emulator.
    

**
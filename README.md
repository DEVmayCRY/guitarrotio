# Guitarottio - A Modern Tuner & Music Theory Tool

**Guitarottio** is a high-precision chromatic tuner and an advanced musical tool designed for Android. It provides musicians with an intuitive interface to tune their instruments accurately and a powerful visual aid for studying and exploring a vast library of musical scales on both a guitar fretboard and a piano keyboard.

This project is a functional MVP (Minimum Viable Product), built with a clean, scalable architecture and a focus on performance and accuracy.

## 📸 Screenshots

|Modo Afinador|Modo Escalas|
|---|---|
|![ecrã do afinador](https://github.com/DEVmayCRY/guitarrotio/blob/main/assets/2.png)|![ecrã das escalas](https://github.com/DEVmayCRY/guitarrotio/blob/main/assets/2.png)|

## ✨ Features

- **High-Precision Tuner Mode**:
    
    - **Accurate Pitch Detection**: Utilizes the robust **MPM (McLeod Pitch Method)** algorithm for fast and reliable pitch analysis.

    - **Harmonic Detection**: An excellent **post-processing heuristic** for pitch detection, which I developed using only pure musical mathematics.
        
    - **Detailed Feedback**: Displays the detected frequency (Hz), the closest note name with octave, and the precise deviation in **cents**.
        
    - **Intuitive Visuals**: A simple indicator arrow (← ● →) provides clear visual feedback for sharp, flat, and in-tune notes.
        
    - **Smart Noise Reduction**: Implements an **RMS-based volume cutoff** to ignore background noise and prevent analysis of distorted signals, ensuring stable readings.
        
    - **Reading Stability**: A persistence logic (`MAX_SILENT_FRAMES`) prevents the UI from flickering, especially on low or high notes, by holding the last detected note for a brief moment.


        
- **Interactive Scales Mode**:
    
    - **Dual Visualization**: See scales and notes highlighted simultaneously on both a **guitar fretboard** and a **6-octave piano keyboard**.
        
    - **Comprehensive Scale Library**: Includes over 20 scales, from standard Major/Minor and Pentatonics to exotic scales like Phrygian Dominant and Double Harmonic.
        
    - **Live Feedback**: Play a note on your instrument, and it will instantly light up in red on both the fretboard and the piano, providing real-time context within the selected scale.
        
    - **Easy Selection**: Quickly choose any root note and scale type from simple dropdown menus.
        

## 📦 Installation

Since this app is not yet available on the Google Play Store, you can install it directly by downloading the APK from the **Releases** section of this GitHub repository.

1. Navigate to the [**Releases Page**](https://github.com/DEVmayCRY/guitarottio/releases "null").
    
2. Download the latest `Guitarottio-vX.X.apk` file.
    
3. Open the downloaded file on your Android device.
    
4. You may need to allow installation from "unknown sources" in your device's security settings.
    
5. Follow the on-screen instructions to complete the installation.
    

## 🛠️ Technology Stack

- **Language**: 100% **Kotlin**, following modern Android development practices.
    
- **Architecture**: Clean, feature-based architecture with separation of concerns (`core`, `features`, `ui`).
    
- **Audio Processing**: Powered by **TarsosDSP**, a robust library for real-time audio signal processing.
    
- **UI**: Android Views with custom, high-performance `View` components for the fretboard and piano.
    
- **Concurrency**: **Kotlin Coroutines** for managing background tasks and ensuring a smooth, non-blocking UI.
    

## 🚀 Future Roadmap

This project is a living portfolio piece. Future enhancements include:

- **Advanced Chord Detection**: Implementing an FFT-based system to accurately detect major, minor, and other chord types.
    
- **UI Upgrade:** A better and cool interface, with “harmonic” colors!
    
- **Custom Tunings**: Allowing users to select from a list of alternate guitar tunings (Drop D, Open G, etc.).
    
- **Metronome**: A built-in, precise metronome for practice.
    
- **"Hear the Note" Feature**: The ability to tap a note on the fretboard or piano to hear its corresponding sound.
    

## ☕ Support This Project

This app is developed with passion, free of charge, and without any ads. If you find it useful, please consider supporting its development.
Paypal - [Donate](https://www.paypal.com/donate/?hosted_button_id=GSFW5XEUS525N)

## 📜 License

This project is licensed under the MIT License - see the [LICENSE.md](https://github.com/DEVmayCRY/guitarrotio/blob/main/LICENSE) file for details.
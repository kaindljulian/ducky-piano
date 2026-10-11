package com.github.kaindljulian.piano.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.kaindljulian.piano.Main;
import com.github.kaindljulian.piano.audio.AudioCaptureReader;
import com.github.kaindljulian.piano.audio.MusicAnalyzer;

import java.io.File;

/** Launches the desktop (LWJGL3) application. Pass a WAV file to play it and generate tiles from it. */
public class Lwjgl3Launcher {
    public static void main(String[] args) throws Exception {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication(args.length > 0 ? listenTo(new File(args[0])) : new MusicAnalyzer(44100, 2));
    }

    private static MusicAnalyzer listenTo(File wav) throws Exception {
        WavFileCapture capture = new WavFileCapture(wav);
        MusicAnalyzer music = new MusicAnalyzer(capture.getSampleRate(), capture.getChannelCount());
        new AudioCaptureReader(capture, 2048, music);
        return music;
    }

    private static Lwjgl3Application createApplication(MusicAnalyzer music) {
        return new Lwjgl3Application(new Main(music, null), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Piano106");
        configuration.useVsync(false);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        configuration.setWindowedMode(405, 720);
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}

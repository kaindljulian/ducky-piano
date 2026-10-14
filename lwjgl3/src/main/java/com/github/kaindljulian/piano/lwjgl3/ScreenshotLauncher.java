package com.github.kaindljulian.piano.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.github.kaindljulian.piano.ScreenshotGame;
import com.github.kaindljulian.piano.audio.AudioCapture;
import com.github.kaindljulian.piano.audio.AudioCaptureReader;
import com.github.kaindljulian.piano.audio.MusicAnalyzer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

/**
 * Takes the Google Play phone screenshots at 1080x1920. Run it from the project root with
 * {@code gradlew :lwjgl3:screenshots}. It plays a WAV file into the game silently, on a loop.
 */
public final class ScreenshotLauncher {
    private ScreenshotLauncher() {
    }

    public static void main(String[] args) throws Exception {
        LoopedWav capture = new LoopedWav(new File("core/src/test/resources/piano-loop-120-bpm.wav"));
        MusicAnalyzer music = new MusicAnalyzer(capture.getSampleRate(), capture.getChannelCount());
        new AudioCaptureReader(capture, 2048, music);

        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Ducky Piano screenshots");
        // Taller than most monitors, which only works without a title bar.
        configuration.setWindowedMode(1080, 1920);
        configuration.setDecorated(false);
        configuration.setResizable(false);
        // Off to the right of the monitor, so it doesn't cover the desktop while it plays.
        configuration.setWindowPosition(1920, 0);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);
        new Lwjgl3Application(new ScreenshotGame(music, Path.of("android/play/screenshots"), 150, 405), configuration);
    }

    /** A WAV file handed over in real time and looped forever, without playing it out loud. */
    private static final class LoopedWav implements AudioCapture {
        private final float[] samples;
        private final int sampleRate;
        private final int channelCount;
        private int position;
        private long startNanos;
        private long delivered;
        private volatile boolean recording;

        LoopedWav(File file) throws Exception {
            try (AudioInputStream source = AudioSystem.getAudioInputStream(file)) {
                AudioFormat from = source.getFormat();
                channelCount = from.getChannels();
                sampleRate = Math.round(from.getSampleRate());
                AudioFormat pcm = new AudioFormat(from.getSampleRate(), 16, channelCount, true, false);
                try (AudioInputStream stream = AudioSystem.getAudioInputStream(pcm, source)) {
                    ByteBuffer bytes = ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
                    samples = new float[bytes.remaining() / 2];
                    for (int i = 0; i < samples.length; i++) {
                        samples[i] = bytes.getShort() / 32768f;
                    }
                }
            }
        }

        @Override
        public int getSampleRate() {
            return sampleRate;
        }

        @Override
        public int getChannelCount() {
            return channelCount;
        }

        @Override
        public void start() {
            startNanos = System.nanoTime();
            recording = true;
        }

        @Override
        public int read(float[] buffer, int offset, int length) {
            if (!recording) return -1;
            length -= length % channelCount;
            long due = startNanos + (delivered + length) / channelCount * 1_000_000_000L / sampleRate;
            long wait = due - System.nanoTime();
            if (wait > 0) {
                try {
                    Thread.sleep(wait / 1_000_000, (int) (wait % 1_000_000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
            for (int i = 0; i < length; i++) {
                buffer[offset + i] = samples[position];
                position = (position + 1) % samples.length;
            }
            delivered += length;
            return length;
        }

        @Override
        public void stop() {
            recording = false;
        }

        @Override
        public void close() {
        }
    }
}

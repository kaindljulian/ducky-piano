package com.github.kaindljulian.piano.audio;

/**
 * Tells whether the last block of an audio stream had any sound in it.
 * Feed it from an {@link AudioCaptureReader} and query {@link #isPlaying()} from any thread.
 */
public class SoundDetector implements AudioCaptureReader.Listener {
    private volatile boolean playing;

    @Override
    public void onSamples(float[] samples, int count) {
        for (int i = 0; i < count; i++) {
            if (samples[i] != 0) {
                playing = true;
                return;
            }
        }
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }
}

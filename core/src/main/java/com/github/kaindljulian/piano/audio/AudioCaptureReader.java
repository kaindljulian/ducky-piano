package com.github.kaindljulian.piano.audio;

public class AudioCaptureReader implements AutoCloseable {
    public interface Listener {
        /**
         * Called on the reader thread. The next block overwrites {@code samples}, so copy anything you keep.
         */
        void onSamples(float[] samples, int count);
    }

    private final AudioCapture capture;
    private final Thread thread;
    private volatile boolean running = true;

    /**
     * @param blockSize samples per block, a multiple of the channel count
     */
    public AudioCaptureReader(AudioCapture capture, int blockSize, Listener listener) {
        if (blockSize <= 0 || blockSize % capture.getChannelCount() != 0) {
            throw new IllegalArgumentException("blockSize must be a positive multiple of the channel count");
        }
        this.capture = capture;
        float[] block = new float[blockSize];
        thread = new Thread(() -> {
            while (running) {
                int count = capture.read(block, 0, block.length);
                if (count < 0) break;
                if (count > 0) listener.onSamples(block, count);
            }
        }, "audio-capture");
        thread.setDaemon(true);
        capture.start();
        thread.start();
    }

    /**
     * Stops the capture, waits for the reader thread to finish and releases the capture.
     */
    @Override
    public void close() {
        running = false;
        capture.stop();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        capture.close();
    }
}

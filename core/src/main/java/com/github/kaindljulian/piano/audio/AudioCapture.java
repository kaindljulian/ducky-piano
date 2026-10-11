package com.github.kaindljulian.piano.audio;

public interface AudioCapture extends AutoCloseable {
    int getSampleRate();

    int getChannelCount();

    /**
     * Starts recording. Call this before the first {@link #read}.
     */
    void start();

    /**
     * Blocks until samples arrive and copies up to {@code length} of them into {@code buffer}.
     *
     * @return the number of samples read, a multiple of the channel count, or -1 if the capture is not recording
     */
    int read(float[] buffer, int offset, int length);

    /**
     * Stops recording. A {@link #read} blocked on another thread returns.
     */
    void stop();

    /**
     * Releases the underlying resources. Stop the capture and finish all reads first.
     */
    @Override
    void close();
}

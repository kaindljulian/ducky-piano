package com.github.kaindljulian.piano.audio;

/** Tracks the beat of a live audio stream. Feed it from an {@link AudioCaptureReader} and query it from any thread. */
public class MusicAnalyzer implements AudioCaptureReader.Listener {
    private static final int BEATS_TO_LOCK = 2;
    static final float SILENT_LEVEL = 0.001f;
    private static final float SILENCE_SECONDS = 1;
    private static final int HISTORY = 1024;

    private record Window(long from, long to) {
        long size() {
            return to - from + 1;
        }
    }

    private final int sampleRate;
    private final int channelCount;
    private final double nanosPerSample;
    private final float framesPerSecond;
    private final int silentFramesForSilence;
    private final float[][] bandFlux = new float[HISTORY][OnsetDetector.BANDS];
    private final float[][] bandEnergy = new float[HISTORY][OnsetDetector.BANDS];
    private final float[] levels = new float[HISTORY];
    private OnsetDetector onsets;
    private TempoEstimator tempo;
    private BeatTracker tracker;
    private long samples;
    private long frames;
    private long anchorNanos;
    private int silentFrames;
    private int beatsSinceSilence;
    private volatile Beat latestBeat;

    public MusicAnalyzer(int sampleRate, int channelCount) {
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        nanosPerSample = 1e9 / sampleRate;
        framesPerSecond = (float) sampleRate / OnsetDetector.HOP;
        silentFramesForSilence = Math.round(SILENCE_SECONDS * framesPerSecond);
        reset();
    }

    /** Forgets everything heard so far. Call it before feeding a new stream, while nothing is feeding this one. */
    public synchronized void reset() {
        onsets = new OnsetDetector(sampleRate);
        tempo = new TempoEstimator(framesPerSecond);
        tracker = new BeatTracker();
        samples = 0;
        frames = 0;
        anchorNanos = Long.MAX_VALUE;
        silentFrames = 0;
        beatsSinceSilence = 0;
        latestBeat = null;
    }

    @Override
    public void onSamples(float[] samples, int count) {
        process(samples, count, System.nanoTime());
    }

    /**
     * The latest predicted beat, or null until the tracker locks on. Through silence it stays at the last beat
     * before it, so the beats carry on at the same tempo.
     */
    public Beat latestBeat() {
        return latestBeat;
    }

    /**
     * How much each frequency band rose around {@code nanos}, relative to that band's average over the recent
     * history. Returns null if that time isn't in the history.
     */
    public synchronized float[] bandRise(long nanos, long halfWidthNanos) {
        Window window = window(nanos, halfWidthNanos);
        if (window == null) {
            return null;
        }

        long oldest = oldestFrame();
        float[] rise = new float[OnsetDetector.BANDS];
        for (int band = 0; band < rise.length; band++) {
            float sum = 0;
            for (long frame = window.from(); frame <= window.to(); frame++) {
                sum += bandFlux[slot(frame)][band];
            }
            float total = 0;
            for (long frame = oldest; frame < frames; frame++) {
                total += bandFlux[slot(frame)][band];
            }
            float average = total / (frames - oldest);
            rise[band] = average > 0 ? sum / window.size() / average : 0;
        }
        return rise;
    }

    /** The average energy of each frequency band around {@code nanos}, or null if that time isn't in the history. */
    public synchronized float[] bandEnergy(long nanos, long halfWidthNanos) {
        Window window = window(nanos, halfWidthNanos);
        if (window == null) {
            return null;
        }

        float[] energy = new float[OnsetDetector.BANDS];
        for (long frame = window.from(); frame <= window.to(); frame++) {
            for (int band = 0; band < energy.length; band++) {
                energy[band] += bandEnergy[slot(frame)][band] / window.size();
            }
        }
        return energy;
    }

    /** The average RMS level around {@code nanos}, or NaN if that time isn't in the history. */
    public synchronized float level(long nanos, long halfWidthNanos) {
        Window window = window(nanos, halfWidthNanos);
        if (window == null) {
            return Float.NaN;
        }

        float sum = 0;
        for (long frame = window.from(); frame <= window.to(); frame++) {
            sum += levels[slot(frame)];
        }
        return sum / window.size();
    }

    void process(float[] block, int count, long receivedNanos) {
        samples += count / channelCount;
        // Blocks arrive late by a varying amount, so the earliest arrival seen so far is the best estimate of
        // when sample 0 was captured.
        synchronized (this) {
            anchorNanos = Math.min(anchorNanos, receivedNanos - Math.round(samples * nanosPerSample));
        }
        for (int i = 0; i < count; i += channelCount) {
            float mono = 0;
            for (int channel = 0; channel < channelCount; channel++) {
                mono += block[i + channel];
            }
            if (onsets.add(mono / channelCount)) {
                analyzeFrame();
            }
        }
    }

    private void analyzeFrame() {
        float flux = onsets.flux();
        synchronized (this) {
            for (int band = 0; band < OnsetDetector.BANDS; band++) {
                bandFlux[slot(frames)][band] = onsets.bandFlux(band);
                bandEnergy[slot(frames)][band] = onsets.bandEnergy(band);
            }
            levels[slot(frames)] = onsets.level();
            frames++;
        }
        tempo.add(flux);

        silentFrames = onsets.level() < SILENT_LEVEL ? silentFrames + 1 : 0;
        if (silentFrames >= silentFramesForSilence) {
            beatsSinceSilence = 0;
        }
        if (tracker.add(flux, tempo.period()) && ++beatsSinceSilence >= BEATS_TO_LOCK) {
            long periodNanos = Math.round(tempo.period() * OnsetDetector.HOP * nanosPerSample);
            latestBeat = new Beat(frameNanos(tracker.beat()), periodNanos);
        }
    }

    /** Onset frames are timed at the center of their window. */
    private long frameNanos(long frame) {
        return anchorNanos + Math.round((frame * OnsetDetector.HOP + OnsetDetector.WINDOW / 2) * nanosPerSample);
    }

    private long frameAt(long nanos) {
        return Math.round(((nanos - anchorNanos) / nanosPerSample - OnsetDetector.WINDOW / 2) / OnsetDetector.HOP);
    }

    /** The frames within {@code halfWidthNanos} of {@code nanos}, or null if any of them isn't in the history. */
    private Window window(long nanos, long halfWidthNanos) {
        long center = frameAt(nanos);
        long halfWidth = Math.round(halfWidthNanos / nanosPerSample / OnsetDetector.HOP);
        long from = center - halfWidth;
        long to = center + halfWidth;
        return from < oldestFrame() || to >= frames ? null : new Window(from, to);
    }

    private long oldestFrame() {
        return Math.max(0, frames - HISTORY);
    }

    private static int slot(long frame) {
        return (int) (frame % HISTORY);
    }
}

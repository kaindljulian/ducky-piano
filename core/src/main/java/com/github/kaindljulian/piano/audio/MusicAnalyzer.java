package com.github.kaindljulian.piano.audio;

/** Tracks the beat of a live audio stream. Feed it from an {@link AudioCaptureReader} and query it from any thread. */
public class MusicAnalyzer implements AudioCaptureReader.Listener {
    private static final int BEATS_TO_LOCK = 2;
    private static final float SILENT_LEVEL = 0.001f;
    private static final float SILENCE_SECONDS = 1;
    private static final int HISTORY = 1024;

    private final int sampleRate;
    private final int channelCount;
    private final double nanosPerSample;
    private final float framesPerSecond;
    private final int silentFramesForSilence;
    private final float[][] bandFlux = new float[HISTORY][OnsetDetector.BANDS];
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

    /** The latest predicted beat, or null while no music plays or the tracker hasn't locked on yet. */
    public Beat latestBeat() {
        return latestBeat;
    }

    /**
     * How much each frequency band rose around {@code nanos}, relative to that band's average over the recent
     * history. Returns null if that time isn't in the history.
     */
    public synchronized float[] bandRise(long nanos, long halfWidthNanos) {
        long center = frameAt(nanos);
        long halfWidth = Math.round(halfWidthNanos / nanosPerSample / OnsetDetector.HOP);
        long from = center - halfWidth;
        long to = center + halfWidth;
        long oldest = Math.max(0, frames - HISTORY);
        if (from < oldest || to >= frames) {
            return null;
        }

        float[] rise = new float[OnsetDetector.BANDS];
        for (int band = 0; band < rise.length; band++) {
            float window = 0;
            for (long frame = from; frame <= to; frame++) {
                window += bandFlux[(int) (frame % HISTORY)][band];
            }
            float total = 0;
            for (long frame = oldest; frame < frames; frame++) {
                total += bandFlux[(int) (frame % HISTORY)][band];
            }
            float average = total / (frames - oldest);
            rise[band] = average > 0 ? window / (to - from + 1) / average : 0;
        }
        return rise;
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
                bandFlux[(int) (frames % HISTORY)][band] = onsets.bandFlux(band);
            }
            frames++;
        }
        tempo.add(flux);

        silentFrames = onsets.level() < SILENT_LEVEL ? silentFrames + 1 : 0;
        if (silentFrames >= silentFramesForSilence) {
            beatsSinceSilence = 0;
            latestBeat = null;
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
}

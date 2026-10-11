package com.github.kaindljulian.piano.audio;

/**
 * Causal beat tracking with a cumulative score (Ellis 2007). Half a period after each beat it predicts the
 * next one by extending the score into the future (Stark, Davies and Plumbley 2009).
 */
class BeatTracker {
    private static final float ALPHA = 0.9f;
    private static final float TIGHTNESS = 5;
    private static final int HISTORY = 512;

    private final float[] score = new float[HISTORY];
    private final float[] future = new float[HISTORY];
    private long frame;
    private long nextPrediction;
    private long beat = -1;

    /** Adds the next onset frame. Returns true when this frame predicted a new {@link #beat()}. */
    boolean add(float onset, float period) {
        long now = frame++;
        score[index(now)] = period > 0 ? (1 - ALPHA) * onset + ALPHA * bestPrevious(now, period, now) : onset;
        if (period == 0 || now < nextPrediction) {
            return false;
        }
        predict(now, period);
        return true;
    }

    /** The onset frame of the latest predicted beat, which can lie in the future. */
    long beat() {
        return beat;
    }

    private void predict(long now, float period) {
        double expected = beat >= 0 ? beat + period : now + period / 2;
        double sigma = period / 2;
        float bestValue = -1;
        long best = now + 1;
        for (int i = 1; i <= Math.round(period); i++) {
            future[i] = ALPHA * bestPrevious(now + i, period, now);
            double distance = (now + i - expected) / sigma;
            float value = (float) (future[i] * Math.exp(-0.5 * distance * distance));
            if (value > bestValue) {
                bestValue = value;
                best = now + i;
            }
        }
        beat = best;
        nextPrediction = best + Math.round(period / 2);
    }

    /**
     * The best score one period before {@code at}, weighted by a log-Gaussian around that period.
     * Frames after {@code now} come from the extrapolated {@link #future} scores.
     */
    private float bestPrevious(long at, float period, long now) {
        float best = 0;
        for (long v = at - Math.round(2 * period); v <= at - Math.round(period / 2); v++) {
            if (v < 0 || v <= now - HISTORY) {
                continue;
            }
            float previous = v <= now ? score[index(v)] : future[(int) (v - now)];
            double logRatio = TIGHTNESS * Math.log((at - v) / period);
            best = Math.max(best, (float) (previous * Math.exp(-0.5 * logRatio * logRatio)));
        }
        return best;
    }

    private static int index(long frame) {
        return (int) (frame % HISTORY);
    }
}

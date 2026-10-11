package com.github.kaindljulian.piano.audio;

import java.util.Arrays;

/** Estimates the beat period from the autocorrelation of the last few seconds of onset strength. */
class TempoEstimator {
    private static final float MIN_BPM = 55;
    private static final float MAX_BPM = 230;
    private static final float PRIOR_BPM = 120;
    private static final float PRIOR_WIDTH_OCTAVES = 1;
    private static final int HARMONICS = 4;
    private static final float FASTER_PULSE_RATIO = 0.8f;
    private static final float HISTORY_SECONDS = 8;
    private static final float ESTIMATES_PER_SECOND = 4;
    private static final int THRESHOLD_RADIUS = 8;
    private static final float CHANGE_WIDTH_OCTAVES = 0.1f;
    private static final float SWITCH_FLOOR = 0.002f;

    private final float framesPerSecond;
    private final int minLag;
    private final int maxLag;
    private final int minFrames;
    private final int estimateEvery;
    private final float[] history;
    private final float[] signal;
    private final float[] thresholded;
    private final float[] autocorrelation;
    private final float[][] transition;
    private final float[] belief;
    private final float[] next;
    private long count;
    private float period;

    TempoEstimator(float framesPerSecond) {
        this.framesPerSecond = framesPerSecond;
        minLag = (int) Math.floor(60 * framesPerSecond / MAX_BPM);
        maxLag = (int) Math.ceil(60 * framesPerSecond / MIN_BPM);
        history = new float[Math.round(HISTORY_SECONDS * framesPerSecond)];
        signal = new float[history.length];
        thresholded = new float[history.length];
        autocorrelation = new float[HARMONICS * (maxLag + 1) + 1];
        minFrames = autocorrelation.length + 1;
        estimateEvery = Math.round(framesPerSecond / ESTIMATES_PER_SECOND);

        int lags = maxLag - minLag + 1;
        transition = new float[lags][lags];
        for (int from = 0; from < lags; from++) {
            for (int to = 0; to < lags; to++) {
                double octaves = log2((double) (minLag + to) / (minLag + from)) / CHANGE_WIDTH_OCTAVES;
                transition[from][to] = (float) Math.exp(-0.5 * octaves * octaves);
            }
        }
        belief = new float[lags];
        next = new float[lags];
        Arrays.fill(belief, 1f / lags);
    }

    void add(float onset) {
        history[(int) (count++ % history.length)] = onset;
        if (count >= minFrames && count % estimateEvery == 0) {
            estimate();
        }
    }

    /** The beat period in onset frames, or 0 while unknown. */
    float period() {
        return period;
    }

    /**
     * A hidden Markov model over the candidate periods, as in BTrack: the belief carries over to nearby periods,
     * so the tempo drifts freely, but a jump needs several estimates in a row that agree. {@link #SWITCH_FLOOR}
     * keeps every period reachable, which bounds how long a real tempo change takes.
     */
    private void estimate() {
        computeAutocorrelation();
        float total = 0;
        float maxBelief = 0;
        for (float value : belief) {
            maxBelief = Math.max(maxBelief, value);
        }
        for (int to = 0; to < next.length; to++) {
            float carried = SWITCH_FLOOR * maxBelief;
            for (int from = 0; from < belief.length; from++) {
                carried = Math.max(carried, belief[from] * transition[from][to]);
            }
            next[to] = score(minLag + to) * carried;
            total += next[to];
        }
        if (total == 0) {
            return;
        }
        int best = 0;
        for (int i = 0; i < belief.length; i++) {
            belief[i] = next[i] / total;
            if (belief[i] > belief[best]) {
                best = i;
            }
        }
        period = refine(minLag + best);
    }

    private void computeAutocorrelation() {
        int n = (int) Math.min(count, history.length);
        for (int i = 0; i < n; i++) {
            signal[i] = history[(int) ((count - n + i) % history.length)];
        }
        // Subtracting the local mean keeps only the peaks, so sustained loudness doesn't flatten the autocorrelation.
        for (int i = 0; i < n; i++) {
            int from = Math.max(0, i - THRESHOLD_RADIUS);
            int to = Math.min(n - 1, i + THRESHOLD_RADIUS);
            float sum = 0;
            for (int j = from; j <= to; j++) {
                sum += signal[j];
            }
            thresholded[i] = Math.max(0, signal[i] - sum / (to - from + 1));
        }
        for (int lag = 0; lag < autocorrelation.length; lag++) {
            float sum = 0;
            for (int i = 0; i + lag < n; i++) {
                sum += thresholded[i] * thresholded[i + lag];
            }
            autocorrelation[lag] = sum / (n - lag);
        }
    }

    /**
     * Comb filter over the autocorrelation at multiples of the lag, weighted by a log-Gaussian tempo prior that
     * favors periods near {@link #PRIOR_BPM}.
     */
    private float score(int lag) {
        float comb = 0;
        for (int harmonic = 1; harmonic <= HARMONICS; harmonic++) {
            comb += peak(harmonic * lag, harmonic);
        }
        double octaves = log2(60 * framesPerSecond / lag / PRIOR_BPM) / PRIOR_WIDTH_OCTAVES;
        float prior = (float) Math.exp(-0.5 * octaves * octaves);
        return comb * prior * (hasFasterPulse(lag, 2) || hasFasterPulse(lag, 3) ? 0.5f : 1);
    }

    /**
     * A click track at 200 BPM fits a 100 or 67 BPM comb just as well, so a lag counts as too slow when the points
     * dividing it into {@code parts} are about as strong as the lag itself.
     */
    private boolean hasFasterPulse(int lag, int parts) {
        if ((float) lag / parts < minLag) {
            return false;
        }
        float between = 0;
        for (int part = 1; part < parts; part++) {
            between += energy((float) lag * part / parts) + energy((float) lag * (parts + part) / parts);
        }
        float beat = energy(lag) + energy(2 * lag);
        return beat > 0 && between / (parts - 1) >= FASTER_PULSE_RATIO * beat;
    }

    /** A fractional period smears its autocorrelation peak over neighboring frames, so this sums them. */
    private float energy(float lag) {
        int center = Math.round(lag);
        float sum = 0;
        for (int i = Math.max(0, center - 1); i <= Math.min(autocorrelation.length - 1, center + 1); i++) {
            sum += autocorrelation[i];
        }
        return sum;
    }

    /** The true period is rarely a whole number of frames, so each multiple takes the peak within a radius. */
    private float peak(float lag, int radius) {
        int center = Math.round(lag);
        float peak = 0;
        for (int i = Math.max(0, center - radius); i <= Math.min(autocorrelation.length - 1, center + radius); i++) {
            peak = Math.max(peak, autocorrelation[i]);
        }
        return peak;
    }

    /**
     * Whole-frame lags are too coarse: 34 and 35 frames are 152 and 148 BPM. The peak at the highest multiple,
     * located with parabolic interpolation, divides that error by {@link #HARMONICS}.
     */
    private float refine(int lag) {
        int peak = HARMONICS * lag;
        for (int candidate = HARMONICS * (lag - 1); candidate <= HARMONICS * lag + HARMONICS - 1; candidate++) {
            if (autocorrelation[candidate] > autocorrelation[peak]) {
                peak = candidate;
            }
        }
        float left = autocorrelation[peak - 1];
        float center = autocorrelation[peak];
        float right = autocorrelation[peak + 1];
        float curvature = left - 2 * center + right;
        float offset = curvature < 0 ? 0.5f * (left - right) / curvature : 0;
        return (peak + offset) / HARMONICS;
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }
}

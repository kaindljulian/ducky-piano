package com.github.kaindljulian.piano.audio;

import java.util.Arrays;

/**
 * Spectral flux onset detection: how much the log-magnitude spectrum grew since the previous frame,
 * in total and per frequency band.
 */
class OnsetDetector {
    static final int WINDOW = 1024;
    static final int HOP = 512;
    static final int BANDS = 4;
    private static final float[] BAND_EDGES_HZ = {200, 800, 3000};
    private static final float COMPRESSION = 100;

    private final Fft fft = new Fft(WINDOW);
    private final float[] hann = new float[WINDOW];
    private final float[] input = new float[WINDOW];
    private final float[] re = new float[WINDOW];
    private final float[] im = new float[WINDOW];
    private final float[] previous = new float[WINDOW / 2 + 1];
    private final int[] bandOfBin = new int[WINDOW / 2 + 1];
    private final float[] bandFlux = new float[BANDS];
    private final float[] bandEnergy = new float[BANDS];
    private int filled;
    private float flux;
    private float level;

    OnsetDetector(int sampleRate) {
        for (int i = 0; i < WINDOW; i++) {
            hann[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / WINDOW));
        }
        for (int bin = 0; bin < bandOfBin.length; bin++) {
            float hz = (float) bin * sampleRate / WINDOW;
            int band = 0;
            while (band < BAND_EDGES_HZ.length && hz >= BAND_EDGES_HZ[band]) {
                band++;
            }
            bandOfBin[bin] = band;
        }
    }

    /** Returns true when this sample completed a frame, which updates {@link #flux()} and {@link #bandFlux}. */
    boolean add(float sample) {
        input[filled++] = sample;
        if (filled < WINDOW) {
            return false;
        }
        analyze();
        System.arraycopy(input, HOP, input, 0, WINDOW - HOP);
        filled = WINDOW - HOP;
        return true;
    }

    float flux() {
        return flux;
    }

    /** The RMS of the frame's samples. */
    float level() {
        return level;
    }

    float bandFlux(int band) {
        return bandFlux[band];
    }

    /** The spectral energy of the frame in a band. */
    float bandEnergy(int band) {
        return bandEnergy[band];
    }

    private void analyze() {
        float sumOfSquares = 0;
        for (int i = 0; i < WINDOW; i++) {
            sumOfSquares += input[i] * input[i];
            re[i] = input[i] * hann[i];
            im[i] = 0;
        }
        level = (float) Math.sqrt(sumOfSquares / WINDOW);
        fft.transform(re, im);

        flux = 0;
        Arrays.fill(bandFlux, 0);
        Arrays.fill(bandEnergy, 0);
        for (int bin = 0; bin < previous.length; bin++) {
            float energy = re[bin] * re[bin] + im[bin] * im[bin];
            bandEnergy[bandOfBin[bin]] += energy;
            float magnitude = (float) Math.sqrt(energy);
            float level = (float) Math.log1p(COMPRESSION * magnitude);
            float rise = level - previous[bin];
            previous[bin] = level;
            if (rise > 0) {
                flux += rise;
                bandFlux[bandOfBin[bin]] += rise;
            }
        }
    }
}

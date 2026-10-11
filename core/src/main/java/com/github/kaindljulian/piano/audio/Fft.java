package com.github.kaindljulian.piano.audio;

/** In-place iterative radix-2 FFT. */
class Fft {
    private final int size;
    private final float[] cos;
    private final float[] sin;
    private final int[] bitReversed;

    Fft(int size) {
        if (Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("size must be a power of two");
        }
        this.size = size;
        cos = new float[size / 2];
        sin = new float[size / 2];
        for (int i = 0; i < size / 2; i++) {
            cos[i] = (float) Math.cos(2 * Math.PI * i / size);
            sin[i] = (float) Math.sin(2 * Math.PI * i / size);
        }
        bitReversed = new int[size];
        int bits = Integer.numberOfTrailingZeros(size);
        for (int i = 0; i < size; i++) {
            bitReversed[i] = Integer.reverse(i) >>> (32 - bits);
        }
    }

    void transform(float[] re, float[] im) {
        for (int i = 0; i < size; i++) {
            int j = bitReversed[i];
            if (j > i) {
                float swap = re[i];
                re[i] = re[j];
                re[j] = swap;
                swap = im[i];
                im[i] = im[j];
                im[j] = swap;
            }
        }
        for (int length = 2; length <= size; length *= 2) {
            int half = length / 2;
            int step = size / length;
            for (int start = 0; start < size; start += length) {
                for (int k = 0; k < half; k++) {
                    float c = cos[k * step];
                    float s = sin[k * step];
                    int a = start + k;
                    int b = a + half;
                    float re2 = re[b] * c + im[b] * s;
                    float im2 = im[b] * c - re[b] * s;
                    re[b] = re[a] - re2;
                    im[b] = im[a] - im2;
                    re[a] += re2;
                    im[a] += im2;
                }
            }
        }
    }
}

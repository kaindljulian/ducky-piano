package com.github.kaindljulian.piano.audio;

import com.badlogic.gdx.utils.Array;

import java.util.Arrays;

/**
 * Lane shapes a hand can learn, up to 4 tiles long. A pattern is either 2 lanes wide, on the left or the right half,
 * or 4 lanes wide.
 */
final class Patterns {
    static final int MAX_TILES = 4;
    /** {@link #half} of a pattern that spans both halves. */
    static final int WIDE = -1;
    private static final int[][] SHAPES = {
        {0, 1, 0, 1},
        {1, 0, 1, 0},
        {2, 3, 2, 3},
        {3, 2, 3, 2},
        {0, 1, 2, 3},
        {3, 2, 1, 0},
        {0, 2, 1, 3},
        {3, 1, 2, 0},
    };

    private Patterns() {
    }

    /**
     * Lanes for notes whose loudest bands are {@code bands}. Picks the shape closest to the bands, so a melody that
     * climbs gets a stair and one that sits on a band gets a trill next to it.
     */
    static int[] fit(int[] bands, int previousLane, int previousHalf) {
        int[] best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int[] shape : SHAPES) {
            int[] lanes = follow(shape, bands.length, previousLane, previousHalf);
            if (lanes == null) {
                continue;
            }
            int distance = 0;
            for (int i = 0; i < bands.length; i++) {
                distance += Math.abs(shape[i] - bands[i]);
            }
            if (distance < bestDistance) {
                best = lanes;
                bestDistance = distance;
            }
        }
        return best;
    }

    static int[] random(int count, int previousLane, int previousHalf) {
        Array<int[]> options = new Array<>();
        for (int[] shape : SHAPES) {
            int[] lanes = follow(shape, count, previousLane, previousHalf);
            if (lanes != null) {
                options.add(lanes);
            }
        }
        return options.random();
    }

    /**
     * The first {@code count} lanes of a shape, mirrored when it would repeat a 2 wide pattern on the same half, or
     * null when it would start on the previous lane.
     */
    private static int[] follow(int[] shape, int count, int previousLane, int previousHalf) {
        int[] lanes = Arrays.copyOf(shape, count);
        if (repeatsHalf(lanes, previousHalf)) {
            lanes = mirror(lanes);
        }
        return lanes[0] == previousLane ? null : lanes;
    }

    /** Whether a pattern can come next: it doesn't start on the previous lane or repeat a 2 wide half. */
    static boolean follows(int[] lanes, int previousLane, int previousHalf) {
        return lanes[0] != previousLane && !repeatsHalf(lanes, previousHalf);
    }

    private static boolean repeatsHalf(int[] lanes, int previousHalf) {
        return half(lanes) != WIDE && half(lanes) == previousHalf;
    }

    /** 0 if all lanes are on the left half, 1 if all are on the right half, else {@link #WIDE}. */
    static int half(int[] lanes) {
        int half = WIDE;
        for (int lane : lanes) {
            int laneHalf = lane / 2;
            if (half != WIDE && laneHalf != half) {
                return WIDE;
            }
            half = laneHalf;
        }
        return half;
    }

    private static int[] mirror(int[] lanes) {
        int[] mirrored = new int[lanes.length];
        for (int i = 0; i < lanes.length; i++) {
            mirrored[i] = ChartGenerator.LANES - 1 - lanes[i];
        }
        return mirrored;
    }
}

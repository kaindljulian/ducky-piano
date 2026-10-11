package com.github.kaindljulian.piano.audio;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ChartGeneratorTest {
    private static final int BEATS = 100;

    @ParameterizedTest
    @ValueSource(floats = {1, 1.3f, 1.5f, 2})
    void placesTilesPerBeatAsSet(float tilesPerBeat) {
        ChartGenerator chart = new ChartGenerator(new MusicAnalyzer(44100, 2));
        chart.setTilesPerBeat(tilesPerBeat);

        int tiles = 0;
        int previousLane = -1;
        for (int beat = 0; beat < BEATS; beat++) {
            for (int lane : chart.lanes(4)) {
                assertNotEquals(previousLane, lane);
                previousLane = lane;
                tiles++;
            }
        }
        assertEquals(tilesPerBeat * BEATS, tiles, 1);
    }
}

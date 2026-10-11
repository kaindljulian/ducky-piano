package com.github.kaindljulian.piano.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeatClockTest {
    private static final long HALF_SECOND = 500_000_000L;

    @Test
    void startsOnTheBeat() {
        BeatClock clock = new BeatClock();
        long now = 1_000_000_000_000L;

        assertEquals(0.5, clock.update(new Beat(now - HALF_SECOND / 2, HALF_SECOND), now), 1e-9);
    }

    @Test
    void easesIntoANewTempo() {
        BeatClock clock = new BeatClock();
        long now = 1_000_000_000_000L;
        Beat slow = new Beat(now, HALF_SECOND);
        clock.update(slow, now);

        Beat fast = new Beat(now, HALF_SECOND / 2);
        double before = clock.update(fast, now += 16_000_000L);
        double after = clock.update(fast, now += 16_000_000L);
        double beatsPerFrame = after - before;

        assertTrue(beatsPerFrame > 0.032 && beatsPerFrame < 0.04, "advanced " + beatsPerFrame + " beats");
    }
}

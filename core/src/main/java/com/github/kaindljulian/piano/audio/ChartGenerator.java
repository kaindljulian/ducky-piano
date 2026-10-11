package com.github.kaindljulian.piano.audio;

import com.badlogic.gdx.math.MathUtils;

/**
 * Picks tile lanes from the music. A tile's beat hasn't been heard yet when it spawns, but music repeats by the
 * bar, so the lane comes from the frequency band that rose most at the same beat two bars earlier.
 */
public class ChartGenerator {
    public static final int LANES = OnsetDetector.BANDS;
    private static final int LOOKBACK_BEATS = 8;

    private final MusicAnalyzer music;
    private int last = -1;
    private int secondLast = -1;

    public ChartGenerator(MusicAnalyzer music) {
        this.music = music;
    }

    public int lane(double beatsAhead) {
        int lane = laneFromMusic(beatsAhead);
        if (lane == last && lane == secondLast) {
            lane = (lane + 1) % LANES;
        }
        secondLast = last;
        last = lane;
        return lane;
    }

    private int laneFromMusic(double beatsAhead) {
        Beat beat = music.latestBeat();
        if (beat == null) {
            return MathUtils.random(LANES - 1);
        }
        long nanos = System.nanoTime() + Math.round((beatsAhead - LOOKBACK_BEATS) * beat.periodNanos());
        float[] rise = music.bandRise(nanos, beat.periodNanos() / 4);
        if (rise == null) {
            return MathUtils.random(LANES - 1);
        }
        int lane = 0;
        for (int band = 1; band < rise.length; band++) {
            if (rise[band] > rise[lane]) {
                lane = band;
            }
        }
        return lane;
    }
}

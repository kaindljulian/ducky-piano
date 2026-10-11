package com.github.kaindljulian.piano.audio;

import com.badlogic.gdx.math.MathUtils;
import com.github.kaindljulian.piano.Board;

/**
 * Builds the chart from the music. A tile's beat hasn't been heard yet when it spawns, but music repeats by the
 * bar, so every decision looks at the same point two bars earlier: the lane comes from the frequency band that
 * rose most there, and the off-beats that get an extra tile are the ones where the song had its strongest notes.
 */
public class ChartGenerator implements Board.Chart {
    public static final int LANES = OnsetDetector.BANDS;
    private static final int LOOKBACK_BEATS = 8;
    private static final int RECENT_OFFBEATS = 16;

    private final MusicAnalyzer music;
    private final float[] recentOffbeats = new float[RECENT_OFFBEATS];
    private int offbeatCount;
    private float tilesPerBeat = 1;
    private float extraTileCredit;
    private int lastLane = -1;

    public ChartGenerator(MusicAnalyzer music) {
        this.music = music;
    }

    /** Between 1 (a tile on every beat) and 2 (a tile on every 8th note). */
    public void setTilesPerBeat(float tilesPerBeat) {
        this.tilesPerBeat = tilesPerBeat;
    }

    @Override
    public int[] lanes(double beatsAhead) {
        Beat beat = music.latestBeat();
        int onBeat = nextLane(rise(beat, beatsAhead));
        float[] offBeatRise = rise(beat, beatsAhead + 0.5);
        if (!hasOffbeatTile(offBeatRise)) {
            return new int[]{onBeat};
        }
        return new int[]{onBeat, nextLane(offBeatRise)};
    }

    private float[] rise(Beat beat, double beatsAhead) {
        if (beat == null) {
            return null;
        }
        long nanos = System.nanoTime() + Math.round((beatsAhead - LOOKBACK_BEATS) * beat.periodNanos());
        return music.bandRise(nanos, beat.periodNanos() / 8);
    }

    /** The band that rose most, skipping the previous tile's lane so neighboring tiles never merge into one. */
    private int nextLane(float[] rise) {
        int lane;
        if (rise == null) {
            lane = MathUtils.random(lastLane < 0 ? LANES - 1 : LANES - 2);
            if (lane >= lastLane && lastLane >= 0) {
                lane++;
            }
        } else {
            lane = -1;
            for (int band = 0; band < rise.length; band++) {
                if (band != lastLane && (lane < 0 || rise[band] > rise[lane])) {
                    lane = band;
                }
            }
        }
        lastLane = lane;
        return lane;
    }

    /**
     * An off-beat gets a tile when fewer than {@code tilesPerBeat - 1} of the recent off-beats were stronger, so
     * the share of extra tiles matches the setting and they land where the song has notes between the beats.
     * Without music to go by, the extra tiles are spread evenly.
     */
    private boolean hasOffbeatTile(float[] rise) {
        float share = tilesPerBeat - 1;
        if (rise == null) {
            extraTileCredit += share;
            if (extraTileCredit < 1) {
                return false;
            }
            extraTileCredit -= 1;
            return true;
        }

        float strength = 0;
        for (float band : rise) {
            strength += band;
        }
        recentOffbeats[offbeatCount++ % RECENT_OFFBEATS] = strength;
        int recent = Math.min(offbeatCount, RECENT_OFFBEATS);
        int stronger = 0;
        for (int i = 0; i < recent; i++) {
            if (recentOffbeats[i] > strength) {
                stronger++;
            }
        }
        return stronger < share * recent;
    }
}

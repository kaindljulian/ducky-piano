package com.github.kaindljulian.piano.audio;

import com.github.kaindljulian.piano.Board;
import com.github.kaindljulian.piano.Board.Note;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

/**
 * Builds the chart from the music, one bar at a time. A bar's beats haven't been heard yet when it's planned, but
 * music repeats by the bar, so each bar follows the same bar two bars earlier. The rhythm comes from where the song
 * had its strongest notes, quiet stretches turn into hold tiles, the lanes follow a learnable shape that matches the
 * frequency bands that rose, and a bar that sounds like a recent one gets the same tiles again.
 */
public class ChartGenerator implements Board.Chart {
    public static final int LANES = OnsetDetector.BANDS;
    private static final int BEATS_PER_BAR = 4;
    private static final int SLOTS = 2 * BEATS_PER_BAR;
    private static final int LOOKBACK_BEATS = 8;
    private static final int RECENT_OFFBEATS = 16;
    private static final int RECENT_BEATS = 16;
    /** A beat below this share of the recent median holds the note before it. */
    private static final float HOLD_RATIO = 0.6f;
    /** A note rings on while its band keeps this share of the energy it started with... */
    private static final float RING_RATIO = 0.5f;
    /** ...and rises less than its average there, so no new note starts in it. */
    private static final float NEW_NOTE_RISE = 1;
    /** An off-beat below this share of the recent median gets no tile. */
    private static final float QUIET_RATIO = 0.35f;
    /** Each half bar gets its own pattern. A slot holds at most one note, so a pattern stays within its length. */
    private static final int SLOTS_PER_PATTERN = Patterns.MAX_TILES;
    private static final int REMEMBERED_BARS = 8;
    private static final float REPEAT_SIMILARITY = 0.9f;
    private static final int NO_NOTE = -1;

    /** How much each band rose, the energy of each band and how loud the music was, at each eighth note of a bar. */
    record BarSound(float[][] rise, float[][] energy, float[] level) {
    }

    /** The lane and hold length of the note at each eighth note, with {@link #NO_NOTE} where there is none. */
    private record Bar(float[] features, int[] lanes, float[] lengths) {
    }

    private final MusicAnalyzer music;
    private final Queue<Note[]> planned = new ArrayDeque<>();
    private final float[] recentOffbeats = new float[RECENT_OFFBEATS];
    private final float[] recentBeats = new float[RECENT_BEATS];
    private final Bar[] bars = new Bar[REMEMBERED_BARS];
    private int offbeatCount;
    private int beatCount;
    private int barCount;
    private float tilesPerBeat = 1;
    private float extraTileCredit;
    private int lastLane = NO_NOTE;
    private int lastHalf = Patterns.WIDE;

    public ChartGenerator(MusicAnalyzer music) {
        this.music = music;
    }

    /** Between 1 (a tile on every beat) and 2 (a tile on every 8th note). */
    public void setTilesPerBeat(float tilesPerBeat) {
        if (tilesPerBeat != this.tilesPerBeat) {
            Arrays.fill(bars, null);
        }
        this.tilesPerBeat = tilesPerBeat;
    }

    @Override
    public Note[] notes(double beatsAhead) {
        if (planned.isEmpty()) {
            Collections.addAll(planned, plan(sound(beatsAhead, System.nanoTime())));
        }
        return planned.remove();
    }

    /** The notes of each beat of the next bar. Without music to go by, the tiles follow a random shape. */
    Note[][] plan(BarSound sound) {
        Bar bar = sound == null ? guess() : listen(sound);
        Note[][] beats = new Note[BEATS_PER_BAR][];
        for (int beat = 0; beat < BEATS_PER_BAR; beat++) {
            List<Note> notes = new ArrayList<>();
            for (int slot = 2 * beat; slot < 2 * beat + 2; slot++) {
                if (bar.lanes()[slot] != NO_NOTE) {
                    notes.add(new Note(bar.lanes()[slot], slot % 2 / 2f, bar.lengths()[slot]));
                    lastLane = bar.lanes()[slot];
                }
            }
            beats[beat] = notes.toArray(new Note[0]);
        }
        for (int from = 0; from < SLOTS; from += SLOTS_PER_PATTERN) {
            int[] pattern = pattern(bar.lanes(), from);
            if (pattern.length > 0) {
                lastHalf = Patterns.half(pattern);
            }
        }
        return beats;
    }

    /** What the music did two bars before the bar that starts {@code beatsAhead} beats after {@code now}. */
    BarSound sound(double beatsAhead, long now) {
        Beat beat = music.latestBeat();
        if (beat == null) {
            return null;
        }
        float[][] rise = new float[SLOTS][];
        float[][] energy = new float[SLOTS][];
        float[] level = new float[SLOTS];
        long halfWidth = beat.periodNanos() / 8;
        for (int slot = 0; slot < SLOTS; slot++) {
            double beatsBack = beatsAhead + slot / 2.0 - LOOKBACK_BEATS;
            long nanos = now + Math.round(beatsBack * beat.periodNanos());
            rise[slot] = music.bandRise(nanos, halfWidth);
            energy[slot] = music.bandEnergy(nanos, halfWidth);
            level[slot] = music.level(nanos, halfWidth);
            if (rise[slot] == null) {
                return null;
            }
        }
        return new BarSound(rise, energy, level);
    }

    private Bar guess() {
        float[] lengths = new float[SLOTS];
        Arrays.fill(lengths, NO_NOTE);
        for (int beat = 0; beat < BEATS_PER_BAR; beat++) {
            lengths[2 * beat] = 0;
            extraTileCredit += tilesPerBeat - 1;
            if (extraTileCredit >= 1) {
                extraTileCredit -= 1;
                lengths[2 * beat + 1] = 0;
            }
        }
        return new Bar(null, lanes(lengths, null), lengths);
    }

    private Bar listen(BarSound sound) {
        float[][] rise = sound.rise();
        float[] lengths = rhythm(sound);
        float[] features = features(rise);
        Bar repeat = mostSimilar(features);
        Bar bar;
        if (repeat != null) {
            bar = new Bar(features, mirrorIfClashing(repeat.lanes()), repeat.lengths());
        } else {
            bar = new Bar(features, lanes(lengths, rise), lengths);
        }
        // Until the recent beats fill up, quiet beats aren't recognized yet, so those bars aren't worth repeating.
        if (beatCount >= RECENT_BEATS) {
            bars[barCount++ % REMEMBERED_BARS] = bar;
        }
        return bar;
    }

    /**
     * A note on every beat and on the off-beats that {@link #hasOffbeatTile} picks. Slots much quieter than the
     * recent beats get no note. The note before a beat is held through it while that note {@link #ringsOn}, or while
     * the beat is quiet but not silent. Silent beats stay empty.
     */
    private float[] rhythm(BarSound sound) {
        float[][] rise = sound.rise();
        float[] lengths = new float[SLOTS];
        Arrays.fill(lengths, NO_NOTE);
        int previous = NO_NOTE;
        for (int beat = 0; beat < BEATS_PER_BAR; beat++) {
            int on = 2 * beat;
            float strength = strength(rise[on]);
            float median = median(recentBeats, Math.min(beatCount, RECENT_BEATS));
            recentBeats[beatCount++ % RECENT_BEATS] = strength;
            boolean quiet = strength < HOLD_RATIO * median;
            if (sound.level()[on] < MusicAnalyzer.SILENT_LEVEL) {
                previous = NO_NOTE;
                continue;
            }
            if (previous != NO_NOTE && (quiet || ringsOn(sound, previous, on))) {
                lengths[previous] = (on + 2 - previous) / 2f;
                continue;
            }
            lengths[on] = 0;
            previous = on;
            float[] offbeat = rise[on + 1];
            if (!quiet && hasOffbeatTile(offbeat) && strength(offbeat) >= QUIET_RATIO * median) {
                lengths[on + 1] = 0;
                previous = on + 1;
            }
        }
        return lengths;
    }

    /**
     * Whether the note at slot {@code from} still sounds at slot {@code at}: its loudest band holds most of its
     * energy and has no new note. This finds held notes even when drums hit every beat in other bands.
     */
    private static boolean ringsOn(BarSound sound, int from, int at) {
        int band = loudestBand(sound.rise()[from]);
        return sound.rise()[at][band] < NEW_NOTE_RISE
            && sound.energy()[at][band] > RING_RATIO * sound.energy()[from][band];
    }

    /**
     * An off-beat gets a tile when fewer than {@code tilesPerBeat - 1} of the recent off-beats were stronger, so
     * the share of extra tiles matches the setting and they land where the song has notes between the beats.
     */
    private boolean hasOffbeatTile(float[] rise) {
        float strength = strength(rise);
        recentOffbeats[offbeatCount++ % RECENT_OFFBEATS] = strength;
        int recent = Math.min(offbeatCount, RECENT_OFFBEATS);
        int stronger = 0;
        for (int i = 0; i < recent; i++) {
            if (recentOffbeats[i] > strength) {
                stronger++;
            }
        }
        return stronger < (tilesPerBeat - 1) * recent;
    }

    /** The remembered bar that rose most alike, by correlation, or null if none comes close enough. */
    private Bar mostSimilar(float[] features) {
        Bar best = null;
        float bestSimilarity = REPEAT_SIMILARITY;
        for (Bar bar : bars) {
            if (bar == null) {
                continue;
            }
            float similarity = 0;
            for (int i = 0; i < features.length; i++) {
                similarity += features[i] * bar.features()[i];
            }
            if (similarity >= bestSimilarity) {
                best = bar;
                bestSimilarity = similarity;
            }
        }
        return best;
    }

    /**
     * A repeated bar gets mirrored when its first pattern would start on the last lane, where two tiles would merge,
     * or repeat a 2 wide pattern on the same half.
     */
    private int[] mirrorIfClashing(int[] lanes) {
        for (int from = 0; from < SLOTS; from += SLOTS_PER_PATTERN) {
            int[] pattern = pattern(lanes, from);
            if (pattern.length == 0) {
                continue;
            }
            if (Patterns.follows(pattern, lastLane, lastHalf)) {
                return lanes;
            }
            int[] mirrored = new int[SLOTS];
            for (int slot = 0; slot < SLOTS; slot++) {
                mirrored[slot] = lanes[slot] == NO_NOTE ? NO_NOTE : LANES - 1 - lanes[slot];
            }
            return mirrored;
        }
        return lanes;
    }

    /** Gives the notes of each half bar a pattern that follows the one before. Without music, a random one. */
    private int[] lanes(float[] lengths, float[][] rise) {
        int[] lanes = new int[SLOTS];
        Arrays.fill(lanes, NO_NOTE);
        int previousLane = lastLane;
        int previousHalf = lastHalf;
        for (int from = 0; from < SLOTS; from += SLOTS_PER_PATTERN) {
            int[] slots = IntStream.range(from, from + SLOTS_PER_PATTERN)
                .filter(slot -> lengths[slot] != NO_NOTE)
                .toArray();
            if (slots.length == 0) {
                continue;
            }
            int[] pattern;
            if (rise == null) {
                pattern = Patterns.random(slots.length, previousLane, previousHalf);
            } else {
                int[] bands = new int[slots.length];
                for (int i = 0; i < slots.length; i++) {
                    bands[i] = loudestBand(rise[slots[i]]);
                }
                pattern = Patterns.fit(bands, previousLane, previousHalf);
            }
            for (int i = 0; i < slots.length; i++) {
                lanes[slots[i]] = pattern[i];
            }
            previousLane = pattern[pattern.length - 1];
            previousHalf = Patterns.half(pattern);
        }
        return lanes;
    }

    /** The lanes of the notes in the pattern starting at slot {@code from}. */
    private static int[] pattern(int[] lanes, int from) {
        return IntStream.range(from, from + SLOTS_PER_PATTERN)
            .map(slot -> lanes[slot])
            .filter(lane -> lane != NO_NOTE)
            .toArray();
    }

    /** The rises of the whole bar as one vector with mean 0 and length 1, so a dot product is the correlation. */
    private static float[] features(float[][] rise) {
        float[] features = new float[SLOTS * LANES];
        float mean = 0;
        for (int slot = 0; slot < SLOTS; slot++) {
            for (int band = 0; band < LANES; band++) {
                features[slot * LANES + band] = rise[slot][band];
                mean += rise[slot][band] / features.length;
            }
        }
        float length = 0;
        for (int i = 0; i < features.length; i++) {
            features[i] -= mean;
            length += features[i] * features[i];
        }
        length = (float) Math.sqrt(length);
        for (int i = 0; i < features.length; i++) {
            features[i] = length > 0 ? features[i] / length : 0;
        }
        return features;
    }

    private static int loudestBand(float[] rise) {
        int loudest = 0;
        for (int band = 1; band < rise.length; band++) {
            if (rise[band] > rise[loudest]) {
                loudest = band;
            }
        }
        return loudest;
    }

    private static float strength(float[] rise) {
        float strength = 0;
        for (float band : rise) {
            strength += band;
        }
        return strength;
    }

    private static float median(float[] values, int count) {
        if (count == 0) {
            return 0;
        }
        float[] sorted = Arrays.copyOf(values, count);
        Arrays.sort(sorted);
        return sorted[count / 2];
    }
}

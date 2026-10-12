package com.github.kaindljulian.piano.audio;

import com.github.kaindljulian.piano.Board.Note;
import com.github.kaindljulian.piano.audio.ChartGenerator.BarSound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartGeneratorTest {
    private static final int BEATS = 100;
    private static final int BLOCK = 2048;
    private static final int WARM_UP_BARS = 4;
    private static final float SOUNDING = 0.1f;
    private static final int SLOTS = 8;

    private final ChartGenerator chart = new ChartGenerator(new MusicAnalyzer(44100, 2));

    @ParameterizedTest
    @ValueSource(floats = {1, 1.3f, 1.5f, 2})
    void placesTilesPerBeatAsSet(float tilesPerBeat) {
        chart.setTilesPerBeat(tilesPerBeat);

        int tiles = 0;
        int previousLane = -1;
        for (int beat = 0; beat < BEATS; beat++) {
            for (Note note : chart.notes(4)) {
                assertNotEquals(previousLane, note.lane());
                previousLane = note.lane();
                tiles++;
            }
        }
        assertEquals(tilesPerBeat * BEATS, tiles, 1);
    }

    @Test
    void holdsThroughQuietBeatsWhileTheMusicSounds() {
        playLoudBars();
        float[][] rise = new float[SLOTS][4];
        Arrays.fill(rise[0], 1);

        Note[][] beats = chart.plan(sound(rise, levels(SOUNDING)));

        assertEquals(1, beats[0].length);
        assertEquals(4, beats[0][0].length());
        assertEquals(0, beats[1].length + beats[2].length + beats[3].length);
    }

    @Test
    void holdsANoteThatRingsOnWhileDrumsKeepHitting() {
        playLoudBars();
        float[][] rise = new float[SLOTS][4];
        float[][] energy = new float[SLOTS][4];
        for (int slot = 0; slot < SLOTS; slot += 2) {
            Arrays.fill(rise[slot], 1.5f);
            Arrays.fill(energy[slot], 1);
        }
        rise[0][1] = 3;
        rise[2][1] = 0.2f;
        energy[2][1] = 0.8f;

        Note[][] beats = chart.plan(new BarSound(rise, energy, levels(SOUNDING)));

        assertEquals(2, beats[0][0].length());
        assertEquals(0, beats[1].length);
        assertEquals(1, beats[2].length);
    }

    @Test
    void silentBeatsStayEmpty() {
        playLoudBars();
        float[][] rise = new float[SLOTS][4];
        Arrays.fill(rise[0], 1);
        float[] level = new float[SLOTS];
        level[0] = SOUNDING;
        level[1] = SOUNDING;

        Note[][] beats = chart.plan(sound(rise, level));

        assertEquals(1, beats[0].length);
        assertEquals(0, beats[0][0].length());
        assertEquals(0, beats[1].length + beats[2].length + beats[3].length);
    }

    @Test
    void climbingBandsGiveAStair() {
        chart.setTilesPerBeat(2);

        assertArrayEquals(new int[]{0, 1, 2, 3, 0, 1, 2, 3}, lanes(chart.plan(loudestBands(0, 1, 2, 3, 0, 1, 2, 3))));
    }

    @Test
    void aRepeatedTrillSwitchesSides() {
        chart.setTilesPerBeat(2);

        assertArrayEquals(new int[]{0, 1, 0, 1, 3, 2, 3, 2}, lanes(chart.plan(loudestBands(0, 1, 0, 1, 0, 1, 0, 1))));
    }

    @Test
    void aBarThatSoundsLikeARecentOneGetsTheSameTiles() {
        chart.setTilesPerBeat(1.5f);
        playLoudBars();
        Random random = new Random(1);
        BarSound verse = randomBar(random);
        Note[][] first = chart.plan(verse);
        chart.plan(randomBar(random));
        Note[][] again = chart.plan(verse);

        Note[][] mirrored = Arrays.stream(first)
            .map(beat -> Arrays.stream(beat).map(n -> new Note(3 - n.lane(), n.offset(), n.length())).toArray(Note[]::new))
            .toArray(Note[][]::new);
        assertTrue(Arrays.deepEquals(first, again) || Arrays.deepEquals(mirrored, again));
    }

    @Test
    void chartsTheClicksOfARecordingOnTheBeatsAndRepeatsThem() throws Exception {
        MusicAnalyzerTest.Audio audio = MusicAnalyzerTest.wav("m.130_bpm.wav");
        MusicAnalyzer music = audio.analyzer();
        ChartGenerator clicks = new ChartGenerator(music);
        clicks.setTilesPerBeat(1.5f);

        List<Note[][]> bars = new ArrayList<>();
        float[] block = new float[BLOCK];
        long nextBar = 0;
        for (int start = 0; start + BLOCK <= audio.samples().length; start += BLOCK) {
            System.arraycopy(audio.samples(), start, block, 0, BLOCK);
            long now = Math.round((double) (start + BLOCK) / audio.channelCount() / audio.sampleRate() * 1e9);
            music.process(block, BLOCK, now);
            Beat beat = music.latestBeat();
            if (beat == null || now < nextBar) {
                continue;
            }
            // The board asks for a bar as it scrolls into view, 3 to 4 beats ahead.
            double toNextBeat = (double) Math.floorMod(beat.nanos() - now, beat.periodNanos()) / beat.periodNanos();
            bars.add(clicks.plan(clicks.sound(3 + toNextBeat, now)));
            nextBar = now + 4 * beat.periodNanos();
        }

        assertTrue(bars.size() > 10);
        for (int bar = WARM_UP_BARS + 2; bar < bars.size(); bar++) {
            for (Note[] notes : bars.get(bar)) {
                assertEquals(1, notes.length);
                assertEquals(0, notes[0].offset());
            }
            assertTrue(Arrays.deepEquals(bars.get(bar - 2), bars.get(bar)));
        }
    }

    private void playLoudBars() {
        float[][] rise = new float[SLOTS][4];
        for (float[] slot : rise) {
            Arrays.fill(slot, 1);
        }
        for (int bar = 0; bar < WARM_UP_BARS; bar++) {
            chart.plan(sound(rise, levels(SOUNDING)));
        }
    }

    private static BarSound loudestBands(int... bands) {
        float[][] rise = new float[SLOTS][4];
        for (int slot = 0; slot < SLOTS; slot++) {
            Arrays.fill(rise[slot], 0.5f);
            rise[slot][bands[slot]] = 2;
        }
        return sound(rise, levels(SOUNDING));
    }

    private static BarSound randomBar(Random random) {
        float[][] rise = new float[SLOTS][4];
        for (float[] slot : rise) {
            for (int band = 0; band < slot.length; band++) {
                slot[band] = 0.5f + random.nextFloat();
            }
        }
        return sound(rise, levels(SOUNDING));
    }


    private static BarSound sound(float[][] rise, float[] level) {
        return new BarSound(rise, new float[SLOTS][4], level);
    }

    private static float[] levels(float level) {
        float[] levels = new float[SLOTS];
        Arrays.fill(levels, level);
        return levels;
    }

    private static int[] lanes(Note[][] beats) {
        return Arrays.stream(beats).flatMap(Arrays::stream).mapToInt(Note::lane).toArray();
    }
}

package com.github.kaindljulian.piano.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MusicAnalyzerTest {
    private static final int SAMPLE_RATE = 44100;
    private static final int BLOCK = 2048;
    private static final double FIRST_CLICK_SECONDS = 0.25;
    private static final double LOCK_SECONDS = 8;

    record Audio(float[] samples, int sampleRate, int channelCount) {
        static Audio mono(float[] samples) {
            return new Audio(samples, SAMPLE_RATE, 1);
        }

        MusicAnalyzer analyzer() {
            return new MusicAnalyzer(sampleRate, channelCount);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {90, 120, 150})
    void findsTheTempo(double bpm) {
        Audio audio = Audio.mono(clicks(bpm, 12));
        MusicAnalyzer analyzer = audio.analyzer();
        feed(analyzer, audio, 0);

        Beat beat = analyzer.latestBeat();
        assertNotNull(beat);
        assertEquals(bpm, bpm(beat), 1);
    }

    @ParameterizedTest
    @ValueSource(doubles = {90, 120, 150})
    void predictsBeatsOnTheClicks(double bpm) {
        Audio audio = Audio.mono(clicks(bpm, 20));
        List<Beat> beats = feed(audio.analyzer(), audio, 0);

        double period = 60 / bpm;
        List<Beat> locked = beats.stream().filter(beat -> beat.nanos() > LOCK_SECONDS * 1e9).toList();
        assertFalse(locked.isEmpty());
        for (Beat beat : locked) {
            double offset = (beat.nanos() / 1e9 - FIRST_CLICK_SECONDS) % period;
            double error = Math.min(offset, period - offset);
            assertTrue(error < 0.03, "beat " + beat.nanos() / 1e9 + "s is " + error * 1000 + "ms off");
        }
    }

    @Test
    void keepsTheBeatThroughSilence() {
        MusicAnalyzer analyzer = new MusicAnalyzer(SAMPLE_RATE, 1);
        feed(analyzer, Audio.mono(clicks(120, 12)), 0);
        feed(analyzer, Audio.mono(new float[SAMPLE_RATE * 4]), 12);

        assertEquals(120, bpm(analyzer.latestBeat()), 1);
    }

    @Test
    void ignoresQuietNoise() {
        float[] noise = new float[SAMPLE_RATE * 10];
        Random random = new Random(1);
        for (int i = 0; i < noise.length; i++) {
            noise[i] = (random.nextFloat() * 2 - 1) * 0.0003f;
        }
        Audio audio = Audio.mono(noise);

        assertTrue(feed(audio.analyzer(), audio, 0).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "m.130_bpm.wav, 130",
        "m.170_bpm.wav, 170",
        "m.220_bpm.wav, 220",
        "piano-loop-120-bpm.wav, 120",
    })
    void holdsTheTempoOfRecordings(String file, double bpm) throws Exception {
        Audio audio = wav(file);
        List<Beat> locked = feed(audio.analyzer(), audio, 0).stream()
            .filter(beat -> beat.nanos() > LOCK_SECONDS * 1e9)
            .toList();

        assertFalse(locked.isEmpty());
        for (Beat beat : locked) {
            assertEquals(bpm, bpm(beat), 1.5, "at " + beat.nanos() / 1e9 + "s");
        }
    }

    @Test
    void followsTempoSwitches() throws Exception {
        Audio audio = wav("60_200_bpm_switch_10s.wav");
        MusicAnalyzer analyzer = audio.analyzer();
        double[][] expected = {{9, 60}, {24, 200}, {34, 60}, {44, 200}, {54, 60}, {64, 200}};

        double fed = 0;
        for (double[] check : expected) {
            fed = feed(analyzer, audio, fed, check[0]);
            assertEquals(check[1], bpm(analyzer.latestBeat()), 1.5, "at " + check[0] + "s");
        }
    }

    private static double bpm(Beat beat) {
        return 60e9 / beat.periodNanos();
    }

    private static List<Beat> feed(MusicAnalyzer analyzer, Audio audio, double startSeconds) {
        List<Beat> beats = new ArrayList<>();
        float[] block = new float[BLOCK];
        int frames = BLOCK / audio.channelCount();
        for (int start = 0; start + BLOCK <= audio.samples().length; start += BLOCK) {
            System.arraycopy(audio.samples(), start, block, 0, BLOCK);
            double seconds = startSeconds + (double) (start / audio.channelCount() + frames) / audio.sampleRate();
            analyzer.process(block, BLOCK, Math.round(seconds * 1e9));
            Beat beat = analyzer.latestBeat();
            if (beat != null && (beats.isEmpty() || !beats.get(beats.size() - 1).equals(beat))) {
                beats.add(beat);
            }
        }
        return beats;
    }

    /** Feeds the audio between two times as if it was captured in real time. Returns where it stopped. */
    private static double feed(MusicAnalyzer analyzer, Audio audio, double fromSeconds, double toSeconds) {
        int from = (int) Math.round(fromSeconds * audio.sampleRate()) * audio.channelCount();
        int to = (int) Math.round(toSeconds * audio.sampleRate()) * audio.channelCount();
        int length = (to - from) / BLOCK * BLOCK;
        float[] part = new float[length];
        System.arraycopy(audio.samples(), from, part, 0, length);
        feed(analyzer, new Audio(part, audio.sampleRate(), audio.channelCount()), fromSeconds);
        return fromSeconds + (double) length / audio.channelCount() / audio.sampleRate();
    }

    static Audio wav(String resource) throws Exception {
        var file = new BufferedInputStream(MusicAnalyzerTest.class.getResourceAsStream("/" + resource));
        try (AudioInputStream source = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = source.getFormat();
            AudioFormat pcm = new AudioFormat(format.getSampleRate(), 16, format.getChannels(), true, false);
            byte[] bytes = AudioSystem.getAudioInputStream(pcm, source).readAllBytes();
            float[] samples = new float[bytes.length / 2];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = (short) ((bytes[2 * i + 1] << 8) | (bytes[2 * i] & 0xff)) / 32768f;
            }
            return new Audio(samples, Math.round(format.getSampleRate()), format.getChannels());
        }
    }

    /** Short decaying noise bursts, the first at {@link #FIRST_CLICK_SECONDS}. */
    private static float[] clicks(double bpm, double seconds) {
        float[] audio = new float[(int) (seconds * SAMPLE_RATE)];
        Random random = new Random(1);
        int clickLength = SAMPLE_RATE / 50;
        for (double t = FIRST_CLICK_SECONDS; t < seconds; t += 60 / bpm) {
            int start = (int) Math.round(t * SAMPLE_RATE);
            for (int i = 0; i < clickLength && start + i < audio.length; i++) {
                audio[start + i] = (float) ((random.nextDouble() * 2 - 1) * Math.exp(-5.0 * i / clickLength));
            }
        }
        return audio;
    }
}

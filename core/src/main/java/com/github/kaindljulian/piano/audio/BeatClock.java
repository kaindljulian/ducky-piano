package com.github.kaindljulian.piano.audio;

/** A smooth beat count that follows the latest {@link Beat}. Whole numbers fall on beats. */
public class BeatClock {
    private static final long RESYNC_AFTER_NANOS = 500_000_000L;
    private static final double TEMPO_CHANGE_SECONDS = 2;
    private static final double PHASE_CORRECTION = 0.3;

    private double beats;
    private double beatsPerNano;
    private long lastNanos;
    private boolean started;

    public double update(Beat beat, long nanos) {
        double target = (double) (nanos - beat.nanos()) / beat.periodNanos();
        double targetBeatsPerNano = 1.0 / beat.periodNanos();
        long elapsed = nanos - lastNanos;
        if (!started || elapsed > RESYNC_AFTER_NANOS) {
            beats = target;
            beatsPerNano = targetBeatsPerNano;
        } else {
            beatsPerNano += (targetBeatsPerNano - beatsPerNano) * Math.min(1, elapsed / (TEMPO_CHANGE_SECONDS * 1e9));
            // Phase-locked loop: only the fraction of the target matters. Running up to 15% faster or slower
            // closes the phase error gradually, so the count never jumps or runs backwards.
            double error = target - beats;
            error -= Math.round(error);
            beats += elapsed * beatsPerNano * (1 + PHASE_CORRECTION * error);
        }
        lastNanos = nanos;
        started = true;
        return beats;
    }
}

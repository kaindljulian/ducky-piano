package com.github.kaindljulian.piano.audio;

/** A beat at {@code nanos} on the {@link System#nanoTime()} clock, with the period of the beats around it. */
public record Beat(long nanos, long periodNanos) {
}

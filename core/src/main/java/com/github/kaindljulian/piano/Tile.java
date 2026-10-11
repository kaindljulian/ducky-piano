package com.github.kaindljulian.piano;

/** A black tile in one lane, one cell tall, hit at beat {@code start}. */
public class Tile {
    public final int lane;
    public final float start;
    boolean tapped;
    boolean missed;

    Tile(int lane, float start) {
        this.lane = lane;
        this.start = start;
    }

    public boolean isTapped() {
        return tapped;
    }

    public boolean isMissed() {
        return missed;
    }
}

package com.github.kaindljulian.piano;

/**
 * A black tile in one lane, hit at beat {@code start}. A plain tile is one cell tall. A hold tile is {@code length}
 * beats tall and is held until its top reaches the finger.
 */
public class Tile {
    public final int lane;
    public final float start;
    public final float length;
    boolean tapped;
    boolean missed;
    boolean held;
    /** How many beats above the screen bottom the finger went down on a hold tile. */
    float heldAt;

    Tile(int lane, float start, float length) {
        this.lane = lane;
        this.start = start;
        this.length = length;
    }

    public boolean isHold() {
        return length > 0;
    }

    public boolean isTapped() {
        return tapped;
    }

    public boolean isMissed() {
        return missed;
    }

    public boolean isHeld() {
        return held;
    }

    public float getHeldAt() {
        return heldAt;
    }
}

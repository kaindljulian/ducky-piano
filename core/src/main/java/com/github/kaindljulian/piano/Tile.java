package com.github.kaindljulian.piano;

/**
 * A black tile in one lane, hit at beat {@code start}. A plain tile is one row tall. A hold tile is {@code length}
 * beats of connected rows and is held until its top reaches the finger.
 */
public class Tile {
    public final int lane;
    public final float start;
    public final float length;
    boolean tapped;
    boolean missed;
    boolean held;
    /** How many cells above the screen bottom the finger went down on a hold tile. */
    float heldAt;
    /** How many cells above the tile's start the finger went down. */
    float grabbedAt;
    /** How many of the tile's rows the finger has reached. */
    int passed;

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

    public float getGrabbedAt() {
        return grabbedAt;
    }

    public int getPassed() {
        return passed;
    }
}

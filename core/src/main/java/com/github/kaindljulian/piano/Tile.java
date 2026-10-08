package com.github.kaindljulian.piano;

/** A black tile occupying one cell of the board. */
public class Tile {
    public final int lane;
    public final int row;
    boolean tapped;

    Tile(int lane, int row) {
        this.lane = lane;
        this.row = row;
    }

    public boolean isTapped() {
        return tapped;
    }
}

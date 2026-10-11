package com.github.kaindljulian.piano;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import java.util.function.DoubleToIntFunction;


/** A grid of tiles with one row per beat. */
public class Board {
    public enum State { READY, RUNNING, OVER }

    public final int lanes;
    public final int visibleRows;
    private final DoubleToIntFunction laneForBeatsAhead;

    /** Ordered by row, lowest first. */
    private final Array<Tile> tiles = new Array<>();
    private int nextRow;
    private float scroll;
    private double origin;
    private boolean aligned;
    private State state = State.READY;
    private Tile failed;

    public Board(int lanes, int visibleRows, DoubleToIntFunction laneForBeatsAhead) {
        this.lanes = lanes;
        this.visibleRows = visibleRows;
        this.laneForBeatsAhead = laneForBeatsAhead;
        spawn();
    }

    /** Scrolls to a beat count whose whole numbers fall on beats, so each row lands at the bottom on a beat. */
    public void update(double beats) {
        if (state != State.RUNNING) return;
        if (!aligned) {
            origin = Math.round(beats - scroll);
            aligned = true;
        }
        scroll = (float) (beats - origin);

        Tile next = nextTile();
        if (next != null && next.row + 1 < scroll) {
            fail(next);
            return;
        }
        while (tiles.notEmpty() && tiles.first().tapped && tiles.first().row + 1 < scroll) {
            tiles.removeIndex(0);
        }
        spawn();
    }

    /** Lines the rows up with the beat count again on the next update, e.g. after a pause. */
    public void resync() {
        aligned = false;
    }

    /** Handles a tap at a position in cell coordinates relative to the screen bottom. */
    public void tap(float x, float y) {
        if (state == State.OVER || x < 0 || x >= lanes) return;
        int lane = (int) x;
        int row = MathUtils.floor(y + scroll);

        Tile hit = tileAt(lane, row);
        if (hit == null) {
            if (state == State.RUNNING) fail(new Tile(lane, row));
        } else if (hit == nextTile()) {
            hit.tapped = true;
            state = State.RUNNING;
        }
    }

    public Array<Tile> getTiles() {
        return tiles;
    }

    /** How many rows the board has scrolled so far. A tile is drawn at {@code row - scroll}. */
    public float getScroll() {
        return scroll;
    }

    public State getState() {
        return state;
    }

    /** The tile that ended the game: a missed tile or the empty cell the player tapped. */
    public Tile getFailed() {
        return failed;
    }

    private void fail(Tile tile) {
        failed = tile;
        state = State.OVER;
    }

    private Tile nextTile() {
        for (Tile tile : tiles) {
            if (!tile.tapped) return tile;
        }
        return null;
    }

    private Tile tileAt(int lane, int row) {
        for (Tile tile : tiles) {
            if (tile.lane == lane && tile.row == row) return tile;
        }
        return null;
    }

    /** Keeps the board filled one row past the top edge, so tiles never pop in on screen. */
    private void spawn() {
        while (nextRow <= scroll + visibleRows) {
            tiles.add(new Tile(laneForBeatsAhead.applyAsInt(nextRow - scroll), nextRow++));
        }
    }
}

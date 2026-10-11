package com.github.kaindljulian.piano;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;


/**
 * Tiles on a beat grid. Each beat holds one or more tiles that split it evenly. Every tile is one cell tall and
 * the board scrolls {@link #getCellsPerBeat()} cells per beat, so more tiles per beat means faster scrolling.
 */
public class Board {
    public enum State { READY, RUNNING }

    public interface Chart {
        /** The lanes of the tiles that split the beat {@code beatsAhead} beats from now, in order. */
        int[] lanes(double beatsAhead);
    }

    public final int lanes;
    public final int visibleCells;
    private final Chart chart;

    /** Ordered by start, earliest first. */
    private final Array<Tile> tiles = new Array<>();
    private int nextBeat;
    private float scroll;
    private double origin;
    private boolean aligned;
    private State state = State.READY;
    private int combo;
    private Tile wrongTap;
    private float cellsPerBeat = 1;

    public Board(int lanes, int visibleCells, Chart chart) {
        this.lanes = lanes;
        this.visibleCells = visibleCells;
        this.chart = chart;
        spawn();
    }

    /** Scrolls to a beat count whose whole numbers fall on beats, so each beat reaches the bottom on time. */
    public void update(double beats) {
        if (state != State.RUNNING) return;
        if (!aligned) {
            origin = Math.round(beats - scroll);
            aligned = true;
        }
        scroll = (float) (beats - origin);

        while (tiles.notEmpty() && end(tiles.first()) < scroll) {
            Tile gone = tiles.removeIndex(0);
            if (!gone.tapped && !gone.missed) {
                combo = 0;
            }
        }
        if (wrongTap != null && end(wrongTap) < scroll) {
            wrongTap = null;
        }
        spawn();
    }

    /** Lines the beats up with the beat count again on the next update, e.g. after a pause. */
    public void resync() {
        aligned = false;
    }

    /**
     * Handles a tap at a position in cell coordinates relative to the screen bottom. Any open tile can be hit.
     * Open tiles before it count as missed.
     */
    public void tap(float x, float y) {
        if (x < 0 || x >= lanes) return;
        int lane = (int) x;
        float position = scroll + y / cellsPerBeat;

        Tile hit = tileAt(lane, position);
        if (hit == null) {
            if (state == State.RUNNING) {
                combo = 0;
                int slotsPerBeat = MathUtils.ceil(cellsPerBeat);
                wrongTap = new Tile(lane, (float) MathUtils.floor(position * slotsPerBeat) / slotsPerBeat);
            }
            return;
        }
        if (hit.tapped || hit.missed) return;

        for (Tile tile : tiles) {
            if (tile == hit) break;
            if (!tile.tapped && !tile.missed) {
                tile.missed = true;
                combo = 0;
            }
        }
        hit.tapped = true;
        combo++;
        state = State.RUNNING;
    }

    public Array<Tile> getTiles() {
        return tiles;
    }

    public void setCellsPerBeat(float cellsPerBeat) {
        this.cellsPerBeat = cellsPerBeat;
    }

    public float getCellsPerBeat() {
        return cellsPerBeat;
    }

    /** How many beats the board has scrolled so far. A tile is drawn {@code start - scroll} beats up. */
    public float getScroll() {
        return scroll;
    }

    public State getState() {
        return state;
    }

    /** Hits in a row since the last missed tile or tap on an empty cell. */
    public int getCombo() {
        return combo;
    }

    /** The empty cell the player last tapped, until it scrolls off. */
    public Tile getWrongTap() {
        return wrongTap;
    }

    private Tile tileAt(int lane, float position) {
        for (Tile tile : tiles) {
            if (tile.lane == lane && tile.start <= position && position < end(tile)) return tile;
        }
        return null;
    }

    private float end(Tile tile) {
        return tile.start + 1 / cellsPerBeat;
    }

    /** Keeps the board filled one beat past the top edge, so tiles never pop in on screen. */
    private void spawn() {
        while (nextBeat <= scroll + visibleCells / cellsPerBeat) {
            int[] beatLanes = chart.lanes(nextBeat - scroll);
            for (int i = 0; i < beatLanes.length; i++) {
                tiles.add(new Tile(beatLanes[i], nextBeat + (float) i / beatLanes.length));
            }
            nextBeat++;
        }
    }
}

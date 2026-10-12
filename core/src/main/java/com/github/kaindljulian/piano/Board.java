package com.github.kaindljulian.piano;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;


/**
 * Tiles on a beat grid. Plain tiles are one cell tall and the board scrolls {@link #getCellsPerBeat()} cells per
 * beat, so more tiles per beat means faster scrolling. Hold tiles span whole beats.
 */
public class Board {
    public enum State { READY, RUNNING }

    /** A tile {@code offset} beats into its beat. A {@code length} above 0 makes it a hold tile that many beats long. */
    public record Note(int lane, float offset, float length) {
    }

    public interface Chart {
        /** The notes of the beat {@code beatsAhead} beats from now, in order. */
        Note[] notes(double beatsAhead);
    }

    /** Letting go of a hold this many beats before its end still completes it. */
    private static final float RELEASE_TOLERANCE = 0.25f;

    public final int lanes;
    public final int visibleCells;
    private final Chart chart;

    /** Ordered by start, earliest first. */
    private final Array<Tile> tiles = new Array<>();
    private final IntMap<Tile> heldByPointer = new IntMap<>();
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

        IntMap.Values<Tile> held = heldByPointer.values();
        while (held.hasNext()) {
            Tile tile = held.next();
            if (scroll + tile.heldAt >= tile.start + tile.length) {
                tile.held = false;
                held.remove();
            }
        }
        for (int i = tiles.size - 1; i >= 0; i--) {
            Tile tile = tiles.get(i);
            if (end(tile) >= scroll) continue;
            tiles.removeIndex(i);
            if (!tile.tapped && !tile.missed) {
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
     * Handles a finger going down at a position in cell coordinates relative to the screen bottom. Any open tile
     * can be hit. Open tiles before it count as missed.
     */
    public void tap(float x, float y, int pointer) {
        if (x < 0 || x >= lanes) return;
        int lane = (int) x;
        float position = scroll + y / cellsPerBeat;

        Tile hit = tileAt(lane, position);
        if (hit == null) {
            if (state == State.RUNNING) {
                combo = 0;
                int slotsPerBeat = MathUtils.ceil(cellsPerBeat);
                wrongTap = new Tile(lane, (float) MathUtils.floor(position * slotsPerBeat) / slotsPerBeat, 0);
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
        if (hit.isHold()) {
            hit.held = true;
            hit.heldAt = position - scroll;
            heldByPointer.put(pointer, hit);
        }
        combo++;
        state = State.RUNNING;
    }

    /** Handles a finger going up. Letting go of a hold tile well before its end breaks the combo. */
    public void release(int pointer) {
        Tile tile = heldByPointer.remove(pointer);
        if (tile == null) return;
        tile.held = false;
        if (scroll + tile.heldAt < tile.start + tile.length - RELEASE_TOLERANCE) {
            tile.missed = true;
            combo = 0;
        }
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
        return tile.start + Math.max(tile.length, 1 / cellsPerBeat);
    }

    /** Keeps the board filled one beat past the top edge, so tiles never pop in on screen. */
    private void spawn() {
        while (nextBeat <= scroll + visibleCells / cellsPerBeat) {
            for (Note note : chart.notes(nextBeat - scroll)) {
                tiles.add(new Tile(note.lane(), nextBeat + note.offset(), note.length()));
            }
            nextBeat++;
        }
    }
}

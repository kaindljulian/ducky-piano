package com.github.kaindljulian.piano;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntMap;

import java.util.Arrays;

/**
 * Tiles on a grid of rows one cell tall. A beat gets one row or two, and an off-beat tile always goes in the second
 * row. Plain tiles fill one row. Hold tiles are a run of connected rows that the finger goes down in the first of and
 * comes up in the last of, and every row it is dragged through counts as a hit.
 * <p>
 * Each beat passes in time with the music, so a beat with two rows scrolls past twice as fast. The {@link #setSpeed
 * speed} sets how many beats get two rows, and the second row stays empty when the chart has no off-beat tile for
 * it. The board eases into each change of speed, so for a few beats around a two-row beat it runs a little ahead of
 * the music.
 */
public class Board {
    public enum State { READY, RUNNING }

    /** A tile {@code offset} beats into its beat. A {@code length} above 0 makes it a hold tile that many beats long. */
    public record Note(int lane, float offset, float length) {
    }

    /**
     * Spreads a change of {@code rows} per beat at {@code beat} over {@code half} beats either side of it. The speed
     * changes evenly across that span, so the board drifts off the beat and is back on it once the span is over.
     */
    private record Ramp(int beat, float half, int rows) {
        /** How many cells the board is ahead of the beat at a scroll position. */
        float offset(float scroll) {
            float left = half - Math.abs(scroll - beat);
            return left > 0 ? rows * left * left / (4 * half) : 0;
        }
    }

    public interface Chart {
        /** The notes of the beat {@code beatsAhead} beats from now, in order. */
        Note[] notes(double beatsAhead);
    }

    public final int lanes;
    public final int visibleCells;
    private final Chart chart;

    /** Ordered by start, earliest first. */
    private final Array<Tile> tiles = new Array<>();
    private final IntMap<Tile> heldByPointer = new IntMap<>();
    private int nextBeat;
    /** The cell each spawned beat starts on, plus where the next one will. */
    private final FloatArray beatCells = FloatArray.with(0);
    private final Array<Ramp> ramps = new Array<>();
    private int lastRows = 1;
    private float speed = 1;
    /** Builds up by the speed above 1 each beat, and a beat with two rows uses up 1 of it. */
    private float splitCredit;
    private float scroll;
    private double origin;
    private boolean aligned;
    private State state = State.READY;
    private int combo;
    private Tile wrongTap;

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
        for (int i = ramps.size - 1; i >= 0; i--) {
            Ramp ramp = ramps.get(i);
            if (scroll >= ramp.beat() + ramp.half()) {
                ramps.removeIndex(i);
            }
        }
        float bottom = bottom();

        IntMap.Values<Tile> held = heldByPointer.values();
        while (held.hasNext()) {
            Tile tile = held.next();
            if (drag(tile) >= cell(tile.start + tile.length)) {
                tile.held = false;
                held.remove();
            }
        }
        for (int i = tiles.size - 1; i >= 0; i--) {
            Tile tile = tiles.get(i);
            if (cell(end(tile)) >= bottom) continue;
            tiles.removeIndex(i);
            if (!tile.tapped && !tile.missed) {
                combo = 0;
            }
        }
        if (wrongTap != null && cell(end(wrongTap)) < bottom) {
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
     * can be hit. Open tiles before it count as missed, and so does a hold tile hit above its first row.
     */
    public void tap(float x, float y, int pointer) {
        if (x < 0 || x >= lanes) return;
        int lane = (int) x;
        float tapCell = bottom() + y;
        float position = beatAt(tapCell);

        Tile hit = tileAt(lane, position);
        if (hit == null) {
            if (state == State.RUNNING) {
                combo = 0;
                wrongTap = new Tile(lane, beatAt(MathUtils.floor(tapCell)), 0);
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
        state = State.RUNNING;
        if (hit.isHold() && tapCell >= cell(hit.start) + 1) {
            hit.missed = true;
            combo = 0;
            return;
        }
        hit.tapped = true;
        hit.passed = 1;
        if (hit.isHold()) {
            hit.held = true;
            hit.heldAt = y;
            hit.grabbedAt = tapCell - cell(hit.start);
            heldByPointer.put(pointer, hit);
        }
        combo++;
    }

    /** Handles a finger going up. Letting go of a hold tile before its last row breaks the combo. */
    public void release(int pointer) {
        Tile tile = heldByPointer.remove(pointer);
        if (tile == null) return;
        tile.held = false;
        if (drag(tile) < cell(tile.start + tile.length) - 1) {
            tile.missed = true;
            combo = 0;
        }
    }

    public Array<Tile> getTiles() {
        return tiles;
    }

    /** The average rows per beat, from 1 to 2. Takes effect from the next beat to spawn. */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /** How many beats the board has scrolled so far. */
    public float getScroll() {
        return scroll;
    }

    /** How many cells above the screen bottom a beat count is. */
    public float y(float beat) {
        return cell(beat) - bottom();
    }

    public State getState() {
        return state;
    }

    /** Hits in a row since the last missed tile or tap on an empty cell. */
    public int getCombo() {
        return combo;
    }

    void setCombo(int combo) {
        this.combo = combo;
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

    /** Counts a hit for each row of a held tile the finger reached since last time, and returns the finger's cell. */
    private float drag(Tile tile) {
        float finger = bottom() + tile.heldAt;
        float bottom = cell(tile.start);
        int rows = Math.round(cell(tile.start + tile.length) - bottom);
        int reached = Math.min(MathUtils.floor(finger - bottom) + 1, rows);
        combo += Math.max(reached - tile.passed, 0);
        tile.passed = Math.max(tile.passed, reached);
        return finger;
    }

    private float end(Tile tile) {
        return tile.isHold() ? tile.start + tile.length : beatAt(cell(tile.start) + 1);
    }

    /** The cell at the screen bottom. */
    private float bottom() {
        float bottom = cell(scroll);
        for (Ramp ramp : ramps) {
            bottom += ramp.offset(scroll);
        }
        return bottom;
    }

    /** The cell a beat count falls on. Outside the spawned beats, each beat has one row. */
    private float cell(float beat) {
        int index = MathUtils.floor(beat);
        float from = beatCell(index);
        return from + (beat - index) * (beatCell(index + 1) - from);
    }

    private float beatAt(float cell) {
        if (cell < 0) {
            return cell;
        }
        if (cell >= beatCells.peek()) {
            return nextBeat + cell - beatCells.peek();
        }
        int found = Arrays.binarySearch(beatCells.items, 0, beatCells.size, cell);
        int index = found >= 0 ? found : -found - 2;
        float from = beatCells.get(index);
        return index + (cell - from) / (beatCells.get(index + 1) - from);
    }

    private float beatCell(int beat) {
        if (beat < 0) {
            return beat;
        }
        if (beat > nextBeat) {
            return beatCells.peek() + beat - nextBeat;
        }
        return beatCells.get(beat);
    }

    /** Keeps the board filled one beat past the top edge, so tiles never pop in on screen. */
    private void spawn() {
        while (beatCells.peek() <= bottom() + visibleCells) {
            splitCredit += speed - 1;
            int rows = splitCredit >= 1 ? 2 : 1;
            for (Note note : chart.notes(nextBeat - scroll)) {
                tiles.add(new Tile(note.lane(), nextBeat + note.offset(), note.length()));
                if (note.offset() > 0) {
                    rows = 2;
                }
            }
            if (rows == 2) {
                // An off-beat tile can split a beat ahead of schedule. Owing for it would slow the board down after.
                splitCredit = Math.max(splitCredit - 1, 0);
            }
            // The ramp starts right away, before the beat comes into view.
            if (rows != lastRows) {
                ramps.add(new Ramp(nextBeat, nextBeat - scroll, rows - lastRows));
                lastRows = rows;
            }
            beatCells.add(beatCells.peek() + rows);
            nextBeat++;
        }
    }
}

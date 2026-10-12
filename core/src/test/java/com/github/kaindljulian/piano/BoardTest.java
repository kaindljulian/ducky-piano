package com.github.kaindljulian.piano;

import com.github.kaindljulian.piano.Board.Note;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    private Board board;

    @BeforeEach
    void startOnFirstLane() {
        board = new Board(4, 4, 1, beatsAhead -> new Note[]{new Note(0, 0, 0)});
        board.tap(0.5f, 0.5f, 0);
        board.update(0);
    }

    @Test
    void countsHitsInARow() {
        board.tap(0.5f, 1.5f, 0);
        board.tap(0.5f, 2.5f, 0);

        assertEquals(3, board.getCombo());
    }

    @Test
    void missedTileBreaksTheComboAndPlayContinues() {
        board.tap(0.5f, 1.5f, 0);
        board.update(3.5);

        assertEquals(0, board.getCombo());
        assertEquals(Board.State.RUNNING, board.getState());

        board.tap(0.5f, 0.2f, 0);
        assertEquals(1, board.getCombo());
    }

    @Test
    void tapOnAnEmptyCellBreaksTheCombo() {
        board.tap(1.5f, 1.5f, 0);

        assertEquals(0, board.getCombo());
        assertNotNull(board.getWrongTap());
    }

    @Test
    void anyTileCanBeHitAndSkippedOnesCountAsMissed() {
        board.tap(0.5f, 3.5f, 0);

        assertEquals(1, board.getCombo());
        assertTrue(board.getTiles().get(1).isMissed());
        assertTrue(board.getTiles().get(2).isMissed());

        board.update(3.5);
        assertEquals(1, board.getCombo());
    }

    @Test
    void splitBeatsScrollFasterWithTilesOneCellTall() {
        Board split = new Board(4, 4, 2, beatsAhead -> new Note[]{new Note(0, 0, 0), new Note(1, 0.5f, 0)});
        split.tap(0.5f, 0.5f, 0);
        split.tap(1.5f, 1.5f, 0);
        split.tap(0.5f, 2.5f, 0);

        assertEquals(3, split.getCombo());
        assertEquals(0.5f, split.getTiles().get(1).start);
    }

    @Test
    void beatsWithoutAnOffbeatTileLeaveAnEmptyRow() {
        Board mixed = alternatingSplitBeats(2);
        mixed.update(0.75);

        for (Tile tile : mixed.getTiles()) {
            assertEquals(2 * (tile.start - 0.75f), mixed.y(tile.start), 1e-5f);
        }
    }

    @Test
    void offbeatTilesGetARowEvenWithOneRowPerBeat() {
        Board mixed = alternatingSplitBeats(1);
        mixed.update(0.75);

        float first = mixed.y(mixed.getTiles().first().start);
        for (int i = 0; i < mixed.getTiles().size; i++) {
            assertEquals(first + i, mixed.y(mixed.getTiles().get(i).start), 1e-5f);
        }
    }

    @Test
    void wrongTapSnapsToTheRowGrid() {
        Board mixed = alternatingSplitBeats(2);
        mixed.tap(2.5f, 3.4f, 0);

        assertEquals(1.5f, mixed.getWrongTap().start);
    }

    @Test
    void holdingToTheEndCompletesAHoldWithAHitPerCell() {
        Board hold = holdOnSecondBeat();
        hold.tap(1.5f, 1.2f, 1);
        Tile tile = hold.getTiles().get(1);
        assertEquals(2, hold.getCombo());

        hold.update(1.9);

        assertFalse(tile.isHeld());
        assertFalse(tile.isMissed());
        assertEquals(2, tile.getPassed());
        assertEquals(3, hold.getCombo());
    }

    @Test
    void goingDownAboveTheFirstCellBreaksTheCombo() {
        Board hold = holdOnSecondBeat();
        hold.tap(1.5f, 2.2f, 1);

        Tile tile = hold.getTiles().get(1);
        assertTrue(tile.isMissed());
        assertFalse(tile.isHeld());
        assertEquals(0, hold.getCombo());
    }

    @Test
    void lettingGoBeforeTheLastCellBreaksTheCombo() {
        Board hold = holdOnSecondBeat();
        hold.tap(1.5f, 1.2f, 1);
        hold.update(0.5);
        hold.release(1);

        assertTrue(hold.getTiles().get(1).isMissed());
        assertEquals(0, hold.getCombo());
    }

    @Test
    void lettingGoInTheLastCellCompletesAHold() {
        Board hold = holdOnSecondBeat();
        hold.tap(1.5f, 1.2f, 1);
        hold.update(0.9);
        hold.release(1);

        assertFalse(hold.getTiles().get(1).isMissed());
        assertEquals(3, hold.getCombo());
    }

    @Test
    void anotherFingerCanTapWhileHolding() {
        Board hold = holdOnSecondBeat();
        hold.tap(1.5f, 1.2f, 1);
        hold.update(1);
        hold.tap(0.5f, 2.2f, 2);

        assertTrue(hold.getTiles().get(1).isHeld());
        assertEquals(4, hold.getCombo());
    }

    @Test
    void beatsCoveredByAHoldHaveNoTiles() {
        Board hold = holdOnSecondBeat();

        assertEquals(3f, hold.getTiles().get(2).start);
    }

    /** A split beat with an off-beat tile, then a plain one, and so on. Started at beat 0. */
    private static Board alternatingSplitBeats(int rowsPerBeat) {
        int[] next = {0};
        Board mixed = new Board(4, 4, rowsPerBeat, beatsAhead -> next[0]++ % 2 == 0
            ? new Note[]{new Note(0, 0, 0), new Note(1, 0.5f, 0)}
            : new Note[]{new Note(0, 0, 0)});
        mixed.tap(0.5f, 0.5f, 0);
        mixed.update(0);
        return mixed;
    }

    /** Lane 0 on every beat, except a two beat hold in lane 1 from beat 1. Started and scrolled to beat 0. */
    private static Board holdOnSecondBeat() {
        Note[][] beats = {{new Note(0, 0, 0)}, {new Note(1, 0, 2)}, {}};
        int[] next = {0};
        Board hold = new Board(4, 4, 1, beatsAhead -> next[0] < beats.length ? beats[next[0]++] : new Note[]{new Note(0, 0, 0)});
        hold.tap(0.5f, 0.5f, 0);
        hold.update(0);
        return hold;
    }
}

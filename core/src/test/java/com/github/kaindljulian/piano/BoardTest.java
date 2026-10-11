package com.github.kaindljulian.piano;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardTest {
    private Board board;

    @BeforeEach
    void startOnFirstLane() {
        board = new Board(4, 4, beatsAhead -> new int[]{0});
        board.tap(0.5f, 0.5f);
        board.update(0);
    }

    @Test
    void countsHitsInARow() {
        board.tap(0.5f, 1.5f);
        board.tap(0.5f, 2.5f);

        assertEquals(3, board.getCombo());
    }

    @Test
    void missedTileBreaksTheComboAndPlayContinues() {
        board.tap(0.5f, 1.5f);
        board.update(3.5);

        assertEquals(0, board.getCombo());
        assertEquals(Board.State.RUNNING, board.getState());

        board.tap(0.5f, 0.2f);
        assertEquals(1, board.getCombo());
    }

    @Test
    void tapOnAnEmptyCellBreaksTheCombo() {
        board.tap(1.5f, 1.5f);

        assertEquals(0, board.getCombo());
        assertNotNull(board.getWrongTap());
    }

    @Test
    void anyTileCanBeHitAndSkippedOnesCountAsMissed() {
        board.tap(0.5f, 3.5f);

        assertEquals(1, board.getCombo());
        assertTrue(board.getTiles().get(1).isMissed());
        assertTrue(board.getTiles().get(2).isMissed());

        board.update(3.5);
        assertEquals(1, board.getCombo());
    }

    @Test
    void splitBeatsScrollFasterWithTilesOneCellTall() {
        Board split = new Board(4, 4, beatsAhead -> new int[]{0, 1});
        split.setCellsPerBeat(2);
        split.tap(0.5f, 0.5f);
        split.tap(1.5f, 1.5f);
        split.tap(0.5f, 2.5f);

        assertEquals(3, split.getCombo());
        assertEquals(0.5f, split.getTiles().get(1).start);
    }

    @Test
    void wrongTapSnapsToTheBeatGrid() {
        board.setCellsPerBeat(1.5f);
        board.tap(2.5f, 1.4f);

        assertEquals(0.5f, board.getWrongTap().start);
    }
}

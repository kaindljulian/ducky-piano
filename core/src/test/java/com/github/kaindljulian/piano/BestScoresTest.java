package com.github.kaindljulian.piano;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BestScoresTest {
    @Test
    void keepsTheBestFirstAndTheEarlierOfATieAhead() {
        BestScores scores = new BestScores("");
        scores.add(1, 50);
        scores.add(2, 120);
        scores.add(3, 50);

        assertEquals(new BestScores.Score(2, 120), scores.list().get(0));
        assertEquals(new BestScores.Score(1, 50), scores.list().get(1));
        assertEquals(new BestScores.Score(3, 50), scores.list().get(2));
    }

    @Test
    void keepsOnlyTheBestHundred() {
        BestScores scores = new BestScores("");
        for (int combo = 1; combo <= BestScores.MAX + 20; combo++) {
            scores.add(combo, combo);
        }

        assertEquals(BestScores.MAX, scores.list().size());
        assertEquals(BestScores.MAX + 20, scores.list().get(0).combo());
        assertEquals(21, scores.list().get(BestScores.MAX - 1).combo());
        assertFalse(scores.add(0, 21));
        assertTrue(scores.add(0, 22));
    }

    @Test
    void readsBackWhatItSaves() {
        BestScores scores = new BestScores("");
        scores.add(1700000000000L, 300);
        scores.add(1700000001000L, 40);

        assertEquals(scores.list(), new BestScores(scores.encode()).list());
    }

    @Test
    void skipsUnreadableEntriesAndEmptyCombos() {
        assertEquals(1, new BestScores("x,5:0,7:12,1:2:3").list().size());
    }
}

package com.github.kaindljulian.piano;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The highest combos, best first. On a tie the earlier one stays ahead. Saved as text, see {@link #encode}. */
class BestScores {
    static final int MAX = 100;

    /** A combo of {@code combo} hits that ended at {@code time}, in milliseconds since the epoch. */
    record Score(long time, int combo) {
    }

    private final List<Score> scores = new ArrayList<>();

    /** Reads scores saved by {@link #encode}. Anything unreadable is left out. */
    BestScores(String saved) {
        for (String entry : saved.split(",")) {
            String[] parts = entry.split(":");
            if (parts.length != 2) continue;
            try {
                add(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    /** Whether the combo made the list. */
    boolean add(long time, int combo) {
        if (combo <= 0) return false;
        int index = 0;
        while (index < scores.size() && scores.get(index).combo() >= combo) {
            index++;
        }
        if (index >= MAX) return false;
        scores.add(index, new Score(time, combo));
        if (scores.size() > MAX) {
            scores.remove(MAX);
        }
        return true;
    }

    List<Score> list() {
        return Collections.unmodifiableList(scores);
    }

    /** Each score as {@code time:combo}, joined by commas. */
    String encode() {
        StringBuilder text = new StringBuilder();
        for (Score score : scores) {
            if (text.length() > 0) {
                text.append(',');
            }
            text.append(score.time()).append(':').append(score.combo());
        }
        return text.toString();
    }
}

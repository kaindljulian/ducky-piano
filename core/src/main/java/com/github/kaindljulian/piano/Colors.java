package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;

/** Every color of the game: mostly dark blue, with rubber duck yellow as the accent. */
final class Colors {
    static final Color BACKGROUND_TOP = new Color(0.07f, 0.09f, 0.2f, 1f);
    static final Color BACKGROUND_BOTTOM = new Color(0.02f, 0.02f, 0.05f, 1f);
    static final Color LANE_LINE = new Color(0.14f, 0.16f, 0.3f, 1f);

    static final Color TILE = new Color(0.2f, 0.25f, 0.45f, 1f);
    static final Color TAPPED = new Color(0.1f, 0.12f, 0.24f, 1f);
    static final Color MISSED = new Color(0.75f, 0.25f, 0.3f, 1f);
    static final Color WRONG_TAP = new Color(0.95f, 0.3f, 0.35f, 1f);

    static final Color HOLD = new Color(0.1f, 0.2f, 0.42f, 1f);
    static final Color HOLD_DONE = new Color(0.12f, 0.18f, 0.34f, 1f);
    static final Color WATER_DEEP = new Color(0.08f, 0.2f, 0.6f, 1f);
    /** The water's surface before the hold is tapped. */
    static final Color WATER_IDLE = new Color(0.2f, 0.38f, 0.75f, 1f);
    /** The water's surface while held. */
    static final Color WATER = new Color(0.25f, 0.55f, 1f, 1f);
    static final Color SHINE = new Color(0.7f, 0.9f, 1f, 0.35f);
    static final Color SHINE_FADE = new Color(0.7f, 0.9f, 1f, 0f);
    static final Color FOAM = new Color(0.9f, 0.97f, 1f, 0.85f);
    static final Color BUBBLE = new Color(0.8f, 0.93f, 1f, 1f);

    static final Color ACCENT = new Color(1f, 0.82f, 0.2f, 1f);
    static final Color ACCENT_DOWN = new Color(0.88f, 0.66f, 0.1f, 1f);
    /** Text on accent colored buttons. */
    static final Color ACCENT_TEXT = new Color(0.07f, 0.09f, 0.2f, 1f);
    static final Color BUTTON = new Color(0.12f, 0.16f, 0.34f, 1f);
    static final Color BUTTON_DOWN = new Color(0.18f, 0.24f, 0.5f, 1f);
    static final Color KNOB = new Color(Color.WHITE);
    static final Color OVERLAY = new Color(0.02f, 0.03f, 0.1f, 0.8f);
    static final Color TEXT = new Color(Color.WHITE);
    static final Color MUTED = new Color(0.5f, 0.58f, 0.8f, 1f);
    static final Color FOOTER = new Color(Color.LIGHT_GRAY);
    static final Color HEART = new Color(1f, 0.3f, 0.4f, 1f);
    /** The combo flashes this color every hundred hits. */
    static final Color MILESTONE = new Color(Color.WHITE);
    /** The paler of the two sparkle colors dropping from the combo, next to {@link #ACCENT}. */
    static final Color SPARKLE = new Color(1f, 0.96f, 0.75f, 1f);
    /** The combo turns this color at 500 hits... */
    static final Color COMBO_ORANGE = new Color(1f, 0.55f, 0.1f, 1f);
    /** ...and this one at 900. */
    static final Color COMBO_RED = new Color(1f, 0.18f, 0.12f, 1f);
    /** The hot core of the flames on a burning combo. */
    static final Color FLAME = new Color(1f, 0.9f, 0.45f, 1f);

    static final Color DUCK = new Color(ACCENT);
    static final Color DUCK_SHADE = new Color(0.96f, 0.68f, 0.12f, 1f);
    static final Color DUCK_BEAK = new Color(1f, 0.5f, 0.1f, 1f);
    static final Color DUCK_EYE = new Color(0.07f, 0.09f, 0.2f, 1f);
    static final Color DUCK_SHINE = new Color(1f, 1f, 1f, 0.7f);
    static final Color ICON_BACKGROUND = new Color(0.12f, 0.17f, 0.38f, 1f);

    static final Color RAIN_TILE = new Color(TILE).mul(1, 1, 1, 0.7f);
    static final Color RAIN_HOLD = new Color(WATER).mul(1, 1, 1, 0.35f);

    private Colors() {
    }
}

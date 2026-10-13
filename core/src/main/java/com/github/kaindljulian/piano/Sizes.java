package com.github.kaindljulian.piano;

/**
 * Every size of the game. Menu sizes are in virtual pixels of the {@link #UI_WIDTH} by {@link #UI_HEIGHT} layout,
 * text scales are relative to {@link #FONT_SIZE}, and board sizes are in cells up and lanes across.
 */
final class Sizes {
    /** The viewport stretches this layout to fill the screen. */
    static final float UI_WIDTH = 360;
    static final float UI_HEIGHT = 640;
    /** Text height. */
    static final float FONT_SIZE = 28;
    static final float TEXT_LARGE = 2;
    static final float TEXT_TITLE = 1.7f;
    static final float TEXT_NORMAL = 1;
    static final float TEXT_SMALL = 0.7f;
    static final float TEXT_TINY = 0.6f;

    static final float MENU_BUTTON_WIDTH = 240;
    static final float PAUSE_MENU_BUTTON_WIDTH = 220;
    static final float BUTTON_HEIGHT = 60;
    static final float PAUSE_BUTTON = 56;
    static final float SLIDER_WIDTH = 160;
    static final float SLIDER_VALUE_WIDTH = 44;
    static final float TRACK_HEIGHT = 8;
    static final float KNOB = 26;
    static final float KNOB_DOWN = 30;
    static final float COMBO_WIDTH = 240;
    static final float COMBO_HEIGHT = 90;
    static final float SPARKLE_MIN = 3;
    static final float SPARKLE_MAX = 7;
    static final float FLAME_MIN = 8;
    static final float FLAME_MAX = 16;
    /** The ducks dropping from the combo. */
    static final float COMBO_DUCK = 26;
    static final float HEART = 14;
    static final float LOGO = 120;
    /** How far the logo bobs up and down. */
    static final float LOGO_BOB = 6;

    /** Space around each tile, in cells. */
    static final float TILE_GAP = 0;
    /** In cells. */
    static final float WAVE_HEIGHT = 0.035f;
    /** In cells. */
    static final float IDLE_WAVE_HEIGHT = 0.02f;
    /** How much higher the waves get when the finger is dragged into the next cell, in cells. */
    static final float SURGE_HEIGHT = 0.04f;
    /** In cells. */
    static final float SHINE_HEIGHT = 0.12f;
    /** In cells. */
    static final float FOAM_HEIGHT = 0.018f;
    /** In lanes. */
    static final float BUBBLE_MIN_RADIUS = 0.02f;
    /** In lanes. */
    static final float BUBBLE_MAX_RADIUS = 0.035f;
    /** Half the width of the duck's body, in lanes. */
    static final float DUCK = 0.11f;

    private Sizes() {
    }
}

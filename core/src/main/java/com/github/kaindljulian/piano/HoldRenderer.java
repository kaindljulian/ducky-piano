package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;

/**
 * Draws an open hold tile as a blue tile with a pool of water in its first cell that swells on the beat. While held,
 * the water fills the tile, reaching the top as the finger gets to the middle of the last cell, where it may let go.
 * It calms down as it rises and surges each time the finger is dragged into the next cell. Long holds have a rubber
 * duck on it.
 */
class HoldRenderer {
    private static final int WAVE_STRIPS = 24;
    /** How many times the waves go through their cycle per beat. */
    private static final float WAVE_SPEED = 0.25f;
    /** How many cells the finger moves on while a surge settles. */
    private static final float SURGE_CELLS = 0.75f;
    private static final int BUBBLES = 4;
    /** How many times a bubble rises to the surface per beat. */
    private static final float BUBBLE_SPEED = 0.3f;
    /** Holds at least this many cells tall get a rubber duck. */
    private static final int DUCK_CELLS = 5;

    private final ShapeRenderer shapes;
    private final Color color = new Color();
    /** The width of a lane on screen, in cells. Round shapes are scaled by it to stay round. */
    private float laneWidth;

    HoldRenderer(ShapeRenderer shapes) {
        this.shapes = shapes;
    }

    /** Draws a hold tile {@code height} cells tall, {@code y} cells above the bottom, into a filled shape batch. */
    void draw(Board board, Tile tile, float y, float height, float laneWidth) {
        this.laneWidth = laneWidth;
        shapes.setColor(Colors.HOLD);
        shapes.rect(tile.lane, y, 1, height);
        float phase = board.getScroll() * WAVE_SPEED * MathUtils.PI2;
        float level;
        float amplitude;
        Color surface;
        if (tile.isHeld()) {
            float travel = tile.getHeldAt() - y - tile.getGrabbedAt();
            float topTravel = height - 0.5f - tile.getGrabbedAt();
            float progress = topTravel > 0 ? MathUtils.clamp(travel / topTravel, 0, 1) : 1;
            level = MathUtils.lerp(0.5f, height, Interpolation.sineOut.apply(progress));
            amplitude = Sizes.WAVE_HEIGHT * (1 - progress) + Sizes.SURGE_HEIGHT * surge(tile, y);
            surface = Colors.WATER;
        } else {
            float pulse = 1 - (board.getScroll() - MathUtils.floor(board.getScroll()));
            level = 0.5f;
            amplitude = Sizes.IDLE_WAVE_HEIGHT * (0.6f + 0.4f * pulse);
            surface = Colors.WATER_IDLE;
        }
        drawWater(tile.lane, y, height, level, amplitude, phase, surface);
        drawBubbles(tile, y, level, board.getScroll());
        if (Math.round(height) >= DUCK_CELLS) {
            float x = 0.45f + 0.12f * MathUtils.sin(0.37f * phase);
            float top = 1.45f * Sizes.DUCK * laneWidth;
            float waterline = Math.min(surface(x, height, level, amplitude, phase), height - top);
            drawDuck(tile.lane + x, y + waterline, surface);
        }
    }

    /** 1 as the finger enters a cell after the first, fading to 0 as it moves on. */
    private static float surge(Tile tile, float y) {
        if (tile.getPassed() < 2) return 0;
        return Math.max(1 - (tile.getHeldAt() - y - (tile.getPassed() - 1)) / SURGE_CELLS, 0);
    }

    /** Fills a lane from {@code y} up to a gently rolling surface around {@code level} cells above it. */
    private void drawWater(int lane, float y, float height, float level, float amplitude, float phase,
                           Color surface) {
        float previous = y + surface(0, height, level, amplitude, phase);
        for (int strip = 1; strip <= WAVE_STRIPS; strip++) {
            float x0 = lane + (float) (strip - 1) / WAVE_STRIPS;
            float x1 = lane + (float) strip / WAVE_STRIPS;
            float top = y + surface((float) strip / WAVE_STRIPS, height, level, amplitude, phase);
            drawStrip(x0, x1, y, y, previous, top, Colors.WATER_DEEP, surface);
            drawStrip(x0, x1, Math.max(previous - Sizes.SHINE_HEIGHT, y), Math.max(top - Sizes.SHINE_HEIGHT, y), previous, top,
                Colors.SHINE_FADE, Colors.SHINE);
            drawStrip(x0, x1, Math.max(previous - Sizes.FOAM_HEIGHT, y), Math.max(top - Sizes.FOAM_HEIGHT, y), previous, top,
                Colors.FOAM, Colors.FOAM);
            previous = top;
        }
    }

    /**
     * How many cells above the tile's start the water is at {@code x} lanes across, kept inside the tile. A long,
     * low swell with a slow slosh from side to side, so it reads as water in a glass.
     */
    private static float surface(float x, float height, float level, float amplitude, float phase) {
        float wave = 0.8f * MathUtils.sin(MathUtils.PI2 * 0.55f * x + phase)
            + 0.2f * MathUtils.sin(MathUtils.PI2 * 1.3f * x - 0.8f * phase + 1.7f)
            + 0.6f * (x - 0.5f) * MathUtils.sin(0.5f * phase);
        return MathUtils.clamp(level + amplitude * wave, 0, height);
    }

    /** A few faint bubbles rising from the bottom, placed by the tile's start so each tile has its own. */
    private void drawBubbles(Tile tile, float y, float level, float scroll) {
        for (int i = 0; i < BUBBLES; i++) {
            float seed = fraction(tile.start * 7.31f + i * 0.618f);
            float rise = fraction(scroll * BUBBLE_SPEED + seed);
            float x = tile.lane + 0.15f + 0.7f * fraction(seed * 3.7f) + 0.03f * MathUtils.sin(10 * rise + i);
            float radius = MathUtils.lerp(Sizes.BUBBLE_MIN_RADIUS, Sizes.BUBBLE_MAX_RADIUS, fraction(seed * 5.3f));
            drawCircle(x, y + rise * (level - 0.05f), radius, color.set(Colors.BUBBLE).mul(1, 1, 1, 0.35f * (1 - rise)));
        }
    }

    /** A rubber duck facing right, sitting in the water up to {@code waterline}. */
    private void drawDuck(float x, float waterline, Color water) {
        float s = Sizes.DUCK;
        float h = Sizes.DUCK * laneWidth;
        shapes.setColor(Colors.DUCK);
        shapes.triangle(x - s, waterline + 0.25f * h, x - 1.35f * s, waterline + 0.75f * h,
            x - 0.6f * s, waterline + 0.55f * h);
        shapes.ellipse(x - 1.1f * s, waterline - 0.45f * h, 2.2f * s, 1.2f * h, 24);
        float headX = x + 0.55f * s;
        float headY = waterline + 0.95f * h;
        drawCircle(headX, headY, 0.5f * s, Colors.DUCK);
        shapes.setColor(Colors.DUCK_BEAK);
        shapes.triangle(headX + 0.35f * s, headY + 0.05f * h, headX + 0.35f * s, headY - 0.2f * h,
            headX + 0.85f * s, headY - 0.1f * h);
        drawCircle(headX + 0.15f * s, headY + 0.15f * h, 0.08f * s, Colors.DUCK_EYE);

        shapes.setColor(color.set(water).mul(1, 1, 1, 0.7f));
        shapes.rect(x - 1.15f * s, waterline - 0.45f * h, 2.3f * s, 0.45f * h);
        shapes.setColor(Colors.FOAM);
        shapes.rect(x - 1.15f * s, waterline - Sizes.FOAM_HEIGHT, 2.3f * s, Sizes.FOAM_HEIGHT);
    }

    private static float fraction(float value) {
        return value - MathUtils.floor(value);
    }

    private void drawStrip(float x0, float x1, float bottom0, float bottom1, float top0, float top1, Color low,
                           Color high) {
        shapes.triangle(x0, bottom0, x1, bottom1, x1, top1, low, low, high);
        shapes.triangle(x0, bottom0, x1, top1, x0, top0, low, high, high);
    }

    /** {@code radius} is in lanes. */
    private void drawCircle(float x, float y, float radius, Color color) {
        float height = radius * laneWidth;
        shapes.setColor(color);
        shapes.ellipse(x - radius, y - height, 2 * radius, 2 * height, 16);
    }
}

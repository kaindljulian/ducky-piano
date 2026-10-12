package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Tiles falling down four lanes, as a backdrop for menus. */
class TileRain {
    private static final int LANES = 4;
    private static final int VISIBLE_CELLS = 4;
    private static final float CELLS_PER_SECOND = 1.2f;
    private static final float HOLD_CHANCE = 0.2f;

    private record Drop(int lane, float start, float length, Color color) {
    }

    private final Array<Drop> drops = new Array<>();
    private float scroll;
    private float nextStart;
    private int lastLane = -1;

    void update(float delta) {
        scroll += delta * CELLS_PER_SECOND;
        while (drops.notEmpty() && drops.first().start + drops.first().length < scroll) {
            drops.removeIndex(0);
        }
        while (nextStart < scroll + VISIBLE_CELLS + 1) {
            int lane = MathUtils.random(LANES - 2);
            if (lane >= lastLane) {
                lane++;
            }
            boolean hold = MathUtils.random() < HOLD_CHANCE;
            float length = hold ? MathUtils.random(2, 3) : 1;
            drops.add(new Drop(lane, nextStart, length, hold ? Colors.RAIN_HOLD : Colors.RAIN_TILE));
            lastLane = lane;
            nextStart += length + MathUtils.random(0, 1) * 0.5f;
        }
    }

    /** Fills a {@code width} by {@code height} area. Needs blending on and the shapes begun as filled. */
    void draw(ShapeRenderer shapes, float width, float height) {
        shapes.rect(0, 0, width, height, Colors.BACKGROUND_BOTTOM, Colors.BACKGROUND_BOTTOM, Colors.BACKGROUND_TOP,
            Colors.BACKGROUND_TOP);
        float laneWidth = width / LANES;
        float cellHeight = height / VISIBLE_CELLS;
        for (Drop drop : drops) {
            shapes.setColor(drop.color);
            shapes.rect(drop.lane * laneWidth, (drop.start - scroll) * cellHeight, laneWidth, drop.length * cellHeight);
        }
    }
}

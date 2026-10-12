package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Draws a {@link Board} with plain shapes. */
public class BoardRenderer implements Disposable {
    private static final float GAP = 0f;
    private static final Color TAPPED = new Color(0.1f, 0.12f, 0.24f, 1f);
    private static final Color MISSED = new Color(0.75f, 0.25f, 0.3f, 1f);
    private static final Color WRONG = new Color(0.95f, 0.3f, 0.35f, 1f);
    private static final Color LINE = new Color(0.14f, 0.16f, 0.3f, 1f);
    private static final Color HOLD_BODY = new Color(0.1f, 0.2f, 0.42f, 1f);
    private static final Color RAIL = new Color(0.35f, 0.5f, 0.85f, 1f);
    private static final Color ACCENT_DEEP = new Color(0.08f, 0.2f, 0.6f, 1f);
    private static final Color GLOW = new Color(0.55f, 0.8f, 1f, 1f);
    private static final Color DONE = new Color(0.12f, 0.18f, 0.34f, 1f);
    private static final Color DONE_RAIL = new Color(0.3f, 0.42f, 0.7f, 1f);
    private static final Color MISSED_RAIL = new Color(0.95f, 0.5f, 0.55f, 1f);
    /** In lanes. */
    private static final float RAIL_WIDTH = 0.08f;
    /** In lanes. */
    private static final float THUMB_RADIUS = 0.2f;
    /** In cells. */
    private static final float SHEEN_HEIGHT = 0.2f;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final Color glow = new Color();
    /** The width of a lane on screen, in cells. Circles are scaled by it to stay round. */
    private float laneWidth;

    public void render(Board board, Viewport viewport) {
        ScreenUtils.clear(Theme.BACKGROUND_BOTTOM);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        laneWidth = (float) viewport.getScreenWidth() / board.lanes
            / ((float) viewport.getScreenHeight() / board.visibleCells);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, board.lanes, board.visibleCells, Theme.BACKGROUND_BOTTOM, Theme.BACKGROUND_BOTTOM,
            Theme.BACKGROUND_TOP, Theme.BACKGROUND_TOP);
        for (Tile tile : board.getTiles()) {
            if (tile.isHold()) {
                drawHold(board, tile);
            } else {
                drawTile(board, tile, tile.isTapped() ? TAPPED : tile.isMissed() ? MISSED : Theme.TILE);
            }
        }
        if (board.getWrongTap() != null) {
            drawTile(board, board.getWrongTap(), WRONG);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(LINE);
        for (int lane = 1; lane < board.lanes; lane++) {
            shapes.line(lane, 0, lane, board.visibleCells);
        }
        shapes.end();
    }

    private void drawTile(Board board, Tile tile, Color color) {
        drawRect(tile.lane, y(board, tile), 1, color);
    }

    /**
     * A blue tile with a rail up the middle and a thumb at the start that pulses on the beat. While held, it fills
     * up to the finger and the thumb follows it.
     */
    private void drawHold(Board board, Tile tile) {
        float y = y(board, tile);
        float height = tile.length * board.getCellsPerBeat();
        if (tile.isMissed() || tile.isTapped() && !tile.isHeld()) {
            drawRect(tile.lane, y, height, tile.isMissed() ? MISSED : DONE);
            drawRail(tile.lane, y, height, tile.isMissed() ? MISSED_RAIL : DONE_RAIL);
            return;
        }

        float beat = board.getScroll() - MathUtils.floor(board.getScroll());
        float pulse = 1 - beat;
        drawRect(tile.lane, y, height, HOLD_BODY);
        float thumbY;
        if (tile.isHeld()) {
            float filled = MathUtils.clamp(tile.getHeldAt() * board.getCellsPerBeat() - y, 0, height);
            shapes.rect(tile.lane, y, 1, filled, ACCENT_DEEP, ACCENT_DEEP, Theme.ACCENT, Theme.ACCENT);
            float sheenHeight = Math.min(SHEEN_HEIGHT, filled);
            shapes.setColor(glow.set(GLOW).mul(1, 1, 1, 0.5f * pulse));
            shapes.rect(tile.lane, y + (filled - sheenHeight) * beat, 1, sheenHeight);
            drawRail(tile.lane, y, filled, Color.WHITE);
            drawRail(tile.lane, y + filled, height - filled, RAIL);
            thumbY = y + filled;
        } else {
            drawRail(tile.lane, y, height, RAIL);
            thumbY = y + 0.5f;
        }

        float radius = THUMB_RADIUS * (tile.isHeld() ? 1.2f : 1);
        thumbY = MathUtils.clamp(thumbY, y + radius * laneWidth, y + height - radius * laneWidth);
        float center = tile.lane + 0.5f;
        drawCircle(center, thumbY, radius * (1.3f + 0.7f * beat), glow.set(GLOW).mul(1, 1, 1, 0.6f * pulse));
        drawCircle(center, thumbY, radius * (1 + 0.12f * pulse), Theme.ACCENT);
        if (tile.isHeld()) {
            drawCircle(center, thumbY, radius * 0.55f, Color.WHITE);
        }
    }

    private void drawRect(int lane, float y, float height, Color color) {
        shapes.setColor(color);
        shapes.rect(lane + GAP, y + GAP, 1 - 2 * GAP, height - 2 * GAP);
    }

    private void drawRail(int lane, float y, float height, Color color) {
        shapes.setColor(color);
        shapes.rect(lane + 0.5f - RAIL_WIDTH / 2, y, RAIL_WIDTH, height);
    }

    /** {@code radius} is in lanes. */
    private void drawCircle(float x, float y, float radius, Color color) {
        float height = radius * laneWidth;
        shapes.setColor(color);
        shapes.ellipse(x - radius, y - height, 2 * radius, 2 * height, 32);
    }

    private static float y(Board board, Tile tile) {
        return (tile.start - board.getScroll()) * board.getCellsPerBeat();
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}

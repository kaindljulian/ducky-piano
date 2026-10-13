package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Draws a {@link Board} with plain shapes. On a rainbow combo every tile takes a color of the rainbow. */
public class BoardRenderer implements Disposable {
    /** In degrees around the color wheel per beat, so tiles next to each other differ. */
    private static final float TILE_HUE_STEP = 50;

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final HoldRenderer holds = new HoldRenderer(shapes);
    private final Color color = new Color();
    /** The width of a lane on screen, in cells. */
    private float laneWidth;

    public void render(Board board, Viewport viewport, boolean rainbow) {
        ScreenUtils.clear(Colors.BACKGROUND_BOTTOM);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        laneWidth = (float) viewport.getScreenWidth() / board.lanes
            / ((float) viewport.getScreenHeight() / viewport.getWorldHeight());

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, board.lanes, viewport.getWorldHeight(), Colors.BACKGROUND_BOTTOM, Colors.BACKGROUND_BOTTOM,
            Colors.BACKGROUND_TOP, Colors.BACKGROUND_TOP);
        for (Tile tile : board.getTiles()) {
            if (tile.isHold()) {
                drawHold(board, tile, rainbow);
            } else if (tile.isMissed()) {
                drawTile(board, tile, Colors.MISSED);
            } else if (rainbow) {
                drawTile(board, tile, Colors.rainbow(color, hue(tile), 0.75f, tile.isTapped() ? 0.35f : 0.9f));
            } else {
                drawTile(board, tile, tile.isTapped() ? Colors.TAPPED : Colors.TILE);
            }
        }
        if (board.getWrongTap() != null) {
            drawTile(board, board.getWrongTap(), Colors.WRONG_TAP);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Colors.LANE_LINE);
        for (int lane = 1; lane < board.lanes; lane++) {
            shapes.line(lane, 0, lane, viewport.getWorldHeight());
        }
        shapes.end();
    }

    private static float hue(Tile tile) {
        return tile.start * TILE_HUE_STEP;
    }

    private void drawTile(Board board, Tile tile, Color color) {
        drawRect(tile.lane, board.y(tile.start), 1, color);
    }

    /** An open hold is left to {@link HoldRenderer}. Once tapped through or missed, it's a plain tile. */
    private void drawHold(Board board, Tile tile, boolean rainbow) {
        float y = board.y(tile.start);
        float height = board.y(tile.start + tile.length) - y;
        if (tile.isMissed()) {
            drawRect(tile.lane, y, height, Colors.MISSED);
        } else if (tile.isTapped() && !tile.isHeld()) {
            drawRect(tile.lane, y, height,
                rainbow ? Colors.rainbow(color, hue(tile), 0.75f, 0.4f) : Colors.HOLD_DONE);
        } else {
            holds.draw(board, tile, y, height, laneWidth, rainbow ? hue(tile) : -1);
        }
    }

    private void drawRect(int lane, float y, float height, Color color) {
        shapes.setColor(color);
        shapes.rect(lane + Sizes.TILE_GAP, y + Sizes.TILE_GAP, 1 - 2 * Sizes.TILE_GAP, height - 2 * Sizes.TILE_GAP);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}

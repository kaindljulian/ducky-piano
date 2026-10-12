package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Draws a {@link Board} with plain shapes. */
public class BoardRenderer implements Disposable {
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final HoldRenderer holds = new HoldRenderer(shapes);
    /** The width of a lane on screen, in cells. */
    private float laneWidth;

    public void render(Board board, Viewport viewport) {
        ScreenUtils.clear(Colors.BACKGROUND_BOTTOM);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        laneWidth = (float) viewport.getScreenWidth() / board.lanes
            / ((float) viewport.getScreenHeight() / board.visibleCells);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, board.lanes, board.visibleCells, Colors.BACKGROUND_BOTTOM, Colors.BACKGROUND_BOTTOM,
            Colors.BACKGROUND_TOP, Colors.BACKGROUND_TOP);
        for (Tile tile : board.getTiles()) {
            if (tile.isHold()) {
                drawHold(board, tile);
            } else {
                drawTile(board, tile, tile.isTapped() ? Colors.TAPPED : tile.isMissed() ? Colors.MISSED : Colors.TILE);
            }
        }
        if (board.getWrongTap() != null) {
            drawTile(board, board.getWrongTap(), Colors.WRONG_TAP);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Colors.LANE_LINE);
        for (int lane = 1; lane < board.lanes; lane++) {
            shapes.line(lane, 0, lane, board.visibleCells);
        }
        shapes.end();
    }

    private void drawTile(Board board, Tile tile, Color color) {
        drawRect(tile.lane, board.y(tile.start), 1, color);
    }

    /** An open hold is left to {@link HoldRenderer}. Once tapped through or missed, it's a plain tile. */
    private void drawHold(Board board, Tile tile) {
        float y = board.y(tile.start);
        float height = board.y(tile.start + tile.length) - y;
        if (tile.isMissed() || tile.isTapped() && !tile.isHeld()) {
            drawRect(tile.lane, y, height, tile.isMissed() ? Colors.MISSED : Colors.HOLD_DONE);
        } else {
            holds.draw(board, tile, y, height, laneWidth);
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

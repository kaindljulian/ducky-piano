package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Draws a {@link Board} with plain shapes. */
public class BoardRenderer implements Disposable {
    private static final float GAP = 0f;
    private static final Color TAPPED = new Color(0.85f, 0.85f, 0.85f, 1f);
    private static final Color LINE = new Color(0.7f, 0.7f, 0.7f, 1f);

    private final ShapeRenderer shapes = new ShapeRenderer();

    public void render(Board board, Viewport viewport) {
        ScreenUtils.clear(Color.WHITE);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Tile tile : board.getTiles()) {
            drawTile(board, tile, tile.isTapped() ? TAPPED : Color.BLACK);
        }
        if (board.getFailed() != null) {
            drawTile(board, board.getFailed(), Color.RED);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(LINE);
        for (int lane = 1; lane < board.lanes; lane++) {
            shapes.line(lane, 0, lane, board.visibleRows);
        }
        shapes.end();
    }

    private void drawTile(Board board, Tile tile, Color color) {
        shapes.setColor(color);
        float y = tile.row - board.getScroll();
        shapes.rect(tile.lane + GAP, y + GAP, 1 - 2 * GAP, 1 - 2 * GAP);
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}

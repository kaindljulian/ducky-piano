package com.github.kaindljulian.piano;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private static final int LANES = 4;
    private static final int VISIBLE_ROWS = 4;
    private static final float ROWS_PER_SECOND = 3f;

    private final Vector2 touch = new Vector2();
    private Board board;
    private BoardRenderer renderer;
    private Viewport viewport;

    @Override
    public void create() {
        board = newBoard();
        renderer = new BoardRenderer();
        viewport = new StretchViewport(LANES, VISIBLE_ROWS);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (board.getState() == Board.State.OVER) {
                    board = newBoard();
                    return true;
                }
                viewport.unproject(touch.set(screenX, screenY));
                board.tap(touch.x, touch.y);
                return true;
            }
        });
    }

    private static Board newBoard() {
        return Board.random(LANES, VISIBLE_ROWS, ROWS_PER_SECOND);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        board.update(Gdx.graphics.getDeltaTime());
        renderer.render(board, viewport);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}

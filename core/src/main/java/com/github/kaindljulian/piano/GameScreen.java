package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private static final int LANES = 4;
    private static final int VISIBLE_ROWS = 4;
    private static final float ROWS_PER_SECOND = 3f;

    private final Main game;
    private final Viewport boardViewport = new StretchViewport(LANES, VISIBLE_ROWS);
    private final BoardRenderer renderer = new BoardRenderer();
    private final Stage stage = Ui.createStage();
    private final TextButton pauseButton;
    private final Table menu = new Table();
    private final Vector2 touch = new Vector2();
    private Board board = newBoard();

    GameScreen(Main game) {
        this.game = game;
        Skin skin = game.skin;

        pauseButton = new TextButton("II", skin);
        Ui.onClick(pauseButton, () -> openMenu("Paused", true));
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(8);
        hud.add(pauseButton).size(56);
        stage.addActor(hud);

        menu.setFillParent(true);
        menu.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.7f)));
        menu.setVisible(false);
        stage.addActor(menu);
    }

    private static Board newBoard() {
        return Board.random(LANES, VISIBLE_ROWS, ROWS_PER_SECOND);
    }

    private void openMenu(String title, boolean resumable) {
        Skin skin = game.skin;
        menu.clear();
        menu.add(new Label(title, skin)).padBottom(48).row();
        if (resumable) addMenuButton("Resume", this::closeMenu);
        addMenuButton("Restart", () -> {
            board = newBoard();
            closeMenu();
        });
        addMenuButton("Main menu", () -> game.switchTo(new StartScreen(game)));
        menu.setVisible(true);
        pauseButton.setVisible(false);
    }

    private void addMenuButton(String text, Runnable action) {
        TextButton button = new TextButton(text, game.skin);
        Ui.onClick(button, action);
        menu.add(button).size(220, 60).padBottom(16).row();
    }

    private void closeMenu() {
        menu.setVisible(false);
        pauseButton.setVisible(true);
    }

    @Override
    public void show() {
        // The stage goes first so taps on its buttons never reach the board.
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (menu.isVisible()) return false;
                boardViewport.unproject(touch.set(screenX, screenY));
                board.tap(touch.x, touch.y);
                return true;
            }
        }));
    }

    @Override
    public void render(float delta) {
        if (!menu.isVisible()) {
            board.update(delta);
            if (board.getState() == Board.State.OVER) openMenu("Game over", false);
        }
        renderer.render(board, boardViewport);

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        boardViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    /**
     * Called when the app loses focus, e.g. when the player switches apps on Android.
     */
    @Override
    public void pause() {
        if (!menu.isVisible()) openMenu("Paused", true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        stage.dispose();
    }
}

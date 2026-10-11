package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;

public class StartScreen extends ScreenAdapter {
    private final Stage stage = Ui.createStage();

    StartScreen(Main game) {
        TextButton play = new TextButton("Play", game.skin);
        Ui.onClick(play, () -> game.switchTo(new GameScreen(game)));

        Table root = new Table();
        root.setFillParent(true);
        root.add(new Label("Piano Tiles", game.skin)).padBottom(48).row();
        root.add(play).size(200, 60).row();
        if (game.selectAudioSource != null) {
            TextButton source = new TextButton("Select audio source", game.skin);
            Ui.onClick(source, game.selectAudioSource);
            root.add(source).size(280, 60).padTop(16);
        }
        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}

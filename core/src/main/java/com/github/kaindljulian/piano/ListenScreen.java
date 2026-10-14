package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Explains why the game listens to other apps before the system asks, and what to do if the player says no.
 */
public class ListenScreen extends ScreenAdapter {
    private static final String EXPLANATION =
            """
                    Ducky Piano makes its tiles from music playing in another app, like your music player.
                    
                    Android will ask to record audio and to share your screen. The game only uses the sound. \
                    It stays on your phone and is never saved or sent.""";
    private static final String DENIED =
            """
                    Without both permissions there's no music to play along to.
                    
                    If Android doesn't ask again, allow the microphone for Ducky Piano in your phone's settings.""";
    private static final String PRIVACY_POLICY = "https://kaindljulian.github.io/ducky-piano/privacy.html";

    private final Stage stage = Ui.createStage();

    ListenScreen(Main game) {
        Skin skin = game.skin;
        Label message = Ui.label(skin, EXPLANATION, Colors.TEXT, Sizes.TEXT_SMALL);
        message.setWrap(true);
        message.setAlignment(Align.center);

        TextButton allow = new TextButton("Continue", skin, "accent");
        Ui.onClick(allow, () -> game.audioSource.request(allowed -> Gdx.app.postRunnable(() -> {
            game.listening = allowed;
            if (allowed) {
                game.switchTo(new GameScreen(game));
            } else {
                message.setText(DENIED);
                allow.setText("Try again");
            }
        })));
        Label privacy = Ui.label(skin, "Privacy policy", Colors.ACCENT, Sizes.TEXT_SMALL);
        privacy.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.net.openURI(PRIVACY_POLICY);
            }
        });
        TextButton back = new TextButton("Back", skin);
        Ui.onClick(back, () -> game.switchTo(new StartScreen(game)));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(32, 24, 24, 24);
        root.add(Ui.label(skin, "Audio Source", Colors.TEXT, Sizes.TEXT_TITLE)).padBottom(24).row();
        root.add(message).width(Sizes.UI_WIDTH - 48).expandY().row();
        root.add(privacy).padBottom(24).row();
        root.add(allow).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).row();
        root.add(back).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).padTop(16);
        stage.addActor(root);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Colors.BACKGROUND_TOP);
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

package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public class StartScreen extends ScreenAdapter {
    private static final int HEART_PIXELS = 64;
    /** How long the logo takes to bob up, and again to bob down. */
    private static final float LOGO_BOB_SECONDS = 1.4f;

    private final Stage stage = Ui.createStage();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final TileRain rain = new TileRain();
    private final Blur blur = new Blur();
    private final Texture heart = Ui.texture(HEART_PIXELS, StartScreen::inHeart);

    StartScreen(Main game) {
        Stack logo = Ui.duck(game.skin);
        logo.addAction(Actions.forever(Actions.sequence(
            Actions.moveBy(0, Sizes.LOGO_BOB, LOGO_BOB_SECONDS, Interpolation.sine),
            Actions.moveBy(0, -Sizes.LOGO_BOB, LOGO_BOB_SECONDS, Interpolation.sine))));

        Table title = new Table();
        title.add(Ui.label(game.skin, "Ducky", Colors.ACCENT, Sizes.TEXT_TITLE)).padRight(12);
        title.add(Ui.label(game.skin, "Piano", Colors.TEXT, Sizes.TEXT_TITLE));

        TextButton play = new TextButton("Play", game.skin, "accent");
        Ui.onClick(play, () -> game.switchTo(new GameScreen(game)));

        Table root = new Table();
        root.setFillParent(true);
        root.add(logo).size(Sizes.LOGO).row();
        root.add(title).padBottom(48).row();
        root.add(play).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).row();
        if (game.selectAudioSource != null) {
            TextButton source = new TextButton("Audio Source", game.skin);
            Ui.onClick(source, game.selectAudioSource);
            root.add(source).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).padTop(16);
        }
        stage.addActor(root);

        Image heartImage = new Image(heart);
        heartImage.setColor(Colors.HEART);
        Table footer = new Table();
        footer.setFillParent(true);
        footer.bottom().padBottom(20);
        footer.add(Ui.label(game.skin, "made with", Colors.FOOTER, Sizes.TEXT_TINY)).padRight(6);
        footer.add(heartImage).size(Sizes.HEART);
        stage.addActor(footer);
    }

    /** The font has no heart glyph, so this draws one from the heart curve. */
    private static boolean inHeart(float x, float y) {
        x *= 1.3f;
        y = 1.3f * y + 0.1f;
        float a = x * x + y * y - 1;
        return a * a * a - x * x * y * y * y <= 0;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        rain.update(delta);
        blur.begin();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.setProjectionMatrix(stage.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        rain.draw(shapes, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        shapes.end();
        blur.end();
        blur.draw();

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        blur.resize(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
        shapes.dispose();
        blur.dispose();
        heart.dispose();
    }
}

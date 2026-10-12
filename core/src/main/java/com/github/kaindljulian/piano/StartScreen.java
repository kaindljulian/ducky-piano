package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public class StartScreen extends ScreenAdapter {
    private static final Color ACCENT_DOWN = new Color(0.15f, 0.38f, 0.8f, 1f);
    private static final Color HEART = new Color(1f, 0.3f, 0.4f, 1f);
    private static final float BUTTON_WIDTH = 240;
    private static final float BUTTON_HEIGHT = 60;
    private static final int HEART_PIXELS = 64;

    private final Stage stage = Ui.createStage();
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final TileRain rain = new TileRain();
    private final Blur blur = new Blur();
    private final Texture heart = heartTexture();

    StartScreen(Main game) {
        BitmapFont font = game.skin.getFont("default");
        Label piano = new Label("Piano", new Label.LabelStyle(font, Color.WHITE));
        Label number = new Label("106", new Label.LabelStyle(font, Theme.ACCENT));
        piano.setFontScale(font.getScaleX() * 2);
        number.setFontScale(font.getScaleX() * 2);
        Table title = new Table();
        title.add(piano).padRight(12);
        title.add(number);

        TextButton.TextButtonStyle accent = new TextButton.TextButtonStyle(game.skin.get(TextButton.TextButtonStyle.class));
        accent.up = game.skin.newDrawable("white", Theme.ACCENT);
        accent.down = game.skin.newDrawable("white", ACCENT_DOWN);
        TextButton play = new TextButton("Play", accent);
        Ui.onClick(play, () -> game.switchTo(new GameScreen(game)));

        Table root = new Table();
        root.setFillParent(true);
        root.add(title).padBottom(56).row();
        root.add(play).size(BUTTON_WIDTH, BUTTON_HEIGHT).row();
        if (game.selectAudioSource != null) {
            TextButton source = new TextButton("Audio Source", game.skin);
            Ui.onClick(source, game.selectAudioSource);
            root.add(source).size(BUTTON_WIDTH, BUTTON_HEIGHT).padTop(16);
        }
        stage.addActor(root);

        Label madeWith = new Label("made with", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        madeWith.setFontScale(font.getScaleX() * 0.6f);
        Table footer = new Table();
        footer.setFillParent(true);
        footer.bottom().padBottom(20);
        footer.add(madeWith).padRight(6);
        footer.add(new Image(heart)).size(14);
        stage.addActor(footer);
    }

    /** The font has no heart glyph, so this draws one from the heart curve, smoothed by sampling each pixel 4x4. */
    private static Texture heartTexture() {
        Pixmap pixmap = new Pixmap(HEART_PIXELS, HEART_PIXELS, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        Color color = new Color(HEART);
        for (int px = 0; px < HEART_PIXELS; px++) {
            for (int py = 0; py < HEART_PIXELS; py++) {
                int inside = 0;
                for (int sx = 0; sx < 4; sx++) {
                    for (int sy = 0; sy < 4; sy++) {
                        float x = ((px + (sx + 0.5f) / 4) / HEART_PIXELS - 0.5f) * 2.6f;
                        float y = (0.5f - (py + (sy + 0.5f) / 4) / HEART_PIXELS) * 2.6f + 0.1f;
                        float a = x * x + y * y - 1;
                        if (a * a * a - x * x * y * y * y <= 0) {
                            inside++;
                        }
                    }
                }
                color.a = inside / 16f;
                pixmap.drawPixel(px, py, Color.rgba8888(color));
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
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

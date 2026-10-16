package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/**
 * Shared scene2d setup. The skin is built in code, so the project needs no skin files yet.
 */
final class Ui {
    /**
     * Glyph height in the font texture, in real pixels.
     */
    private static final int FONT_RASTER_SIZE = 128;
    private static final int CIRCLE_PIXELS = 128;
    /** The circle a rounded bar is stretched from. Small, so its ends stay smooth when scaled down. */
    private static final int DUCK_PIXELS = 256;

    /** A shape for {@link #texture}, given coordinates from -1 to 1 across the texture with y pointing up. */
    interface Shape {
        boolean contains(float x, float y);
    }

    private Ui() {
    }

    static Skin createSkin() {
        Skin skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();
        skin.add("circle", texture(CIRCLE_PIXELS, Ui::inCircle));
        for (Duck.Part part : Duck.Part.values()) {
            skin.add(duckTexture(part), texture(DUCK_PIXELS, part::contains));
        }

        // The viewport scales virtual pixels up to the screen, by about 3x on phones. The glyphs are
        // rasterized well above that and shrunk with mipmaps, so text stays sharp at any window size.
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Poppins-SemiBold.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = FONT_RASTER_SIZE;
        parameter.genMipMaps = true;
        parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().setScale(Sizes.FONT_SIZE / FONT_RASTER_SIZE);
        font.setUseIntegerPositions(false);
        skin.add("default", font);

        skin.add("default", new Label.LabelStyle(font, Colors.TEXT));

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.up = skin.newDrawable("white", Colors.BUTTON);
        button.down = skin.newDrawable("white", Colors.BUTTON_DOWN);
        button.font = font;
        skin.add("default", button);

        TextButton.TextButtonStyle round = new TextButton.TextButtonStyle(button);
        round.up = skin.newDrawable("circle", Colors.BUTTON);
        round.down = skin.newDrawable("circle", Colors.BUTTON_DOWN);
        skin.add("round", round);

        TextButton.TextButtonStyle accent = new TextButton.TextButtonStyle(button);
        accent.up = skin.newDrawable("white", Colors.ACCENT);
        accent.down = skin.newDrawable("white", Colors.ACCENT_DOWN);
        accent.fontColor = Colors.ACCENT_TEXT;
        skin.add("accent", accent);

        return skin;
    }

    /** A white texture of a shape, with its edge smoothed by sampling each pixel 4x4. */
    static Texture texture(int pixels, Shape shape) {
        Pixmap pixmap = new Pixmap(pixels, pixels, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        Color color = new Color(Color.WHITE);
        for (int px = 0; px < pixels; px++) {
            for (int py = 0; py < pixels; py++) {
                int inside = 0;
                for (int sx = 0; sx < 4; sx++) {
                    for (int sy = 0; sy < 4; sy++) {
                        float x = 2 * (px + (sx + 0.5f) / 4) / pixels - 1;
                        float y = 1 - 2 * (py + (sy + 0.5f) / 4) / pixels;
                        if (shape.contains(x, y)) {
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

    private static boolean inCircle(float x, float y) {
        return x * x + y * y <= 1;
    }

    /** The duck mascot, its layers stacked as images. */
    static Stack duck(Skin skin) {
        Stack duck = new Stack();
        for (Duck.Part part : Duck.Part.values()) {
            Image layer = new Image(skin.get(duckTexture(part), Texture.class));
            layer.setColor(part.color);
            duck.add(layer);
        }
        return duck;
    }

    static String duckTexture(Duck.Part part) {
        return "duck-" + part.name().toLowerCase();
    }

    /** A label in the skin's font, tinted and scaled relative to the normal text size. */
    static Label label(Skin skin, String text, Color color, float scale) {
        Label label = new Label(text, skin);
        label.setColor(color);
        label.setFontScale(label.getStyle().font.getScaleX() * scale);
        return label;
    }

    static Stage createStage() {
        return new Stage(new ExtendViewport(Sizes.UI_WIDTH, Sizes.UI_HEIGHT));
    }

    /** Runs the action on Android's back button or gesture, or Escape on desktop. The screen must catch back in show. */
    static void onBack(Stage stage, Runnable action) {
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode != Input.Keys.BACK && keycode != Input.Keys.ESCAPE) {
                    return false;
                }
                action.run();
                return true;
            }
        });
    }

    static void onClick(Actor actor, Runnable action) {
        actor.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
    }
}

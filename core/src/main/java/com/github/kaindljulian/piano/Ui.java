package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/**
 * Shared scene2d setup. The skin is built in code, so the project needs no skin files yet.
 */
final class Ui {
    /**
     * UI layout size in virtual pixels. The viewport stretches it to fill the screen.
     */
    private static final float WIDTH = 360;
    private static final float HEIGHT = 640;
    /**
     * Text height in virtual pixels.
     */
    private static final float FONT_SIZE = 28;
    /**
     * Glyph height in the font texture, in real pixels.
     */
    private static final int FONT_RASTER_SIZE = 128;
    private static final int CIRCLE_PIXELS = 128;
    /** The circle a rounded bar is stretched from. Small, so its ends stay smooth when scaled down. */
    private static final int PILL_PIXELS = 32;
    /** In virtual pixels. */
    private static final float TRACK_HEIGHT = 8;
    /** In virtual pixels. */
    private static final float KNOB_SIZE = 26;

    private Ui() {
    }

    static Skin createSkin() {
        Skin skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();
        skin.add("circle", circleTexture(CIRCLE_PIXELS));
        skin.add("pill", circleTexture(PILL_PIXELS));

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
        font.getData().setScale(FONT_SIZE / FONT_RASTER_SIZE);
        font.setUseIntegerPositions(false);
        skin.add("default", font);

        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.up = skin.newDrawable("white", Theme.BUTTON);
        button.down = skin.newDrawable("white", Theme.BUTTON_DOWN);
        button.font = font;
        skin.add("default", button);

        TextButton.TextButtonStyle round = new TextButton.TextButtonStyle(button);
        round.up = skin.newDrawable("circle", Theme.BUTTON);
        round.down = skin.newDrawable("circle", Theme.BUTTON_DOWN);
        skin.add("round", round);

        Slider.SliderStyle slider = new Slider.SliderStyle(bar(skin, Theme.BUTTON), circle(skin, Color.WHITE, KNOB_SIZE));
        slider.knobBefore = bar(skin, Theme.ACCENT);
        slider.knobDown = circle(skin, Color.WHITE, KNOB_SIZE * 1.15f);
        skin.add("default-horizontal", slider);

        return skin;
    }

    /** A bar with round ends, {@link #TRACK_HEIGHT} tall. */
    private static Drawable bar(Skin skin, Color color) {
        int corner = PILL_PIXELS / 2 - 1;
        NinePatch patch = new NinePatch(skin.get("pill", Texture.class), corner, corner, corner, corner);
        patch.scale(TRACK_HEIGHT / 2 / corner, TRACK_HEIGHT / 2 / corner);
        Drawable bar = new NinePatchDrawable(patch).tint(color);
        bar.setMinHeight(TRACK_HEIGHT);
        return bar;
    }

    private static Drawable circle(Skin skin, Color color, float size) {
        Drawable circle = skin.newDrawable("circle", color);
        circle.setMinWidth(size);
        circle.setMinHeight(size);
        return circle;
    }

    /** A white circle with its edge smoothed by sampling each pixel 4x4. */
    private static Texture circleTexture(int pixels) {
        Pixmap pixmap = new Pixmap(pixels, pixels, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        Color color = new Color(Color.WHITE);
        float radius = pixels / 2f;
        for (int px = 0; px < pixels; px++) {
            for (int py = 0; py < pixels; py++) {
                int inside = 0;
                for (int sx = 0; sx < 4; sx++) {
                    for (int sy = 0; sy < 4; sy++) {
                        float x = px + (sx + 0.5f) / 4 - radius;
                        float y = py + (sy + 0.5f) / 4 - radius;
                        if (x * x + y * y <= radius * radius) {
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

    static Stage createStage() {
        return new Stage(new ExtendViewport(WIDTH, HEIGHT));
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

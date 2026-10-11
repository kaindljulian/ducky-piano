package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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

    private Ui() {
    }

    static Skin createSkin() {
        Skin skin = new Skin();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));
        pixmap.dispose();

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
        button.up = skin.newDrawable("white", Color.DARK_GRAY);
        button.down = skin.newDrawable("white", Color.GRAY);
        button.font = font;
        skin.add("default", button);

        return skin;
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

package com.github.kaindljulian.piano;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.function.BooleanSupplier;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    /** Whether music is playing. The tiles only move while it is. */
    final BooleanSupplier musicPlaying;
    Skin skin;

    public Main() {
        this(() -> true);
    }

    public Main(BooleanSupplier musicPlaying) {
        this.musicPlaying = musicPlaying;
    }

    @Override
    public void create() {
        skin = Ui.createSkin();
        setScreen(new StartScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        skin.dispose();
    }

    /** Switches screens on the next frame, so a screen can safely replace itself from its own input handlers. */
    void switchTo(Screen screen) {
        Gdx.app.postRunnable(() -> setScreen(screen));
    }
}

package com.github.kaindljulian.piano;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.github.kaindljulian.piano.audio.MusicAnalyzer;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private static final String SCORES_KEY = "bestScores";

    final MusicAnalyzer music;
    /** Lets the player pick what to capture again, or null where there's no choice. */
    final Runnable selectAudioSource;
    Skin skin;
    BestScores scores;
    private Preferences preferences;

    public Main(MusicAnalyzer music, Runnable selectAudioSource) {
        this.music = music;
        this.selectAudioSource = selectAudioSource;
    }

    @Override
    public void create() {
        skin = Ui.createSkin();
        preferences = Gdx.app.getPreferences("ducky-piano");
        scores = new BestScores(preferences.getString(SCORES_KEY, ""));
        setScreen(new StartScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        skin.dispose();
    }

    /** Saves a combo that just ended, if it's among the best. */
    void recordScore(int combo) {
        if (scores.add(System.currentTimeMillis(), combo)) {
            preferences.putString(SCORES_KEY, scores.encode());
            preferences.flush();
        }
    }

    /** Switches screens on the next frame, so a screen can safely replace itself from its own input handlers. */
    void switchTo(Screen screen) {
        Gdx.app.postRunnable(() -> setScreen(screen));
    }
}

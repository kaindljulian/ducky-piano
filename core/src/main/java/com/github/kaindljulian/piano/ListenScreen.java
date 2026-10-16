package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/**
 * Explains why the game listens to other apps before the system asks, and what to do if the player says no.
 */
public class ListenScreen extends MenuScreen {
    private static final String EXPLANATION =
            """
                    Ducky Piano listens to the music playing on your phone to make matching tiles. \
                    It keeps listening while the game is open, even when you switch to your music app.

                    Android will ask to record audio and to share your screen. The game only uses the sound, \
                    not the microphone or your screen. Nothing is saved or shared.""";
    private static final String DENIED =
            """
                    Without both permissions there's no music to play along to.
                    
                    If Android doesn't ask again, allow the microphone for Ducky Piano in your phone's settings.""";
    private static final String PRIVACY_POLICY = "https://kaindljulian.github.io/ducky-piano/privacy.html";

    private final Table message = new Table();
    private final Skin skin;

    /** @param denied whether the player just said no to Android's prompts */
    ListenScreen(Main game, boolean denied) {
        super(game);
        skin = game.skin;
        showMessage(denied ? DENIED : EXPLANATION);

        TextButton allow = new TextButton(denied ? "Try again" : "Continue", skin, "accent");
        Ui.onClick(allow, () -> game.audioSource.request(allowed -> Gdx.app.postRunnable(() -> {
            game.listening = allowed;
            if (allowed) {
                game.markExplained();
                game.switchTo(new GameScreen(game));
            } else {
                showMessage(DENIED);
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
        root.add(Ui.label(skin, "Audio Source", Colors.TEXT, Sizes.TEXT_HEADING)).padBottom(24).row();
        root.add(message).width(Sizes.UI_WIDTH - 48).expandY().top().row();
        root.add(privacy).padBottom(24).row();
        root.add(allow).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).row();
        root.add(back).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).padTop(16);
        stage.addActor(root);
    }

    /** Left aligned paragraphs read better than one centered block. */
    private void showMessage(String text) {
        message.clear();
        for (String paragraph : text.split("\n\n")) {
            Label label = Ui.label(skin, paragraph, Colors.TEXT, Sizes.TEXT_SMALL);
            label.setWrap(true);
            label.setAlignment(Align.left);
            message.add(label).growX().padBottom(Sizes.PARAGRAPH_GAP).row();
        }
    }
}

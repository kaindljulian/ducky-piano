package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** The best combos, with when each was reached. */
public class ScoresScreen extends ScreenAdapter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    private final Stage stage = Ui.createStage();

    ScoresScreen(Main game) {
        Skin skin = game.skin;
        Table list = new Table();
        list.top();
        int rank = 1;
        for (BestScores.Score score : game.scores.list()) {
            list.add(Ui.label(skin, rank++ + ".", Colors.MUTED, Sizes.TEXT_SMALL)).width(Sizes.SCORE_RANK_WIDTH);
            list.add(Ui.label(skin, String.valueOf(score.combo()), Colors.ACCENT, Sizes.TEXT_NORMAL))
                .width(Sizes.SCORE_COMBO_WIDTH);
            list.add(Ui.label(skin, DATE.format(Instant.ofEpochMilli(score.time())), Colors.TEXT, Sizes.TEXT_SMALL))
                .width(Sizes.SCORE_DATE_WIDTH).row();
        }
        if (game.scores.list().isEmpty()) {
            list.add(Ui.label(skin, "No scores yet", Colors.MUTED, Sizes.TEXT_NORMAL));
        }
        ScrollPane scroll = new ScrollPane(list);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);

        TextButton back = new TextButton("Back", skin);
        Ui.onClick(back, () -> game.switchTo(new StartScreen(game)));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(32, 16, 24, 16);
        root.add(Ui.label(skin, "Best Scores", Colors.TEXT, Sizes.TEXT_TITLE)).padBottom(24).row();
        root.add(scroll).expand().fill().align(Align.top).row();
        root.add(back).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).padTop(16);
        stage.addActor(root);
        stage.setScrollFocus(scroll);
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

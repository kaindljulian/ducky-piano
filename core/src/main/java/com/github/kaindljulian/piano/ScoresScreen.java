package com.github.kaindljulian.piano;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** The best combos, with when each was reached. */
public class ScoresScreen extends MenuScreen {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault());

    ScoresScreen(Main game) {
        super(game);
        Skin skin = game.skin;
        Table list = new Table();
        list.top();
        int rank = 1;
        for (BestScores.Score score : game.scores.list()) {
            if (rank > 1) {
                list.add(new Image(skin.newDrawable("white", Colors.LANE_LINE))).colspan(3).height(1).fillX().row();
            }
            list.add(Ui.label(skin, rank++ + ".", Colors.MUTED, Sizes.TEXT_SMALL))
                .width(Sizes.SCORE_RANK_WIDTH).left().padTop(Sizes.SCORE_ROW_PAD).padBottom(Sizes.SCORE_ROW_PAD);
            list.add(Ui.label(skin, String.valueOf(score.combo()), Colors.ACCENT, Sizes.TEXT_NORMAL))
                .width(Sizes.SCORE_COMBO_WIDTH).left();
            list.add(Ui.label(skin, DATE.format(Instant.ofEpochMilli(score.time())), Colors.MUTED, Sizes.TEXT_SMALL))
                .width(Sizes.SCORE_DATE_WIDTH).right().row();
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
        root.add(Ui.label(skin, "Best Scores", Colors.TEXT, Sizes.TEXT_HEADING)).padBottom(24).row();
        root.add(scroll).expand().fill().align(Align.top).row();
        root.add(back).size(Sizes.MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).padTop(16);
        stage.addActor(root);
        stage.setScrollFocus(scroll);
    }
}

package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.kaindljulian.piano.audio.Beat;
import com.github.kaindljulian.piano.audio.BeatClock;
import com.github.kaindljulian.piano.audio.ChartGenerator;

public class GameScreen extends ScreenAdapter {
    private static final int LANES = ChartGenerator.LANES;
    private static final int VISIBLE_CELLS = 4;
    private static final int COMBO_MILESTONE = 100;
    private static final float COMBO_WIDTH = 240;
    private static final float COMBO_HEIGHT = 90;

    private final Main game;
    private final Viewport boardViewport = new StretchViewport(LANES, VISIBLE_CELLS);
    private final BoardRenderer renderer = new BoardRenderer();
    private final Stage stage = Ui.createStage();
    private final TextButton pauseButton;
    private final Table menu = new Table();
    private final Label listening;
    private final Label bpm;
    private final Label density;
    private final Label combo;
    private final Container<Label> comboPop;
    private final Sound comboBreak = Gdx.audio.newSound(Gdx.files.internal("sounds/break.wav"));
    private final Sound comboMilestone = Gdx.audio.newSound(Gdx.files.internal("sounds/milestone.wav"));
    private final Vector2 touch = new Vector2();
    private final BeatClock clock = new BeatClock();
    private final ChartGenerator chart;
    private Board board;
    private int shownCombo;

    GameScreen(Main game) {
        this.game = game;
        chart = new ChartGenerator(game.music);
        board = newBoard();
        Skin skin = game.skin;
        BitmapFont font = skin.getFont("default");
        Label.LabelStyle hint = new Label.LabelStyle(font, Color.GRAY);

        pauseButton = new TextButton("II", skin);
        Ui.onClick(pauseButton, this::openMenu);
        bpm = new Label("", hint);
        density = new Label("", hint);
        density.setFontScale(font.getScaleX() * 0.7f);
        Table info = new Table();
        info.add(bpm).right().row();
        info.add(density).right();
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(8);
        hud.add(pauseButton).size(56);
        hud.add(info).expandX().right().top().padRight(8);
        stage.addActor(hud);

        combo = new Label("", new Label.LabelStyle(font, Color.WHITE));
        combo.setFontScale(font.getScaleX() * 2);
        combo.setColor(Theme.ACCENT);
        comboPop = new Container<>(combo);
        comboPop.setTransform(true);
        comboPop.setOrigin(COMBO_WIDTH / 2, COMBO_HEIGHT / 2);
        comboPop.setVisible(false);
        Table top = new Table();
        top.setFillParent(true);
        top.top().padTop(80);
        top.add(comboPop).size(COMBO_WIDTH, COMBO_HEIGHT);
        stage.addActor(top);

        listening = new Label("Listening...", hint);
        Table center = new Table();
        center.setFillParent(true);
        center.add(listening);
        stage.addActor(center);

        menu.setFillParent(true);
        menu.setBackground(skin.newDrawable("white", new Color(0, 0, 0, 0.7f)));
        menu.setVisible(false);
        stage.addActor(menu);

        applyDensity();
    }

    private Board newBoard() {
        return new Board(LANES, VISIBLE_CELLS, rowsPerBeat(), chart);
    }

    private void openMenu() {
        Skin skin = game.skin;
        menu.clear();
        menu.add(new Label("Paused", skin)).padBottom(48).row();

        Label densityValue = new Label(densityText(), skin);
        Slider densitySlider = new Slider(1, 2, 0.1f, false, skin);
        densitySlider.setValue(game.tilesPerBeat);
        densitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.tilesPerBeat = densitySlider.getValue();
                applyDensity();
                densityValue.setText(densityText());
            }
        });
        Table densityRow = new Table();
        densityRow.add(densitySlider).width(160).padRight(16);
        densityRow.add(densityValue).width(44);
        menu.add(densityRow).padBottom(32).row();

        addMenuButton("Resume", this::closeMenu);
        addMenuButton("Restart", () -> {
            board = newBoard();
            shownCombo = 0;
            closeMenu();
        });
        addMenuButton("Main menu", () -> game.switchTo(new StartScreen(game)));
        menu.setVisible(true);
        pauseButton.setVisible(false);
    }

    private void addMenuButton(String text, Runnable action) {
        TextButton button = new TextButton(text, game.skin);
        Ui.onClick(button, action);
        menu.add(button).size(220, 60).padBottom(16).row();
    }

    private void closeMenu() {
        board.resync();
        menu.setVisible(false);
        pauseButton.setVisible(true);
    }

    private void applyDensity() {
        chart.setTilesPerBeat(game.tilesPerBeat);
        board.setRowsPerBeat(rowsPerBeat());
        density.setText(densityText());
    }

    /** Off-beat tiles need a row of their own, so any density above 1 gets a row on every 8th note. */
    private int rowsPerBeat() {
        return game.tilesPerBeat > 1 ? 2 : 1;
    }

    private String densityText() {
        return String.format("%.1fx", game.tilesPerBeat);
    }

    private void updateCombo() {
        int current = board.getCombo();
        if (current > shownCombo) {
            combo.setText(String.valueOf(current));
            pop(current % COMBO_MILESTONE == 0);
        } else if (current < shownCombo) {
            comboBreak.play();
        }
        comboPop.setVisible(current > 0);
        shownCombo = current;
    }

    private void pop(boolean milestone) {
        comboPop.clearActions();
        comboPop.setScale(1.3f);
        comboPop.addAction(Actions.scaleTo(1, 1, 0.15f, Interpolation.pow2Out));
        if (milestone) {
            comboMilestone.play();
            combo.clearActions();
            combo.setColor(Color.GOLD);
            combo.addAction(Actions.color(Theme.ACCENT, 0.6f));
        }
    }

    @Override
    public void show() {
        // The stage goes first so taps on its buttons never reach the board.
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (menu.isVisible()) return false;
                boardViewport.unproject(touch.set(screenX, screenY));
                board.tap(touch.x, touch.y, pointer);
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                board.release(pointer);
                return true;
            }
        }));
    }

    @Override
    public void render(float delta) {
        Beat beat = game.music.latestBeat();
        if (!menu.isVisible()) {
            if (beat != null) {
                board.update(clock.update(beat, System.nanoTime()));
            } else if (board.getState() == Board.State.RUNNING) {
                openMenu();
            }
        }
        updateCombo();
        listening.setVisible(beat == null && !menu.isVisible());
        bpm.setText(beat == null ? "" : Math.round(60e9 / beat.periodNanos()) + " BPM");
        renderer.render(board, boardViewport);

        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        boardViewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    /**
     * Called when the app loses focus, e.g. when the player switches apps on Android.
     */
    @Override
    public void pause() {
        if (!menu.isVisible()) openMenu();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        renderer.dispose();
        stage.dispose();
        comboBreak.dispose();
        comboMilestone.dispose();
    }
}

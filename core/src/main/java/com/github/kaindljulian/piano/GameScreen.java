package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.github.kaindljulian.piano.audio.Beat;
import com.github.kaindljulian.piano.audio.BeatClock;
import com.github.kaindljulian.piano.audio.ChartGenerator;

public class GameScreen extends ScreenAdapter {
    private static final int LANES = ChartGenerator.LANES;
    private static final int VISIBLE_CELLS = 4;
    private static final int COMBO_MILESTONE = 100;
    private static final int RAINBOW_COMBO = 1000;
    /** Smaller combos break without a quack. */
    private static final int QUACK_COMBO = 30;
    /** Each milestone up to {@link #RAINBOW_COMBO} adds this many tiles per beat, and makes the board as much faster. */
    private static final float MILESTONE_DENSITY = 0.1f;
    /** The chart can't go denser than a tile on every 8th note. */
    private static final float MAX_DENSITY = 2;

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
    private final Container<Container<Label>> comboThrob;
    private final ComboTrail comboTrail;
    private final ComboPhase comboPhase;
    private final Sound comboBreak = Gdx.audio.newSound(Gdx.files.internal("sounds/quack.wav"));
    private final Sound comboMilestone = Gdx.audio.newSound(Gdx.files.internal("sounds/milestone.wav"));
    private final Vector2 touch = new Vector2();
    private final BeatClock clock = new BeatClock();
    private final ChartGenerator chart;
    private Board board;
    private int shownCombo;
    private int boostedMilestones;
    /** Whether Android's audio prompts are showing, which pauses the app without the player leaving. */
    private boolean requesting;

    GameScreen(Main game) {
        this.game = game;
        chart = new ChartGenerator(game.music);
        board = newBoard();
        Skin skin = game.skin;

        pauseButton = new TextButton("II", skin, "round") {
            @Override
            public Actor hit(float x, float y, boolean touchable) {
                if (touchable && getTouchable() != Touchable.enabled || !isVisible()) {
                    return null;
                }
                float slop = Sizes.PAUSE_BUTTON_SLOP;
                return x >= -slop && x < getWidth() + slop && y >= -slop && y < getHeight() + slop ? this : null;
            }
        };
        Ui.onClick(pauseButton, this::openMenu);
        bpm = Ui.label(skin, "", Colors.MUTED, Sizes.TEXT_NORMAL);
        density = Ui.label(skin, "", Colors.MUTED, Sizes.TEXT_SMALL);
        Table info = new Table();
        info.add(bpm).right().row();
        info.add(density).right();
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().left().pad(8);
        hud.add(pauseButton).size(Sizes.PAUSE_BUTTON);
        hud.add(info).expandX().right().top().padRight(8);
        stage.addActor(hud);

        combo = Ui.label(skin, "", Colors.ACCENT, Sizes.TEXT_LARGE);
        comboPop = new Container<>(combo);
        comboPop.setTransform(true);
        comboPop.setOrigin(Sizes.COMBO_WIDTH / 2, Sizes.COMBO_HEIGHT / 2);
        comboPop.setVisible(false);
        comboThrob = new Container<>(comboPop).fill();
        comboThrob.setTransform(true);
        comboThrob.setOrigin(Sizes.COMBO_WIDTH / 2, Sizes.COMBO_HEIGHT / 2);
        comboTrail = new ComboTrail(skin, combo);
        stage.addActor(comboTrail);
        comboPhase = new ComboPhase(skin, combo);
        stage.addActor(comboPhase);
        Table top = new Table();
        top.setFillParent(true);
        top.top().padTop(56);
        top.add(comboThrob).size(Sizes.COMBO_WIDTH, Sizes.COMBO_HEIGHT);
        stage.addActor(top);

        listening = Ui.label(skin, "Listening...", Colors.MUTED, Sizes.TEXT_NORMAL);
        Table center = new Table();
        center.setFillParent(true);
        center.add(listening);
        stage.addActor(center);

        menu.setFillParent(true);
        menu.setBackground(skin.newDrawable("white", Colors.OVERLAY));
        menu.setVisible(false);
        stage.addActor(menu);

        applyDensity();
    }

    Board getBoard() {
        return board;
    }

    private Board newBoard() {
        return new Board(LANES, VISIBLE_CELLS, chart);
    }

    private void openMenu() {
        Skin skin = game.skin;
        menu.clear();
        menu.add(Ui.label(skin, "Paused", Colors.TEXT, Sizes.TEXT_HEADING)).padBottom(48).row();

        addMenuButton("Resume", this::closeMenu);
        addMenuButton("Restart", () -> {
            game.recordScore(shownCombo);
            shownCombo = 0;
            boostedMilestones = 0;
            board = newBoard();
            applyDensity();
            closeMenu();
        });
        addMenuButton("Main menu", () -> {
            game.recordScore(shownCombo);
            game.switchTo(new StartScreen(game));
        });
        menu.setVisible(true);
        pauseButton.setVisible(false);
    }

    private void addMenuButton(String text, Runnable action) {
        TextButton button = new TextButton(text, game.skin);
        Ui.onClick(button, action);
        menu.add(button).size(Sizes.PAUSE_MENU_BUTTON_WIDTH, Sizes.BUTTON_HEIGHT).padBottom(16).row();
    }

    private void closeMenu() {
        board.resync();
        menu.setVisible(false);
        pauseButton.setVisible(true);
    }

    private void applyDensity() {
        chart.setTilesPerBeat(density());
        board.setSpeed(density());
        density.setText(densityText());
    }

    /** Tiles per beat, going up with the combo's milestones. */
    private float density() {
        return Math.min(1 + MILESTONE_DENSITY * boostedMilestones, MAX_DENSITY);
    }

    private String densityText() {
        return String.format("%.1fx", density());
    }

    private void updateCombo() {
        int current = board.getCombo();
        boolean rainbow = current >= RAINBOW_COMBO;
        // The trail has nothing to drop at level 0.
        int level = rainbow ? 0 : current / COMBO_MILESTONE;
        comboTrail.setLevel(level);
        if (current > shownCombo) {
            combo.setText(String.valueOf(current));
            if (rainbow && shownCombo < RAINBOW_COMBO) {
                comboMilestone.play();
                combo.clearActions();
                comboTrail.clearParticles();
                pop(false);
            } else {
                pop(level > shownCombo / COMBO_MILESTONE);
            }
            comboTrail.drip();
        } else if (current < shownCombo) {
            game.recordScore(shownCombo);
            if (shownCombo >= QUACK_COMBO) {
                comboBreak.play();
            }
        }
        if (rainbow) {
            combo.setColor(comboPhase.comboColor());
        } else if (!combo.hasActions()) {
            combo.setColor(comboTrail.comboColor());
        }
        comboPhase.setVisible(rainbow);
        boolean burning = level >= ComboTrail.BURNING;
        if (burning != comboThrob.hasActions()) {
            comboThrob.clearActions();
            comboThrob.setScale(1);
            if (burning) {
                comboThrob.addAction(Actions.forever(Actions.sequence(
                    Actions.scaleTo(1.12f, 1.12f, 0.25f, Interpolation.sine),
                    Actions.scaleTo(1, 1, 0.25f, Interpolation.sine))));
            }
        }
        comboPop.setVisible(current > 0);
        shownCombo = current;
        int milestones = Math.min(current, RAINBOW_COMBO) / COMBO_MILESTONE;
        if (milestones != boostedMilestones) {
            boostedMilestones = milestones;
            applyDensity();
        }
    }

    private void pop(boolean milestone) {
        comboPop.clearActions();
        comboPop.setScale(1.3f);
        comboPop.addAction(Actions.scaleTo(1, 1, 0.15f, Interpolation.pow2Out));
        if (milestone) {
            comboMilestone.play();
            combo.clearActions();
            combo.setColor(Colors.MILESTONE);
            combo.addAction(Actions.color(comboTrail.comboColor(), 0.6f));
            comboTrail.burst();
        }
    }

    @Override
    public void show() {
        if (game.audioSource != null && !game.listening) {
            requesting = true;
            game.audioSource.request(allowed -> Gdx.app.postRunnable(() -> {
                requesting = false;
                game.listening = allowed;
                if (!allowed) {
                    game.switchTo(new ListenScreen(game, true));
                }
            }));
        }
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
        renderer.render(board, boardViewport, board.getCombo() >= RAINBOW_COMBO);

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
        if (!menu.isVisible() && !requesting) openMenu();
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

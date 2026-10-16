package com.github.kaindljulian.piano;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.github.kaindljulian.piano.audio.MusicAnalyzer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plays the real game with a bot that hits every tile, and saves the Play Store screenshots along the way: the main
 * menu, then the board at each combo. Instead of playing all the way up, it jumps to a little below each combo and
 * plays the rest, so the board has time to speed up and fill in. Lives in the game's package to reach its screens, and only ships with the
 * desktop tools.
 */
public class ScreenshotGame extends Main {
    /** How long the main menu runs before its screenshot, so the tile rain fills the screen. */
    private static final float MENU_SECONDS = 3;
    /** How many frames after reaching a combo to wait, so its pop animation settles. */
    private static final int SETTLE_FRAMES = 20;
    /** How many hits below each combo the bot jumps to. */
    private static final int RUN_UP = 20;

    private final Path folder;
    private final int[] combos;
    private float menuTime;
    private int nextCombo;
    private int settle = -1;
    private int pointer;
    private int jumped = -1;

    /** @param combos the combos to take a board screenshot at, from low to high */
    public ScreenshotGame(MusicAnalyzer music, Path folder, int... combos) {
        // The phone build asks for its audio source, which shows the Audio Source button on the menu.
        super(music, done -> {
        });
        this.folder = folder;
        this.combos = combos;
        listening = true;
    }

    @Override
    public void render() {
        super.render();
        if (getScreen() instanceof StartScreen) {
            menuTime += Gdx.graphics.getDeltaTime();
            if (menuTime >= MENU_SECONDS && music.latestBeat() != null) {
                save("1-main-menu.png");
                switchTo(new GameScreen(this));
            }
        } else if (getScreen() instanceof GameScreen screen) {
            Board board = screen.getBoard();
            play(board);
            if (jumped < nextCombo && board.getCombo() > 0) {
                board.setCombo(combos[nextCombo] - RUN_UP);
                jumped = nextCombo;
            }
            if (settle < 0 && board.getCombo() >= combos[nextCombo]) {
                settle = SETTLE_FRAMES;
            }
            if (settle == 0) {
                save((nextCombo + 2) + "-combo-" + combos[nextCombo] + ".png");
                settle = -1;
                if (++nextCombo == combos.length) {
                    Gdx.app.exit();
                }
            } else if (settle > 0) {
                settle--;
            }
        }
    }

    /** The window loses focus while the bot plays, which would open the pause menu and stop the board. */
    @Override
    public void pause() {
    }

    /** Never saves the bot's combos as best scores. */
    @Override
    void recordScore(int combo) {
    }

    /** Taps each tile in order once its bottom row reaches the bottom of the screen. Hold tiles are kept held. */
    private void play(Board board) {
        for (Tile tile : board.getTiles()) {
            if (tile.isTapped() || tile.isMissed()) continue;
            float y = board.y(tile.start);
            if (y > 0.3f) break;
            board.tap(tile.lane + 0.5f, y + 0.25f, pointer++);
        }
    }

    /** Saves the frame just drawn, without its alpha so the PNG is opaque. */
    private void save(String name) {
        int width = Gdx.graphics.getBackBufferWidth();
        int height = Gdx.graphics.getBackBufferHeight();
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
        ByteBuffer pixels = pixmap.getPixels();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = ((height - 1 - y) * width + x) * 4;
                image.setRGB(x, y, (pixels.get(i) & 0xff) << 16 | (pixels.get(i + 1) & 0xff) << 8
                    | pixels.get(i + 2) & 0xff);
            }
        }
        pixmap.dispose();
        try {
            Files.createDirectories(folder);
            ImageIO.write(image, "png", folder.resolve(name).toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("Saved " + name + " at " + width + "x" + height);
    }
}

package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

/**
 * The combo text on a rainbow combo: it runs through the rainbow with afterimages in the colors ahead of it trailing
 * to the left, and thin white speed lines shoot down past it.
 */
class ComboPhase extends Actor {
    private static final int GHOSTS = 4;
    /** In degrees around the color wheel per second. */
    private static final float HUE_SPEED = 240;
    /** In degrees around the color wheel. */
    private static final float GHOST_HUE_STEP = 45;
    /** How far apart the afterimages are, in virtual pixels. */
    private static final float GHOST_SPACING = 6;
    private static final float LINES_PER_SECOND = 40;
    /** How far beside and above the text lines can start, in virtual pixels. */
    private static final float LINE_MARGIN = 30;

    private static class Line {
        float x;
        float y;
        float length;
        float speed;
        float age;
        float life;
    }

    private final Label text;
    private final Label[] ghosts = new Label[GHOSTS];
    private final Texture circle;
    private final Array<Line> lines = new Array<>(false, 32);
    private final Color color = new Color();
    private final Vector2 corner = new Vector2();
    private final Vector2 opposite = new Vector2();
    private float time;
    private float lineDue;

    ComboPhase(Skin skin, Label text) {
        this.text = text;
        circle = skin.get("circle", Texture.class);
        for (int i = 0; i < GHOSTS; i++) {
            ghosts[i] = Ui.label(skin, "", Colors.TEXT, Sizes.TEXT_LARGE);
        }
    }

    /** The combo text's color right now. */
    Color comboColor() {
        return Colors.rainbow(color, time * HUE_SPEED, 0.6f, 1);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta;
        for (int i = lines.size - 1; i >= 0; i--) {
            Line line = lines.get(i);
            line.age += delta;
            line.y -= line.speed * delta;
            if (line.age >= line.life) {
                lines.removeIndex(i);
            }
        }
        if (!isVisible()) {
            lines.clear();
            return;
        }
        text.localToStageCoordinates(corner.set(0, 0));
        text.localToStageCoordinates(opposite.set(text.getWidth(), text.getHeight()));
        for (lineDue += delta * LINES_PER_SECOND; lineDue >= 1; lineDue--) {
            Line line = new Line();
            line.x = MathUtils.random(corner.x - LINE_MARGIN, opposite.x + LINE_MARGIN);
            line.y = MathUtils.random(corner.y, opposite.y + LINE_MARGIN);
            line.length = MathUtils.random(Sizes.SPEED_LINE_MIN, Sizes.SPEED_LINE_MAX);
            line.speed = MathUtils.random(600f, 900f);
            line.life = MathUtils.random(0.08f, 0.15f);
            lines.add(line);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        for (Line line : lines) {
            // Fades in and out, so lines don't pop up or vanish at full brightness.
            float alpha = 0.7f * MathUtils.sin(MathUtils.PI * line.age / line.life);
            batch.setColor(1, 1, 1, alpha * parentAlpha);
            batch.draw(circle, line.x - Sizes.SPEED_LINE_WIDTH / 2, line.y, Sizes.SPEED_LINE_WIDTH, line.length);
        }
        batch.setColor(Color.WHITE);

        text.localToStageCoordinates(corner.set(0, 0));
        text.localToStageCoordinates(opposite.set(text.getWidth(), text.getHeight()));
        // The text pops bigger on every hit, and the afterimages pop with it.
        float scale = (opposite.x - corner.x) / text.getWidth();
        // The farthest afterimage goes first, so the nearer ones cover it.
        for (int i = GHOSTS; i >= 1; i--) {
            Label ghost = ghosts[i - 1];
            ghost.setText(text.getText());
            ghost.setFontScale(text.getFontScaleX() * scale);
            ghost.setBounds(corner.x - i * GHOST_SPACING, corner.y, opposite.x - corner.x, opposite.y - corner.y);
            Colors.rainbow(ghost.getColor(), time * HUE_SPEED + i * GHOST_HUE_STEP, 0.8f, 1).a =
                0.7f * (1 - i / (GHOSTS + 1f));
            ghost.draw(batch, parentAlpha);
        }
    }
}

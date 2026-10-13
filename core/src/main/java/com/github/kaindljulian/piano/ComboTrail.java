package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

/**
 * Sparkles and little ducks falling out of the combo text. The level is the combo in hundreds. Every new level bursts,
 * and the bursts get bigger. From level 2 each hit drips sparkles, from level 4 ducks too. From level 5 the colors turn
 * orange, reaching red at level 9, and at {@link #BURNING} the text catches fire.
 */
class ComboTrail extends Actor {
    static final int BURNING = 10;
    private static final int ORANGE = 5;
    private static final int RED = 9;
    private static final int BURST_SPARKLES_PER_LEVEL = 15;
    private static final int MAX_BURST_SPARKLES = 200;
    private static final int BURST_DUCKS_PER_LEVEL = 2;
    private static final int MAX_BURST_DUCKS = 20;
    private static final int MAX_DRIP_SPARKLES = 8;
    private static final float DRIP_DUCK_CHANCE_PER_LEVEL = 0.1f;
    private static final float MAX_DRIP_DUCK_CHANCE = 0.6f;
    private static final float FLAMES_PER_SECOND = 40;
    /** In virtual pixels per second squared. */
    private static final float GRAVITY = -500;
    private static final float FLAME_LIFT = 150;
    /** Particles fade out over this last part of their life. */
    private static final float FADE = 0.4f;
    /** Particles fade out over this distance before reaching the middle of the screen, in virtual pixels. */
    private static final float FADE_HEIGHT = 120;

    private enum Kind {
        SPARKLE, DUCK, FLAME
    }

    private static class Particle {
        Kind kind;
        float x;
        float y;
        float vx;
        float vy;
        float rotation;
        float spin;
        float size;
        float age;
        float life;
        boolean flip;
        Color color;
    }

    private final Actor text;
    private final Texture circle;
    private final Texture[] duckParts;
    private final Color[] duckColors;
    private final Array<Particle> particles = new Array<>(false, 256);
    private final Vector2 corner = new Vector2();
    private final Vector2 opposite = new Vector2();
    private final Color tint = new Color();
    private final Color color = new Color(Colors.ACCENT);
    private final Color shade = new Color(Colors.DUCK_SHADE);
    private int level;
    private float flameDue;

    ComboTrail(Skin skin, Actor text) {
        this.text = text;
        circle = skin.get("circle", Texture.class);
        // The pond stays behind, a falling duck has no water under it.
        duckParts = new Texture[Duck.Part.values().length - 1];
        duckColors = new Color[duckParts.length];
        for (int i = 0; i < duckParts.length; i++) {
            Duck.Part part = Duck.Part.values()[i];
            duckParts[i] = skin.get(Ui.duckTexture(part), Texture.class);
            duckColors[i] = part.color;
        }
        duckColors[Duck.Part.BODY.ordinal()] = color;
        duckColors[Duck.Part.WING.ordinal()] = shade;
    }

    void setLevel(int level) {
        if (level == this.level) {
            return;
        }
        this.level = level;
        if (level < ORANGE) {
            color.set(Colors.ACCENT);
            shade.set(Colors.DUCK_SHADE);
        } else {
            float redness = Math.min((level - ORANGE) / (float) (RED - ORANGE), 1);
            color.set(Colors.COMBO_ORANGE).lerp(Colors.COMBO_RED, redness);
            shade.set(color).mul(0.85f, 0.85f, 0.85f, 1);
        }
    }

    /** The combo text's color at the current level. */
    Color comboColor() {
        return color;
    }

    /** A pop of sparkles, and ducks from level 3, for reaching a new level. */
    void burst() {
        for (int i = 0; i < Math.min(BURST_SPARKLES_PER_LEVEL * level + 5, MAX_BURST_SPARKLES); i++) {
            Particle sparkle = spawn(Kind.SPARKLE);
            sparkle.vx = MathUtils.random(-160f, 160f);
            sparkle.vy = MathUtils.random(60f, 260f);
        }
        for (int i = 0; i < Math.min(BURST_DUCKS_PER_LEVEL * (level - 2), MAX_BURST_DUCKS); i++) {
            Particle duck = spawn(Kind.DUCK);
            duck.vx = MathUtils.random(-120f, 120f);
            duck.vy = MathUtils.random(120f, 220f);
        }
    }

    /** A few sparkles from level 2, and maybe a duck from level 4, for a hit. */
    void drip() {
        for (int i = 0; i < Math.min(level - 1, MAX_DRIP_SPARKLES); i++) {
            spawn(Kind.SPARKLE);
        }
        if (level >= 4 && MathUtils.randomBoolean(
            Math.min(DRIP_DUCK_CHANCE_PER_LEVEL * (level - 3), MAX_DRIP_DUCK_CHANCE))) {
            spawn(Kind.DUCK);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (level >= BURNING) {
            flameDue += delta * FLAMES_PER_SECOND;
        } else {
            flameDue = 0;
        }
        for (; flameDue >= 1; flameDue--) {
            spawn(Kind.FLAME);
        }
        float floor = getStage().getHeight() / 2;
        for (int i = particles.size - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.age += delta;
            p.vy += (p.kind == Kind.FLAME ? FLAME_LIFT : GRAVITY) * delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.rotation += p.spin * delta;
            if (p.age >= p.life || p.y < floor) {
                particles.removeIndex(i);
            }
        }
    }

    private Particle spawn(Kind kind) {
        text.localToStageCoordinates(corner.set(0, 0));
        text.localToStageCoordinates(opposite.set(text.getWidth(), text.getHeight()));
        float width = opposite.x - corner.x;
        float height = opposite.y - corner.y;
        Particle p = new Particle();
        p.kind = kind;
        p.x = corner.x + MathUtils.random(0.1f, 0.9f) * width;
        p.vx = MathUtils.random(-30f, 30f);
        switch (kind) {
            case DUCK:
                p.y = corner.y + MathUtils.random(0.2f, 0.5f) * height;
                p.vy = MathUtils.random(-40f, 20f);
                p.size = Sizes.COMBO_DUCK;
                p.spin = MathUtils.random(-120f, 120f);
                p.flip = MathUtils.randomBoolean();
                p.life = 4;
                break;
            case SPARKLE:
                p.y = corner.y + MathUtils.random(0.2f, 0.5f) * height;
                p.vy = MathUtils.random(-40f, 20f);
                p.size = MathUtils.random(Sizes.SPARKLE_MIN, Sizes.SPARKLE_MAX);
                p.color = MathUtils.randomBoolean(0.3f) ? Colors.SPARKLE : new Color(color);
                p.life = MathUtils.random(0.8f, 1.6f);
                break;
            case FLAME:
                p.y = corner.y + MathUtils.random(0.3f, 0.7f) * height;
                p.vy = MathUtils.random(20f, 60f);
                p.size = MathUtils.random(Sizes.FLAME_MIN, Sizes.FLAME_MAX);
                p.life = MathUtils.random(0.4f, 0.8f);
                break;
        }
        particles.add(p);
        return p;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float floor = getStage().getHeight() / 2;
        for (Particle p : particles) {
            float alpha = parentAlpha * Math.min(Math.min((p.life - p.age) / (FADE * p.life), (p.y - floor) / FADE_HEIGHT), 1);
            if (p.kind == Kind.DUCK) {
                for (int i = 0; i < duckParts.length; i++) {
                    draw(batch, duckParts[i], duckColors[i], alpha, p.size, p);
                }
            } else if (p.kind == Kind.SPARKLE) {
                draw(batch, circle, p.color, alpha, p.size, p);
            }
        }
        // Flames add up to a glow where they overlap, cooling from yellow to the combo color as they rise.
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (Particle p : particles) {
            if (p.kind == Kind.FLAME) {
                float heat = p.age / p.life;
                tint.set(Colors.FLAME).lerp(color, heat);
                draw(batch, circle, tint, parentAlpha * (1 - heat), p.size * (1 - heat * 0.6f), p);
            }
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(Color.WHITE);
    }

    private void draw(Batch batch, Texture texture, Color color, float alpha, float size, Particle p) {
        batch.setColor(color.r, color.g, color.b, color.a * alpha);
        float half = size / 2;
        batch.draw(texture, p.x - half, p.y - half, half, half, size, size, 1, 1, p.rotation, 0, 0,
            texture.getWidth(), texture.getHeight(), p.flip, false);
    }
}

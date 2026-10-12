package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;

/**
 * The rubber duck mascot, facing right and floating on a small pond. It's made of layered shapes, each given
 * coordinates from -1 to 1 across a square with y pointing up, so the logo and the app icons are drawn from the same
 * duck.
 */
public final class Duck {
    /** Behind the duck on the app icons. */
    public static final Color ICON_BACKGROUND = Colors.ICON_BACKGROUND;

    /** The duck's layers, back to front. */
    public enum Part {
        BODY(Colors.DUCK) {
            @Override
            public boolean contains(float x, float y) {
                return inEllipse(x, y, -0.08f, -0.32f, 0.78f, 0.42f, 0)
                    || inEllipse(x, y, -0.7f, -0.12f, 0.28f, 0.14f, -40)
                    || inEllipse(x, y, 0.32f, 0.3f, 0.36f, 0.36f, 0);
            }
        },
        WING(Colors.DUCK_SHADE) {
            @Override
            public boolean contains(float x, float y) {
                return inEllipse(x, y, -0.2f, -0.25f, 0.4f, 0.19f, -12);
            }
        },
        BEAK(Colors.DUCK_BEAK) {
            @Override
            public boolean contains(float x, float y) {
                return inPill(x, y, 0.71f, 0.18f, 0.2f, 0.1f, -8);
            }
        },
        EYE(Colors.DUCK_EYE) {
            @Override
            public boolean contains(float x, float y) {
                return inEllipse(x, y, 0.42f, 0.38f, 0.065f, 0.065f, 0);
            }
        },
        SHINE(Colors.DUCK_SHINE) {
            @Override
            public boolean contains(float x, float y) {
                return inEllipse(x, y, 0.44f, 0.41f, 0.022f, 0.022f, 0)
                    || inEllipse(x, y, 0.2f, 0.5f, 0.09f, 0.05f, 30);
            }
        },
        POND(Colors.WATER) {
            @Override
            public boolean contains(float x, float y) {
                return y < -0.62f + 0.035f * MathUtils.sin(7 * x + 1)
                    && inEllipse(x, y, 0, -0.74f, 0.98f, 0.22f, 0);
            }
        };

        public final Color color;

        Part(Color color) {
            this.color = color;
        }

        public abstract boolean contains(float x, float y);
    }

    private Duck() {
    }

    /**
     * Whether a point is inside a pill reaching {@code length} to each side of its center, with round ends of
     * {@code radius}, turned {@code degrees} left.
     */
    private static boolean inPill(float x, float y, float cx, float cy, float length, float radius, float degrees) {
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
        float dx = x - cx;
        float dy = y - cy;
        float u = dx * cos + dy * sin;
        float v = dy * cos - dx * sin;
        float along = Math.abs(u) - Math.max(length - radius, 0);
        return Math.max(along, 0) * Math.max(along, 0) + v * v <= radius * radius;
    }

    /** Whether a point is inside an ellipse with radii {@code rx} and {@code ry}, turned {@code degrees} left. */
    private static boolean inEllipse(float x, float y, float cx, float cy, float rx, float ry, float degrees) {
        float cos = MathUtils.cosDeg(degrees);
        float sin = MathUtils.sinDeg(degrees);
        float dx = x - cx;
        float dy = y - cy;
        float u = (dx * cos + dy * sin) / rx;
        float v = (dy * cos - dx * sin) / ry;
        return u * u + v * v <= 1;
    }
}

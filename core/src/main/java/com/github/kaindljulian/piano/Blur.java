package com.github.kaindljulian.piano;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * Blurs whatever is drawn between {@link #begin} and {@link #end} as one layer, then {@link #draw} puts it on the
 * screen. The layer is drawn at a quarter of the screen size and blurred a few times with a Gaussian, first across
 * and then down, which is cheap and hides the low resolution.
 */
class Blur implements Disposable {
    private static final int DOWNSCALE = 4;
    /** Distance between blur samples, in pixels of the small layer. Above about 1.5 the result gets streaky. */
    private static final float SPREAD = 1.5f;
    /** Each pass blurs the result of the one before, so more passes blur further. */
    private static final int PASSES = 3;
    private static final String VERTEX = """
        attribute vec4 a_position;
        attribute vec2 a_texCoord0;
        uniform mat4 u_projTrans;
        varying vec2 v_texCoords;
        void main() {
            v_texCoords = a_texCoord0;
            gl_Position = u_projTrans * a_position;
        }
        """;
    // 9 taps of a Gaussian from 5 texture reads: each off-center read falls between two taps and linear filtering
    // averages them with the right weights.
    private static final String FRAGMENT = """
        #ifdef GL_ES
        precision mediump float;
        #endif
        varying vec2 v_texCoords;
        uniform sampler2D u_texture;
        uniform vec2 u_step;
        void main() {
            vec4 sum = texture2D(u_texture, v_texCoords) * 0.227027;
            sum += texture2D(u_texture, v_texCoords + u_step * 1.384615) * 0.316216;
            sum += texture2D(u_texture, v_texCoords - u_step * 1.384615) * 0.316216;
            sum += texture2D(u_texture, v_texCoords + u_step * 3.230769) * 0.070270;
            sum += texture2D(u_texture, v_texCoords - u_step * 3.230769) * 0.070270;
            gl_FragColor = sum;
        }
        """;

    private final ShaderProgram shader = new ShaderProgram(VERTEX, FRAGMENT);
    private final SpriteBatch batch = new SpriteBatch();
    private FrameBuffer layer;
    private FrameBuffer across;

    Blur() {
        if (!shader.isCompiled()) {
            throw new GdxRuntimeException(shader.getLog());
        }
        batch.setShader(shader);
        batch.disableBlending();
        batch.getProjectionMatrix().setToOrtho2D(0, 0, 1, 1);
    }

    /** Takes the size of the screen in pixels. */
    void resize(int width, int height) {
        disposeBuffers();
        layer = buffer(width, height);
        across = buffer(width, height);
    }

    void begin() {
        layer.begin();
        ScreenUtils.clear(Color.BLACK);
    }

    void end() {
        layer.end();
    }

    void draw() {
        for (int i = 0; i < PASSES; i++) {
            across.begin();
            pass(layer, 1, 0);
            across.end();
            if (i == PASSES - 1) {
                pass(across, 0, 1);
            } else {
                layer.begin();
                pass(across, 0, 1);
                layer.end();
            }
        }
    }

    private void pass(FrameBuffer from, float x, float y) {
        Texture texture = from.getColorBufferTexture();
        batch.begin();
        shader.setUniformf("u_step", x * SPREAD / texture.getWidth(), y * SPREAD / texture.getHeight());
        batch.draw(texture, 0, 0, 1, 1, 0, 0, 1, 1);
        batch.end();
    }

    private static FrameBuffer buffer(int width, int height) {
        FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(1, width / DOWNSCALE),
            Math.max(1, height / DOWNSCALE), false);
        buffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return buffer;
    }

    private void disposeBuffers() {
        if (layer != null) {
            layer.dispose();
            across.dispose();
        }
    }

    @Override
    public void dispose() {
        disposeBuffers();
        batch.dispose();
        shader.dispose();
    }
}

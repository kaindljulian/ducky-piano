package com.github.kaindljulian.piano.lwjgl3;

import com.badlogic.gdx.graphics.Color;
import com.github.kaindljulian.piano.Duck;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Draws the app icons from {@link Duck}. Run it from the project root with {@code gradlew :lwjgl3:exportIcons} after
 * changing the duck.
 */
public final class IconExporter {
    private static final String[] DENSITIES = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
    private static final float[] DENSITY_SCALES = {1, 1.5f, 2, 3, 4};
    /** In dp. */
    private static final int LEGACY_SIZE = 48;
    /** In dp. Android masks it down to the inner 66dp, so the duck has to fit in there. */
    private static final int ADAPTIVE_SIZE = 108;
    /** How much of an icon the duck spans, on a background and on an adaptive icon's foreground. */
    private static final float DUCK_SCALE = 0.78f;
    private static final float FOREGROUND_DUCK_SCALE = 0.56f;
    /** The middle of the duck, from its pond to the top of its head, which goes in the middle of the icon. */
    private static final float DUCK_MIDDLE = -0.15f;
    /** The corner radius of the background, from 0 to 1. */
    private static final float CORNER = 0.45f;

    private IconExporter() {
    }

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < DENSITIES.length; i++) {
            Path folder = Path.of("android/res/drawable-" + DENSITIES[i]);
            write(icon(Math.round(LEGACY_SIZE * DENSITY_SCALES[i]), true), folder.resolve("ic_launcher.png"));
            write(icon(Math.round(ADAPTIVE_SIZE * DENSITY_SCALES[i]), false),
                folder.resolve("ic_launcher_foreground.png"));
        }
        for (int size : new int[]{16, 32, 64, 128}) {
            write(icon(size, true), Path.of("lwjgl3/src/main/resources/icon" + size + ".png"));
        }
        write(icon(512, true), Path.of("lwjgl3/icons/logo.png"));
        writeIco(Path.of("lwjgl3/icons/logo.ico"), 16, 32, 48, 256);
        writeIcns(Path.of("lwjgl3/icons/logo.icns"));
    }

    /**
     * The duck on a rounded square, or on its own for an adaptive icon's foreground. Each pixel is sampled 4x4 and
     * the layers are blended per sample, so edges are smooth and the pond stays inside the background.
     */
    private static BufferedImage icon(int size, boolean background) {
        float scale = background ? DUCK_SCALE : FOREGROUND_DUCK_SCALE;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        float[] pixel = new float[4];
        float[] sample = new float[4];
        for (int px = 0; px < size; px++) {
            for (int py = 0; py < size; py++) {
                Arrays.fill(pixel, 0);
                for (int sx = 0; sx < 4; sx++) {
                    for (int sy = 0; sy < 4; sy++) {
                        float x = 2 * (px + (sx + 0.5f) / 4) / size - 1;
                        float y = 1 - 2 * (py + (sy + 0.5f) / 4) / size;
                        Arrays.fill(sample, 0);
                        if (background) {
                            if (!inRoundedSquare(x, y)) continue;
                            blend(sample, Duck.ICON_BACKGROUND);
                        }
                        for (Duck.Part part : Duck.Part.values()) {
                            if (part.contains(x / scale, y / scale + DUCK_MIDDLE)) {
                                blend(sample, part.color);
                            }
                        }
                        for (int i = 0; i < 4; i++) {
                            pixel[i] += sample[i] / 16;
                        }
                    }
                }
                image.setRGB(px, py, argb(pixel));
            }
        }
        return image;
    }

    private static boolean inRoundedSquare(float x, float y) {
        float dx = Math.max(Math.abs(x) - (1 - CORNER), 0);
        float dy = Math.max(Math.abs(y) - (1 - CORNER), 0);
        return dx * dx + dy * dy <= CORNER * CORNER;
    }

    /** Lays a color over a premultiplied red, green, blue and alpha. */
    private static void blend(float[] premultiplied, Color color) {
        float keep = 1 - color.a;
        premultiplied[0] = color.r * color.a + premultiplied[0] * keep;
        premultiplied[1] = color.g * color.a + premultiplied[1] * keep;
        premultiplied[2] = color.b * color.a + premultiplied[2] * keep;
        premultiplied[3] = color.a + premultiplied[3] * keep;
    }

    private static int argb(float[] premultiplied) {
        float alpha = premultiplied[3];
        if (alpha == 0) return 0;
        int a = Math.round(alpha * 255);
        int r = Math.round(premultiplied[0] / alpha * 255);
        int g = Math.round(premultiplied[1] / alpha * 255);
        int b = Math.round(premultiplied[2] / alpha * 255);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static void write(BufferedImage image, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }

    private static byte[] png(int size) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(icon(size, true), "png", bytes);
        return bytes.toByteArray();
    }

    /** A Windows icon with a PNG for each size. */
    private static void writeIco(Path path, int... sizes) throws IOException {
        byte[][] images = new byte[sizes.length][];
        for (int i = 0; i < sizes.length; i++) {
            images[i] = png(sizes[i]);
        }
        ByteBuffer header = ByteBuffer.allocate(6 + 16 * sizes.length).order(ByteOrder.LITTLE_ENDIAN);
        header.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        int offset = header.capacity();
        for (int i = 0; i < sizes.length; i++) {
            // A size of 256 doesn't fit in a byte, so it's written as 0.
            header.put((byte) (sizes[i] % 256)).put((byte) (sizes[i] % 256)).put((byte) 0).put((byte) 0);
            header.putShort((short) 1).putShort((short) 32).putInt(images[i].length).putInt(offset);
            offset += images[i].length;
        }
        try (OutputStream out = Files.newOutputStream(path)) {
            out.write(header.array());
            for (byte[] image : images) {
                out.write(image);
            }
        }
    }

    /** A macOS icon with PNGs of 128, 256 and 512 pixels. */
    private static void writeIcns(Path path) throws IOException {
        String[] types = {"ic07", "ic08", "ic09"};
        byte[][] images = {png(128), png(256), png(512)};
        int length = 8;
        for (byte[] image : images) {
            length += 8 + image.length;
        }
        ByteBuffer icns = ByteBuffer.allocate(length);
        icns.put("icns".getBytes()).putInt(length);
        for (int i = 0; i < types.length; i++) {
            icns.put(types[i].getBytes()).putInt(8 + images[i].length).put(images[i]);
        }
        Files.write(path, icns.array());
    }
}

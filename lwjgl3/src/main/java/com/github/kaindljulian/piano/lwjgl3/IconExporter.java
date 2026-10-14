package com.github.kaindljulian.piano.lwjgl3;

import com.badlogic.gdx.graphics.Color;
import com.github.kaindljulian.piano.Duck;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
 * Draws the app icons and the Google Play feature graphic from {@link Duck}. Run it from the project root with
 * {@code gradlew :lwjgl3:exportIcons} after changing the duck.
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
    /** Colors from {@code Colors}, which the desktop module can't see. */
    private static final java.awt.Color BACKGROUND = new java.awt.Color(0.07f, 0.09f, 0.2f);
    private static final java.awt.Color ACCENT = new java.awt.Color(1f, 0.82f, 0.2f);
    private static final java.awt.Color MUTED = new java.awt.Color(0.5f, 0.58f, 0.8f);

    private IconExporter() {
    }

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < DENSITIES.length; i++) {
            Path folder = Path.of("android/res/drawable-" + DENSITIES[i]);
            write(icon(Math.round(LEGACY_SIZE * DENSITY_SCALES[i]), true, CORNER), folder.resolve("ic_launcher.png"));
            write(icon(Math.round(ADAPTIVE_SIZE * DENSITY_SCALES[i]), false, CORNER),
                folder.resolve("ic_launcher_foreground.png"));
        }
        for (int size : new int[]{16, 32, 64, 128}) {
            write(icon(size, true, CORNER), Path.of("lwjgl3/src/main/resources/icon" + size + ".png"));
        }
        write(icon(512, true, CORNER), Path.of("lwjgl3/icons/logo.png"));
        // Google Play rounds the corners of its store icon itself, so it gets a full square.
        write(icon(512, true, 0), Path.of("android/play/icon.png"));
        writeIco(Path.of("lwjgl3/icons/logo.ico"), 16, 32, 48, 256);
        writeIcns(Path.of("lwjgl3/icons/logo.icns"));
        write(featureGraphic(), Path.of("android/play/feature-graphic.png"));
    }

    /** The duck and the game's name on the menus' background color. */
    private static BufferedImage featureGraphic() throws IOException {
        int width = 1024;
        int height = 500;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(BACKGROUND);
        g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, Path.of("assets/fonts/Poppins-SemiBold.ttf").toFile());
        } catch (FontFormatException e) {
            throw new IOException(e);
        }
        Font title = font.deriveFont(88f);
        Font tagline = font.deriveFont(26f);
        FontMetrics titleMetrics = g.getFontMetrics(title);
        String first = "Ducky ";
        String second = "Piano";
        int titleWidth = titleMetrics.stringWidth(first + second);

        // The duck takes up the middle 56% of its image, so the image overlaps the title's side a little.
        int duckSize = 360;
        int duckWidth = Math.round(duckSize * FOREGROUND_DUCK_SCALE);
        int gap = 24;
        int left = (width - duckWidth - gap - titleWidth) / 2;
        g.drawImage(icon(duckSize, false, CORNER), left + duckWidth / 2 - duckSize / 2, height / 2 - duckSize / 2, null);

        int textLeft = left + duckWidth + gap;
        int baseline = height / 2 + 10;
        g.setFont(title);
        g.setColor(ACCENT);
        g.drawString(first, textLeft, baseline);
        g.setColor(java.awt.Color.WHITE);
        g.drawString(second, textLeft + titleMetrics.stringWidth(first), baseline);
        g.setFont(tagline);
        g.setColor(MUTED);
        g.drawString("Quack quack quack.", textLeft + 4, baseline + 46);
        g.dispose();
        return image;
    }

    /**
     * The duck on a square with the given corner radius, or on its own for an adaptive icon's foreground. Each pixel is sampled 4x4 and
     * the layers are blended per sample, so edges are smooth and the pond stays inside the background.
     */
    private static BufferedImage icon(int size, boolean background, float corner) {
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
                            if (!inRoundedSquare(x, y, corner)) continue;
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

    private static boolean inRoundedSquare(float x, float y, float corner) {
        float dx = Math.max(Math.abs(x) - (1 - corner), 0);
        float dy = Math.max(Math.abs(y) - (1 - corner), 0);
        return dx * dx + dy * dy <= corner * corner;
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
        ImageIO.write(icon(size, true, CORNER), "png", bytes);
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

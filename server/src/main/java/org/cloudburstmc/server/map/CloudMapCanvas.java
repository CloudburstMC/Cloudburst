package org.cloudburstmc.server.map;

import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.map.MapCanvas;
import org.cloudburstmc.api.map.MapCursor;
import org.cloudburstmc.api.map.MapView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CloudMapCanvas implements MapCanvas {

    private final MapView map;
    private final int[] pixels = new int[128 * 128];
    private final List<MapCursor> cursors = new ArrayList<>();
    @Setter
    private int[] base = new int[128 * 128];

    public CloudMapCanvas(MapView map) {
        this.map = Objects.requireNonNull(map, "map");
    }

    @Override
    public MapView getMapView() {
        return this.map;
    }

    @Override
    public @Nullable Color getPixelColor(int x, int y) {
        int color = this.pixels[index(x, y)];
        return color == 0 ? null : new Color(color, true);
    }

    @Override
    public Color getBasePixelColor(int x, int y) {
        return new Color(this.base[index(x, y)], true);
    }

    @Override
    public void setPixelColor(int x, int y, @Nullable Color color) {
        this.pixels[index(x, y)] = color == null || color.getAlpha() == 0 ? 0 : color.getRGB();
    }

    @Override
    public void drawImage(int x, int y, BufferedImage image) {
        Objects.requireNonNull(image, "image");
        for (int row = Math.max(0, y); row < Math.min(128L, (long) y + image.getHeight()); row++) {
            for (int column = Math.max(0, x); column < Math.min(128L, (long) x + image.getWidth()); column++) {
                int color = image.getRGB(column - x, row - y);
                this.pixels[index(column, row)] = (color >>> 24) == 0 ? 0 : color;
            }
        }
    }

    @Override
    public List<MapCursor> getCursors() {
        return this.cursors;
    }

    public void copyTerrain(int[] terrain) {
        System.arraycopy(terrain, 0, this.pixels, 0, this.pixels.length);
    }

    public void compositeInto(int[] result) {
        for (int i = 0; i < result.length; i++) {
            result[i] = composite(result[i], this.pixels[i]);
        }
    }

    public static int composite(int below, int above) {
        int alpha = above >>> 24;
        if (alpha == 0) {
            return below;
        }

        if (alpha == 255) {
            return above;
        }

        int belowAlpha = below >>> 24;
        int outAlpha = alpha + belowAlpha * (255 - alpha) / 255;
        int result = outAlpha << 24;

        for (int shift = 0; shift <= 16; shift += 8) {
            int component = (((above >>> shift) & 255) * alpha + ((below >>> shift) & 255) * belowAlpha * (255 - alpha) / 255) / outAlpha;
            result |= component << shift;
        }

        return result;
    }

    private static int index(int x, int y) {
        Objects.checkIndex(x, 128);
        Objects.checkIndex(y, 128);
        return y * 128 + x;
    }
}

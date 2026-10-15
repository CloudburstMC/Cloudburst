package org.cloudburstmc.api.map;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * A 128 by 128 renderer layer. Pixel coordinates range from 0 through 127.
 * Pixels and cursors are retained between render calls. Access the canvas on the server thread.
 */
public interface MapCanvas {

    /**
     * @return the map containing this layer
     */
    MapView getMapView();

    /**
     * @param x the horizontal pixel coordinate
     * @param y the vertical pixel coordinate
     * @return the pixel color, or null when this layer is fully transparent at the pixel
     * @throws IndexOutOfBoundsException if either coordinate is outside the canvas
     */
    @Nullable
    Color getPixelColor(int x, int y);

    /**
     * @param x the horizontal pixel coordinate
     * @param y the vertical pixel coordinate
     * @return the composite pixel color below this layer, transparent when no lower layer covers it
     * @throws IndexOutOfBoundsException if either coordinate is outside the canvas
     */
    Color getBasePixelColor(int x, int y);

    /**
     * Sets a layer pixel. Translucent colors blend with the layers below it.
     *
     * @param x     the horizontal pixel coordinate
     * @param y     the vertical pixel coordinate
     * @param color the pixel color, or null to reveal lower layers
     * @throws IndexOutOfBoundsException if either coordinate is outside the canvas
     */
    void setPixelColor(int x, int y, @Nullable Color color);

    /**
     * Copies image pixels into this layer at their original size, clipping at the canvas edges.
     * Transparent pixels reveal lower layers. Translucent pixels retain their opacity.
     *
     * @param x     the image's left pixel coordinate
     * @param y     the image's top pixel coordinate
     * @param image the image to draw
     */
    void drawImage(int x, int y, BufferedImage image);

    /**
     * @return the mutable cursor list belonging to this layer
     */
    List<MapCursor> getCursors();
}

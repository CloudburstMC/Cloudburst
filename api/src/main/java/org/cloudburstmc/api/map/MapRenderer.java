package org.cloudburstmc.api.map;

import org.cloudburstmc.api.player.Player;

/**
 * Draws a retained map layer. Transparent pixels reveal the layers below it.
 */
public abstract class MapRenderer {

    private final boolean contextual;

    /**
     * Creates a renderer whose canvas is shared by viewers.
     */
    protected MapRenderer() {
        this(false);
    }

    /**
     * @param contextual whether each viewer has a separate canvas
     */
    protected MapRenderer(boolean contextual) {
        this.contextual = contextual;
    }

    /**
     * @return whether each viewer has a separate canvas
     */
    public boolean isContextual() {
        return this.contextual;
    }

    /**
     * Called after this renderer is added to a map, before its first render call.
     * Removing and re-adding the renderer calls this method again.
     *
     * @param map the map this renderer has been added to
     */
    public void initialize(MapView map) {
    }

    /**
     * Updates this renderer's retained canvas on the server thread.
     * For a non-contextual renderer, changes made for one viewer are visible to other viewers.
     *
     * @param map    the map being rendered
     * @param canvas this renderer's layer
     * @param player the current viewer
     */
    public abstract void render(MapView map, MapCanvas canvas, Player player);
}

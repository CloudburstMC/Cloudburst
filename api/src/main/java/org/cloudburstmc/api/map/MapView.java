package org.cloudburstmc.api.map;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.player.Player;

import java.util.List;

/**
 * A persistent 128 by 128 map and its ordered rendering layers.
 * Renderer changes affect presentation, not the map item identity.
 * Access this map and its renderers on the server thread. Renderer registrations
 * and their canvases are not saved with the map.
 */
public interface MapView {

    /**
     * @return the stable identifier shared by items referring to this map
     */
    long getId();

    /**
     * @return a filled map item referring to this map
     */
    ItemStack createItem();

    /**
     * @return the mapped level, or null when it is unloaded
     */
    @Nullable
    Level getLevel();

    /**
     * Changes the mapped level without clearing existing terrain colors.
     *
     * @param level the level to map
     * @throws IllegalArgumentException if the level belongs to a different server
     */
    void setLevel(Level level);

    /**
     * @return the world X coordinate at the map's center
     */
    int getCenterX();

    /**
     * @param x the world X coordinate at the map's center
     */
    void setCenterX(int x);

    /**
     * @return the world Z coordinate at the map's center
     */
    int getCenterZ();

    /**
     * @param z the world Z coordinate at the map's center
     */
    void setCenterZ(int z);

    /**
     * @return the map's sampling scale
     */
    MapScale getScale();

    /**
     * Changes the sampling scale without realigning the center or clearing existing terrain colors.
     *
     * @param scale the new sampling scale
     */
    void setScale(MapScale scale);

    /**
     * @return whether automatic terrain updates are disabled
     */
    boolean isLocked();

    /**
     * Locking does not prevent custom renderers from changing the map's presentation.
     *
     * @param locked whether to freeze automatic terrain updates
     */
    void setLocked(boolean locked);

    /**
     * @return whether player and item-frame position markers are enabled
     */
    boolean isTrackingPosition();

    /**
     * @param tracking whether to show player and item-frame position markers
     */
    void setTrackingPosition(boolean tracking);

    /**
     * @return whether distant holders remain visible at the map edge
     */
    boolean isUnlimitedTracking();

    /**
     * @param unlimited whether to retain cursors for distant holders
     */
    void setUnlimitedTracking(boolean unlimited);

    /**
     * @return an immutable snapshot of renderers, in drawing order
     */
    List<MapRenderer> getRenderers();

    /**
     * Adds a renderer above the existing layers and calls {@link MapRenderer#initialize(MapView)}.
     * Adding the same renderer instance again has no effect while it remains registered.
     *
     * @param renderer the renderer to add
     */
    void addRenderer(MapRenderer renderer);

    /**
     * Removes a renderer and its retained canvases.
     *
     * @param renderer the renderer to remove
     * @return whether the renderer was present
     */
    boolean removeRenderer(MapRenderer renderer);

    /**
     * Renders and sends the current map to a player.
     *
     * @param player the recipient
     * @throws IllegalArgumentException if the player belongs to a different server
     */
    void send(Player player);
}

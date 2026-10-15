package org.cloudburstmc.api.map;

import org.cloudburstmc.api.level.Level;

import java.util.Optional;

/**
 * Creates and resolves persistent maps. Access maps on the server thread.
 */
public interface Maps {

    /**
     * Creates a map aligned to the scale's grid around the supplied world position.
     * The map starts with a terrain renderer and position tracking enabled.
     * {@link org.cloudburstmc.api.event.server.MapInitializeEvent} is fired before this method returns.
     *
     * @param level the level to map
     * @param x the world X coordinate
     * @param z the world Z coordinate
     * @param scale the sampling scale
     * @return the new map
     * @throws IllegalArgumentException if the level belongs to a different server
     * @throws IllegalStateException if the map cannot be stored
     */
    MapView create(Level level, int x, int z, MapScale scale);

    /**
     * Resolves a map, loading it if necessary.
     *
     * @param id the persistent map identifier
     * @return the map, or an empty result when the identifier is unknown
     * @throws IllegalStateException if stored map data cannot be loaded
     */
    Optional<MapView> get(long id);

    /**
     * Creates an independent map with the same terrain and settings.
     * Custom renderers are not copied. They can be registered through the new map's
     * {@link org.cloudburstmc.api.event.server.MapInitializeEvent}.
     *
     * @param original the map to copy
     * @return the new map with a distinct identifier
     * @throws IllegalArgumentException if the original belongs to a different server
     * @throws IllegalStateException if the copy cannot be stored
     */
    MapView copy(MapView original);
}

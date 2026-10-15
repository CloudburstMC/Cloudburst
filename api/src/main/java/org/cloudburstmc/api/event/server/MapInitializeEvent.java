package org.cloudburstmc.api.event.server;

import org.cloudburstmc.api.map.MapView;

import java.util.Objects;

/**
 * Fired when a map is created or first loaded, after its initial settings are applied.
 * Listeners may change its settings and renderers before it is used.
 */
public class MapInitializeEvent extends ServerEvent {

    private final MapView map;

    /**
     * @param map the initialized map
     */
    public MapInitializeEvent(MapView map) {
        this.map = Objects.requireNonNull(map, "map");
    }

    /**
     * @return the initialized map
     */
    public MapView getMap() {
        return this.map;
    }
}

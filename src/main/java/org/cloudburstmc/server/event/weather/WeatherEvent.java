package org.cloudburstmc.server.event.weather;

import org.cloudburstmc.server.event.Event;
import org.cloudburstmc.server.world.World;

/**
 * author: funcraft
 * Nukkit Project
 */
public abstract class WeatherEvent extends Event {

    private final World world;

    public WeatherEvent(World world) {
        this.world = world;
    }

    public World getWorld() {
        return world;
    }
}

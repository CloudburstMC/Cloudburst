package org.cloudburstmc.server.event.weather;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.world.World;

/**
 * author: funcraft
 * Nukkit Project
 */
public class ThunderChangeEvent extends WeatherEvent implements Cancellable {

    private final boolean to;

    public ThunderChangeEvent(World world, boolean to) {
        super(world);
        this.to = to;
    }

    /**
     * Gets the state of thunder that the world is being set to
     *
     * @return true if the thunder is being set to start, false otherwise
     */
    public boolean toThunderState() {
        return to;
    }

}

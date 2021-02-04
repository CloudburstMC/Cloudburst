package org.cloudburstmc.server.event.weather;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.level.Level;

/**
 * author: funcraft
 * Nukkit Project
 */
public class ThunderChangeEvent extends WeatherEvent implements Cancellable {

    private final boolean to;

    public ThunderChangeEvent(Level level, boolean to) {
        super(level);
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

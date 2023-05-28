package org.cloudburstmc.api.event.level;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Level;

/**
 * author: funcraft
 * Nukkit Project
 */
public final class WeatherChangeEvent extends WeatherEvent implements Cancellable {

    private final boolean to;

    public WeatherChangeEvent(Level level, boolean to) {
        super(level);
        this.to = to;
    }

    /**
     * Gets the state of weather that the world is being set to
     *
     * @return true if the weather is being set to raining, false otherwise
     */
    public boolean toWeatherState() {
        return to;
    }

}

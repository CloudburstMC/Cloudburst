package org.cloudburstmc.server.event.weather;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.server.entity.misc.LightningBolt;
import org.cloudburstmc.server.level.Level;

/**
 * author: funcraft
 * Nukkit Project
 */
public class LightningStrikeEvent extends WeatherEvent implements Cancellable {

    private final LightningBolt lightningBolt;

    public LightningStrikeEvent(Level level, final LightningBolt lightningBolt) {
        super(level);
        this.lightningBolt = lightningBolt;
    }

    /**
     * Gets the bolt which is striking the earth.
     *
     * @return lightning entity
     */
    public LightningBolt getLightningBolt() {
        return lightningBolt;
    }

}

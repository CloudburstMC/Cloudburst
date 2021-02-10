package org.cloudburstmc.api.event.weather;

import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.level.Level;

/**
 * author: funcraft
 * Nukkit Project
 */
public final class LightningStrikeEvent extends WeatherEvent implements Cancellable {

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

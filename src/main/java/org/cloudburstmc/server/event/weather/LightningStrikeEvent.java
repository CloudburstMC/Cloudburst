package org.cloudburstmc.server.event.weather;

import org.cloudburstmc.server.entity.misc.LightningBolt;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.world.World;

/**
 * author: funcraft
 * Nukkit Project
 */
public class LightningStrikeEvent extends WeatherEvent implements Cancellable {

    private final LightningBolt lightningBolt;

    public LightningStrikeEvent(World world, final LightningBolt lightningBolt) {
        super(world);
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

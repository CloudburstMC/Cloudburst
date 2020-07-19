package org.cloudburstmc.server.event.weather;

import org.cloudburstmc.server.entity.misc.LightningBolt;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.level.Level;

/**
 * author: funcraft
 * Nukkit Project
 */
public class LightningStrikeEvent extends WeatherEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final LightningBolt lightningBolt;

    public static HandlerList getHandlers() {
        return handlers;
    }

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

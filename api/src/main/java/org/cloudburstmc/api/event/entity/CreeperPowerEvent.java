package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.hostile.Creeper;
import org.cloudburstmc.api.entity.misc.LightningBolt;
import org.cloudburstmc.api.event.Cancellable;

public class CreeperPowerEvent extends EntityEvent implements Cancellable {

    private final CreeperPowerCause cause;
    private LightningBolt lightningBolt;

    public CreeperPowerEvent(final Creeper creeper, final LightningBolt lightningBolt, final CreeperPowerCause cause) {
        this(creeper, cause);
        this.lightningBolt = lightningBolt;
    }

    public CreeperPowerEvent(final Creeper creeper, final CreeperPowerCause cause) {
        this.entity = creeper;
        this.cause = cause;
    }

    @Override
    public Creeper getEntity() {
        return (Creeper) super.getEntity();
    }

    /**
     * Gets the lightning bolt which is striking the Creeper.
     *
     * @return The Entity for the lightning bolt which is striking the Creeper
     */
    public LightningBolt getLightningBolt() {
        return lightningBolt;
    }

    /**
     * Gets the cause of the creeper being (un)powered.
     *
     * @return why the creeper's powered state changed
     */
    public CreeperPowerCause getCause() {
        return cause;
    }
}

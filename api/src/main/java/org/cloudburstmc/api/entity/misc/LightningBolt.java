package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;

/**
 * A lightning strike that may be visual only.
 */
public interface LightningBolt extends Entity {

    /**
     * Returns whether this bolt is prevented from damaging entities and starting fires.
     */
    boolean isVisualOnly();

    /**
     * Controls whether the bolt can damage entities or start fires.
     */
    void setVisualOnly(boolean visualOnly);
}

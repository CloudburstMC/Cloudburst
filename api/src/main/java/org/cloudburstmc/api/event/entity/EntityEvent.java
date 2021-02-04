package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.event.Event;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityEvent extends Event {

    protected Entity entity;

    public Entity getEntity() {
        return entity;
    }
}

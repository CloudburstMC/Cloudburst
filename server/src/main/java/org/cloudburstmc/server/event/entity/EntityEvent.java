package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Event;

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

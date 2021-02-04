package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.entity.EntityEvent;

public class ProjectileLaunchEvent extends EntityEvent implements Cancellable {

    public ProjectileLaunchEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }
}

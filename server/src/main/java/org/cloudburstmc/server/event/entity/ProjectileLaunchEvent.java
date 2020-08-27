package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;

public class ProjectileLaunchEvent extends EntityEvent implements Cancellable {

    public ProjectileLaunchEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return this.entity;
    }
}

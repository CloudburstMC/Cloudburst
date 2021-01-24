package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityLevelChangeEvent extends EntityEvent implements Cancellable {

    private final World originWorld;
    private final World targetWorld;

    public EntityLevelChangeEvent(Entity entity, World originWorld, World targetWorld) {
        this.entity = entity;
        this.originWorld = originWorld;
        this.targetWorld = targetWorld;
    }

    public World getOrigin() {
        return originWorld;
    }

    public World getTarget() {
        return targetWorld;
    }
}

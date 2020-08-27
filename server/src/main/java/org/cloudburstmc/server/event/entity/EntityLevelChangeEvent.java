package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.level.Level;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityLevelChangeEvent extends EntityEvent implements Cancellable {

    private final Level originLevel;
    private final Level targetLevel;

    public EntityLevelChangeEvent(Entity entity, Level originLevel, Level targetLevel) {
        this.entity = entity;
        this.originLevel = originLevel;
        this.targetLevel = targetLevel;
    }

    public Level getOrigin() {
        return originLevel;
    }

    public Level getTarget() {
        return targetLevel;
    }
}

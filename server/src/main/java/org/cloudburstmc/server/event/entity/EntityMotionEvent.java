package org.cloudburstmc.server.event.entity;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityMotionEvent extends EntityEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final Vector3f motion;

    public EntityMotionEvent(Entity entity, Vector3f motion) {
        this.entity = entity;
        this.motion = motion;
    }

    public Vector3f getMotion() {
        return this.motion;
    }
}

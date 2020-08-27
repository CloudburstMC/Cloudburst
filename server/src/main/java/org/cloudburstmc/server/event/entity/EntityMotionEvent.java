package org.cloudburstmc.server.event.entity;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityMotionEvent extends EntityEvent implements Cancellable {

    private final Vector3f motion;

    public EntityMotionEvent(Entity entity, Vector3f motion) {
        this.entity = entity;
        this.motion = motion;
    }

    public Vector3f getMotion() {
        return this.motion;
    }
}

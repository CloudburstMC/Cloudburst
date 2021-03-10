package org.cloudburstmc.api.event.entity;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class EntityMotionEvent extends EntityEvent implements Cancellable {

    private final Vector3f motion;

    public EntityMotionEvent(Entity entity, Vector3f motion) {
        this.entity = entity;
        this.motion = motion;
    }

    public Vector3f getMotion() {
        return this.motion;
    }
}

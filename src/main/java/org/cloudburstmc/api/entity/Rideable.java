package org.cloudburstmc.api.entity;

import org.cloudburstmc.api.util.data.MountType;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface Rideable extends Entity {

    /**
     * Mount or Dismounts an Entity from a rideable entity
     *
     * @param entity The target Entity
     * @return {@code true} if the mounting successful
     */
    boolean mount(Entity entity, MountType mode);

    boolean dismount(Entity entity);
}

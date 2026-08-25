package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Projectile;

import static java.util.Objects.requireNonNull;

/**
 * Called before a projectile is added to a level.
 */
public class ProjectileLaunchEvent extends EntitySpawnEvent {

    public ProjectileLaunchEvent(Projectile projectile) {
        super(requireNonNull(projectile, "projectile"));
    }

    @Override
    public Projectile getEntity() {
        return (Projectile) this.entity;
    }
}

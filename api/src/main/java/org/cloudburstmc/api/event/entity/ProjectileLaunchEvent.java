package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Projectile;

import static java.util.Objects.requireNonNull;

/**
 * Fired before a projectile is spawned into a level.
 */
public class ProjectileLaunchEvent extends EntitySpawnEvent {

    /**
     * Creates a projectile launch event.
     *
     * @param projectile the projectile being launched
     */
    public ProjectileLaunchEvent(Projectile projectile) {
        super(requireNonNull(projectile, "projectile"));
    }

    /**
     * Returns the projectile being launched.
     *
     * @return the projectile
     */
    @Override
    public Projectile getEntity() {
        return (Projectile) this.entity;
    }
}

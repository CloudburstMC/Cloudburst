package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.util.BlockHitResult;
import org.cloudburstmc.api.util.EntityHitResult;
import org.cloudburstmc.api.util.HitResult;

import static java.util.Objects.requireNonNull;

/**
 * Fired when a projectile hits an entity or block.
 *
 * <p>Cancelling an entity hit prevents the impact action. Cancelling a block hit
 * prevents the block's projectile-hit reaction, but not the projectile's physical
 * collision or its own impact effects.
 */
public class ProjectileHitEvent extends EntityEvent implements Cancellable {

    private final HitResult hit;

    /**
     * Creates an event for a block or liquid impact.
     *
     * @param projectile the projectile
     * @param hit        the block impact
     */
    public ProjectileHitEvent(Projectile projectile, BlockHitResult hit) {
        this(projectile, (HitResult) hit);
    }

    /**
     * Creates an event for an entity impact.
     *
     * @param projectile the projectile
     * @param hit        the entity impact
     */
    public ProjectileHitEvent(Projectile projectile, EntityHitResult hit) {
        this(projectile, (HitResult) hit);
    }

    private ProjectileHitEvent(Projectile projectile, HitResult hit) {
        this.entity = requireNonNull(projectile, "projectile");
        this.hit = requireNonNull(hit, "hit");
    }

    @Override
    public Projectile getEntity() {
        return (Projectile) this.entity;
    }

    /**
     * Returns the block or entity impact.
     *
     * @return the impact result
     */
    public HitResult getHitResult() {
        return this.hit;
    }
}

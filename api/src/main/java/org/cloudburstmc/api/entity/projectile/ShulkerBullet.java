package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;

/**
 * A projectile fired by a shulker.
 */
public interface ShulkerBullet extends Projectile {

    /**
     * Returns the current target, or {@code null} if no eligible target remains.
     */
    @Nullable
    Entity getTarget();

    /**
     * Sets the target or clears it with {@code null}. Targets are not saved.
     *
     * @throws IllegalArgumentException if the target is this bullet, dead, closed,
     *                                  in another level, or a spectator player
     */
    void setTarget(@Nullable Entity target);
}

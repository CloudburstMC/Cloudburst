package org.cloudburstmc.api.entity.projectile;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.function.Consumer;

/**
 * A source capable of launching projectiles.
 */
public interface ProjectileSource {

    /**
     * Launches a projectile in the source's facing direction.
     *
     * @param projectileType the projectile type
     * @param <T>            the projectile entity type
     * @return the launched projectile, or {@code null} if the launch is canceled
     */
    default <T extends Projectile> @Nullable T launchProjectile(EntityType<T> projectileType) {
        return this.launchProjectile(projectileType, null, null);
    }

    /**
     * Launches a projectile with an initial velocity.
     *
     * @param projectileType the projectile type
     * @param velocity       the initial velocity, or {@code null} to use the facing direction
     * @param <T>            the projectile entity type
     * @return the launched projectile, or {@code null} if the launch is canceled
     */
    default <T extends Projectile> @Nullable T launchProjectile(EntityType<T> projectileType, @Nullable Vector3f velocity) {
        return this.launchProjectile(projectileType, velocity, null);
    }

    /**
     * Creates and configures a projectile before it is spawned.
     *
     * @param projectileType the projectile type
     * @param velocity       the initial velocity, or {@code null} to use the facing direction
     * @param configure      an optional callback that runs before spawning
     * @param <T>            the projectile entity type
     * @return the launched projectile, or {@code null} if the launch is canceled
     */
    <T extends Projectile> @Nullable T launchProjectile(EntityType<T> projectileType, @Nullable Vector3f velocity, @Nullable Consumer<? super T> configure);
}

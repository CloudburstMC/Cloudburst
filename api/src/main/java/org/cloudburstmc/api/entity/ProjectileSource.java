package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.vector.Vector3f;

import java.util.function.Consumer;

/**
 * An entity capable of launching projectiles.
 */
public interface ProjectileSource {

    /**
     * Launches a projectile in the source's facing direction.
     *
     * @param type the projectile type
     * @param <T>  the projectile type
     * @return the launched projectile, or {@code null} if the launch is canceled
     */
    default <T extends Projectile> @Nullable T launchProjectile(EntityType<T> type) {
        return this.launchProjectile(type, null, null);
    }

    /**
     * Launches a projectile with an initial velocity.
     *
     * @param type     the projectile type
     * @param velocity the initial velocity, or {@code null} to use the facing direction
     * @param <T>      the projectile type
     * @return the launched projectile, or {@code null} if the launch is canceled
     */
    default <T extends Projectile> @Nullable T launchProjectile(EntityType<T> type, @Nullable Vector3f velocity) {
        return this.launchProjectile(type, velocity, null);
    }

    /**
     * Creates and configures a projectile before it is spawned.
     *
     * @param type         the projectile type
     * @param velocity     the initial velocity, or {@code null} to use the facing direction
     * @param configurator an optional pre-spawn configurator
     * @param <T>          the projectile type
     * @return the launched projectile, or {@code null} if the launch is canceled
     */
    <T extends Projectile> @Nullable T launchProjectile(EntityType<T> type, @Nullable Vector3f velocity, @Nullable Consumer<? super T> configurator);
}

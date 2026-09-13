package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;

/**
 * A projectile with arrow-style damage and critical-hit behavior.
 */
public interface AbstractArrow extends Projectile {

    /**
     * Returns whether this projectile is critical.
     *
     * @return whether the projectile is critical
     */
    boolean isCritical();

    /**
     * Changes whether this projectile is critical.
     *
     * @param critical whether the projectile is critical
     */
    void setCritical(boolean critical);

    /**
     * Marks this projectile as critical.
     */
    default void setCritical() {
        this.setCritical(true);
    }

    /**
     * Returns the projectile's base damage.
     *
     * @return the base damage
     */
    float getDamage();

    /**
     * Changes the projectile's base damage.
     *
     * @param damage the base damage
     */
    void setDamage(float damage);
}

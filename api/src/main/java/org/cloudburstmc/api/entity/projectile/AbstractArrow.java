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

    /**
     * Returns how many additional entities this projectile can pierce.
     * Piercing projectiles also bypass item blocking.
     *
     * @return piercing level, or zero for a non-piercing projectile
     */
    int getPierceLevel();

    /**
     * Changes how many additional entities the projectile can pierce.
     *
     * @param level piercing level between zero and 127
     */
    void setPierceLevel(int level);

    /**
     * Returns who may pick up this projectile after it lands.
     *
     * @return the pickup status
     */
    ArrowPickupStatus getPickupStatus();

    /**
     * Changes who may pick up this projectile after it lands.
     *
     * @param status the pickup status
     */
    void setPickupStatus(ArrowPickupStatus status);
}

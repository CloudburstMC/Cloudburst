package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.potion.PotionType;

/**
 * A projectile carrying potion contents.
 */
public interface ThrownPotion extends Projectile {

    /**
     * Returns the potion contents carried by this projectile.
     *
     * @return potion type
     */
    PotionType getPotionType();

    /**
     * Changes the potion contents carried by this projectile.
     *
     * @param type potion type
     */
    void setPotionType(PotionType type);
}

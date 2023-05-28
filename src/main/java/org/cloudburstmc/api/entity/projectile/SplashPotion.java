package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.potion.PotionType;

public interface SplashPotion extends Projectile {

    @Deprecated
    /**
     * Use {@link #getPotionType()}
     */
    short getPotionId();

    @Deprecated
    /**
     * Use {@link #setPotionType(PotionType)}
     */
    void setPotionId(int potionId);

    PotionType getPotionType();

    void setPotionType(PotionType type);
}

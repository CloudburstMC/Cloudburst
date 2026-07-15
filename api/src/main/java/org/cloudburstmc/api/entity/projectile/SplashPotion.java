package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.potion.PotionType;

public interface SplashPotion extends Projectile {
    PotionType getPotionType();

    void setPotionType(PotionType type);
}

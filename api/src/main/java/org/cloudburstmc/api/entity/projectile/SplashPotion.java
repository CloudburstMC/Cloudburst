package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.potion.PotionType;

public interface SplashPotion extends Projectile {

    short getPotionId();

    void setPotionId(int potionId);

    PotionType getPotionType();

    void setPotionType(PotionType type);
}

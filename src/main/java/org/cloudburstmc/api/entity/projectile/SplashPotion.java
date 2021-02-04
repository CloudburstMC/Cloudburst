package org.cloudburstmc.api.entity.projectile;

import org.cloudburstmc.api.entity.Projectile;

public interface SplashPotion extends Projectile {

    short getPotionId();

    void setPotionId(int potionId);
}

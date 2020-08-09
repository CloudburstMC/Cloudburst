package org.cloudburstmc.server.entity.projectile;

import org.cloudburstmc.server.entity.Projectile;

public interface SplashPotion extends Projectile {

    short getPotionId();

    void setPotionId(int potionId);
}

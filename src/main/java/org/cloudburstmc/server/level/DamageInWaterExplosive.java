package org.cloudburstmc.server.level;
import org.cloudburstmc.api.entity.Explosive;

public class DamageInWaterExplosive implements DamageInWaterExplosiveInterface, Explosive {
    @Override
    public boolean shouldCauseDamageInWater() {
        return true;
    }

    @Override
    public void explode() {

    }
}

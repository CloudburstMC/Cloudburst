package org.cloudburstmc.server.level;

public class NoDamageInWaterExplosive extends ExplosiveA {
    @Override
    public boolean shouldCauseDamageInWater() {
        return false;
    }
}

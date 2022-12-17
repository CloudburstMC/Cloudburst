package org.cloudburstmc.api.entity.misc;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.potion.Effect;

import java.util.List;

public interface AreaEffectCloud extends Entity {
    int getWaitTime();

    void setWaitTime(int waitTime);

    short getPotionId();

    void setPotionId(int potionId);

    void recalculatePotionColor();

    int getPotionColor();

    void setPotionColor(int argp);

    void setPotionColor(int alpha, int red, int green, int blue);

    int getPickupCount();

    void setPickupCount(int pickupCount);

    float getRadiusChangeOnPickup();

    void setRadiusChangeOnPickup(float radiusChangeOnPickup);

    float getRadiusPerTick();

    void setRadiusPerTick(float radiusPerTick);

    long getSpawnTime();

    void setSpawnTime(int spawnTime);

    int getDuration();

    void setDuration(int duration);

    float getRadius();

    void setRadius(float radius);

    int getParticleId();

    void setParticleId(int particleId);

    List<? extends Effect> getCloudEffects();
}

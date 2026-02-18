package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.GlowSquid;
import org.cloudburstmc.api.level.Location;

public class EntityGlowSquid extends EntityWaterAnimal implements GlowSquid {

    public EntityGlowSquid(EntityType<GlowSquid> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public float getWidth() {
        return this.isBaby() ? 0.475f : 0.95f;
    }

    @Override
    public float getHeight() {
        return this.isBaby() ? 0.475f : 0.95f;
    }

    @Override
    public String getName() {
        return "Glow Squid";
    }
}

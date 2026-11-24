package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.HappyGhast;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.entity.data.CloudAgeableMobData;

public class EntityHappyGhast extends EntityAgeableAnimal implements HappyGhast {

    private static final float JUVENILE_SCALE = 0.2375f;

    public EntityHappyGhast(EntityType<HappyGhast> type, Location location) {
        super(type, location, new CloudAgeableMobData(), true);
    }

    @Override
    protected String getDefaultName() {
        return "Happy Ghast";
    }

    @Override
    public float getWidth() {
        return 4.0f;
    }

    @Override
    public float getHeight() {
        return 4.0f;
    }

    @Override
    public float getEyeHeight() {
        return this.isBaby() ? 0.46875f : 2.6f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        if (this.isBaby() && this.getScale() != JUVENILE_SCALE) {
            this.setScale(JUVENILE_SCALE);
        }
    }

    @Override
    protected void onBabyStatusChanged(boolean baby) {
        this.setScale(baby ? JUVENILE_SCALE : 1f);
    }

    @Override
    protected boolean canBreatheUnderwater() {
        return this.isBaby();
    }
}

package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.HappyGhastData;
import org.cloudburstmc.api.entity.passive.HappyGhast;
import org.cloudburstmc.api.level.Location;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntityHappyGhast extends Animal implements HappyGhast {

    private int growingAge;

    public EntityHappyGhast(EntityType<HappyGhast> type, Location location) {
        super(type, location);
    }

    @Override
    public String getName() {
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
        this.setMaxHealth(20);
    }

    @Override
    protected void applyData(EntityData data) {
        super.applyData(data);
        this.setAge(((HappyGhastData) data).getAge());
    }

    @Override
    protected void fillData(EntityData data) {
        super.fillData(data);
        ((HappyGhastData) data).setAge(this.growingAge);
    }

    @Override
    public int getAge() {
        return this.growingAge;
    }

    @Override
    public void setAge(int age) {
        boolean wasBaby = this.isBaby();
        this.growingAge = age;
        this.data.setFlag(BABY, age < 0);

        if (wasBaby != this.isBaby()) {
            this.setScale(this.isBaby() ? 0.2375f : 1.0f);
        }
    }

    @Override
    protected boolean canBreatheUnderwater() {
        return this.isBaby();
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        boolean updated = super.entityBaseTick(tickDiff);
        if (!this.isAlive() || tickDiff <= 0 || this.growingAge == 0) {
            return updated;
        }

        this.setAge(this.growingAge < 0
                ? (int) Math.min(0L, (long) this.growingAge + tickDiff)
                : (int) Math.max(0L, (long) this.growingAge - tickDiff));
        return true;
    }
}

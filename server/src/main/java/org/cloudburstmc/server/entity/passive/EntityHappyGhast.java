package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.data.EntityData;
import org.cloudburstmc.api.entity.data.HappyGhastData;
import org.cloudburstmc.api.entity.passive.HappyGhast;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntityHappyGhast extends Animal implements HappyGhast {

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
    protected Class<? extends EntityData> minimumDataClass() {
        return HappyGhastData.class;
    }

    @Override
    protected HappyGhastData data() {
        return (HappyGhastData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        // Age is a DTO int, but setting it syncs the baby metadata and the
        // baby scale, exactly as the old applyData did: it runs before the
        // living-state load below, matching the old applyData-before-loadCustom
        // order. An absent key resets to today's default of 0. The base Scale
        // hook already ran in loadBaseCustom, so a baby's derived scale still
        // wins over an explicit tag scale.
        this.setAge(getInt(tag, "Age", 0));
        super.loadCustom(tag);
    }

    @Override
    public int getAge() {
        return this.data().getAge();
    }

    @Override
    public void setAge(int age) {
        boolean wasBaby = this.isBaby();
        this.data().setAge(age);
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
        if (!this.isAlive() || tickDiff <= 0 || this.getAge() == 0) {
            return updated;
        }

        this.setAge(this.getAge() < 0
                ? (int) Math.min(0L, (long) this.getAge() + tickDiff)
                : (int) Math.max(0L, (long) this.getAge() - tickDiff));
        return true;
    }
}

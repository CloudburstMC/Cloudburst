package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityAgeable;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityCreature;
import org.cloudburstmc.server.entity.passive.DeprecatedVillager;
import org.cloudburstmc.server.level.Location;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.BABY;

public class EntityDeprecatedVillager extends EntityCreature implements DeprecatedVillager, EntityAgeable {

    public EntityDeprecatedVillager(EntityType<DeprecatedVillager> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.3f;
        }
        return 0.6f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.9f;
        }
        return 1.8f;
    }

    @Override
    public String getName() {
        return "Villager";
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }

    public void setBaby(boolean baby) {
        this.data.setFlag(BABY, baby);
        this.setScale(baby ? 0.5f : 1);
    }
}

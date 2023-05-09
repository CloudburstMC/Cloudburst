package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityAgeable;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Villager;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.EntityCreature;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.VARIANT;
import static org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag.BABY;

/**
 * Created by Pub4Game on 21.06.2016.
 */
public class EntityVillager extends EntityCreature implements Villager, EntityAgeable {

    public static final int PROFESSION_FARMER = 0;
    public static final int PROFESSION_LIBRARIAN = 1;
    public static final int PROFESSION_PRIEST = 2;
    public static final int PROFESSION_BLACKSMITH = 3;
    public static final int PROFESSION_BUTCHER = 4;
    public static final int PROFESSION_GENERIC = 5;

    public EntityVillager(EntityType<Villager> type, Location location) {
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

        this.setProfession(PROFESSION_GENERIC);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForInt("Profession", this::setProfession);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putInt("Profession", this.getProfession());
    }

    public int getProfession() {
        return this.data.get(VARIANT);
    }

    public void setProfession(int profession) {
        this.data.set(VARIANT, profession);
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }
}

package org.cloudburstmc.server.entity.impl.passive;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.entity.EntityData;
import org.cloudburstmc.server.entity.EntityAgeable;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.impl.EntityCreature;
import org.cloudburstmc.server.entity.passive.Villager;
import org.cloudburstmc.server.world.Location;

import static com.nukkitx.protocol.bedrock.data.entity.EntityFlag.BABY;

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
        return this.data.getInt(EntityData.VARIANT);
    }

    public void setProfession(int profession) {
        this.data.setInt(EntityData.VARIANT, profession);
    }

    @Override
    public boolean isBaby() {
        return this.data.getFlag(BABY);
    }
}

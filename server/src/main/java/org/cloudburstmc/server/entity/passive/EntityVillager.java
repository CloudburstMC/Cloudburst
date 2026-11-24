package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Villager;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.CloudMob;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;

import java.util.HashSet;
import java.util.Set;

import static org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes.VARIANT;

/**
 * Created by Pub4Game on 21.06.2016.
 */
public class EntityVillager extends CloudMob implements Villager {

    public static final int PROFESSION_FARMER = 0;
    public static final int PROFESSION_LIBRARIAN = 1;
    public static final int PROFESSION_PRIEST = 2;
    public static final int PROFESSION_BLACKSMITH = 3;
    public static final int PROFESSION_BUTCHER = 4;
    public static final int PROFESSION_GENERIC = 5;

    public EntityVillager(EntityType<Villager> type, Location location) {
        super(type, location);
        this.data.set(VARIANT, PROFESSION_GENERIC);
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPos) {
        return MobNameTagInteraction.tryApply(this, player, item);
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
    protected String getDefaultName() {
        return "Villager";
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("Profession")) {
            this.setProfession(getInt(tag, "Profession", this.getProfession()));
        }
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        tag.putInt("Profession", this.getProfession());
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add("Profession");
        return Set.copyOf(keys);
    }

    public int getProfession() {
        return this.data.require(VARIANT);
    }

    public void setProfession(int profession) {
        this.data.set(VARIANT, profession);
    }

}

package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Donkey;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;

/**
 * @author PikyCZ
 */
public class EntityDonkey extends Animal implements Donkey {

    public EntityDonkey(EntityType<Donkey> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        if (this.isBaby()) {
            return 0.6982f;
        }
        return 1.3965f;
    }

    @Override
    public float getHeight() {
        if (this.isBaby()) {
            return 0.8f;
        }
        return 1.6f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{CloudItemRegistry.get().getItem(ItemTypes.LEATHER)};
    }
}

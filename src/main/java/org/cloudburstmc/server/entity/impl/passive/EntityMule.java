package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Mule;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityMule extends Animal implements Mule {

    public EntityMule(EntityType<Mule> type, Location location) {
        super(type, location);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemTypes.LEATHER)};
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
}

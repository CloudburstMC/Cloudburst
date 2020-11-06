package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Dolphin;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityDolphin extends Animal implements Dolphin {

    public EntityDolphin(EntityType<Dolphin> type, Location location) {
        super(type, location);
    }

    public String getName() {
        return "Dolphin";
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        return 0.6f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemTypes.FISH)};
    }
}

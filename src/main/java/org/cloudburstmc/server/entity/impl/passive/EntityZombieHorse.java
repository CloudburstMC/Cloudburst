package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.Smiteable;
import org.cloudburstmc.server.entity.passive.ZombieHorse;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * @author PikyCZ
 */
public class EntityZombieHorse extends Animal implements ZombieHorse, Smiteable {

    public EntityZombieHorse(EntityType<ZombieHorse> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    public float getHeight() {
        return 1.6f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(15);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemTypes.ROTTEN_FLESH)};
    }

    @Override
    public boolean isUndead() {
        return true;
    }

}

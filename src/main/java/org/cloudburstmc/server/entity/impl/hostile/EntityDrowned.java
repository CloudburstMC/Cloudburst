package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.Smiteable;
import org.cloudburstmc.api.entity.hostile.Drowned;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityDrowned extends EntityHostile implements Drowned, Smiteable {

    public EntityDrowned(EntityType<Drowned> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.95f;
    }

    @Override
    public String getName() {
        return "Drowned";
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

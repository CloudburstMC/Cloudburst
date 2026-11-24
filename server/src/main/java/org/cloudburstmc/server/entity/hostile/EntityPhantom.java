package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Phantom;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * Created by PetteriM1
 */
public class EntityPhantom extends EntityHostile implements Phantom {

    public EntityPhantom(EntityType<Phantom> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(20);
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        return 0.5f;
    }

    @Override
    protected String getDefaultName() {
        return "Phantom";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.PHANTOM_MEMBRANE).build()};
    }


}

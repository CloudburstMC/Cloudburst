package org.cloudburstmc.server.entity.passive;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.passive.Squid;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.utils.data.DyeColor;

/**
 * @author PikyCZ
 */
public class EntitySquid extends EntityWaterAnimal implements Squid {

    public static final int NETWORK_ID = 17;

    public EntitySquid(EntityType<Squid> type, Location location) {
        super(type, location);
    }

    @Override
    public float getWidth() {
        return 0.8f;
    }

    @Override
    public float getHeight() {
        return 0.8f;
    }

    @Override
    public void initEntity() {
        super.initEntity();
        this.setMaxHealth(10);
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.get(ItemTypes.DYE, DyeColor.BLACK.getDyeData())};
    }
}

package org.cloudburstmc.server.entity.impl.passive;

import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.passive.Squid;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.world.Location;
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
    public Item[] getDrops() {
        return new Item[]{Item.get(ItemIds.DYE, DyeColor.BLACK.getDyeData())};
    }
}

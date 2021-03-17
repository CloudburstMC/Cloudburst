package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.Arthropod;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Spider;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.item.ItemTypes;

/**
 * @author PikyCZ
 */
public class EntitySpider extends EntityHostile implements Spider, Arthropod {

    public EntitySpider(EntityType<Spider> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setMaxHealth(16);
    }

    @Override
    public float getWidth() {
        return 1.4f;
    }

    @Override
    public float getHeight() {
        return 0.9f;
    }

    @Override
    public String getName() {
        return "Spider";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{CloudItemRegistry.get().getItem(ItemTypes.STRING), CloudItemRegistry.get().getItem(ItemTypes.SPIDER_EYE)};
    }
}

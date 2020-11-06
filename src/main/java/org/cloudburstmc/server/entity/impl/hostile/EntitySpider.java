package org.cloudburstmc.server.entity.impl.hostile;

import org.cloudburstmc.server.entity.Arthropod;
import org.cloudburstmc.server.entity.EntityType;
import org.cloudburstmc.server.entity.hostile.Spider;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Location;

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
        return new ItemStack[]{ItemStack.get(ItemTypes.STRING), ItemStack.get(ItemTypes.SPIDER_EYE)};
    }
}

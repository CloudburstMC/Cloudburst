package org.cloudburstmc.server.entity.hostile;

import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.hostile.Spider;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;

/**
 * @author PikyCZ
 */
public class EntitySpider extends EntityHostile implements Spider {

    public EntitySpider(EntityType<? extends Spider> type, Location location) {
        super(type, location);
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(16);
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
    protected String getDefaultName() {
        return "Spider";
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.STRING).build(), ItemStack.builder().itemType(ItemTypes.SPIDER_EYE).build()};
    }
}

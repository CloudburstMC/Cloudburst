package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.impl.EntityLiving;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityDeathEvent extends EntityEvent {

    private ItemStack[] drops;

    public EntityDeathEvent(EntityLiving entity) {
        this(entity, new ItemStack[0]);
    }

    public EntityDeathEvent(EntityLiving entity, ItemStack[] drops) {
        this.entity = entity;
        this.drops = drops;
    }

    public ItemStack[] getDrops() {
        return drops;
    }

    public void setDrops(ItemStack[] drops) {
        if (drops == null) {
            drops = new ItemStack[0];
        }

        this.drops = drops;
    }
}

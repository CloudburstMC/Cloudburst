package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityDeathEvent extends EntityEvent {

    private ItemStack[] drops;

    public EntityDeathEvent(Living entity) {
        this(entity, new ItemStack[0]);
    }

    public EntityDeathEvent(Living entity, ItemStack[] drops) {
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

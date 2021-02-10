package org.cloudburstmc.api.event.entity;

import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class EntityArmorChangeEvent extends EntityEvent implements Cancellable {

    private final ItemStack oldItem;
    private ItemStack newItem;
    private final int slot;

    public EntityArmorChangeEvent(Entity entity, ItemStack oldItem, ItemStack newItem, int slot) {
        this.entity = entity;
        this.oldItem = oldItem;
        this.newItem = newItem;
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getNewItem() {
        return newItem;
    }

    public void setNewItem(ItemStack newItem) {
        this.newItem = newItem;
    }

    public ItemStack getOldItem() {
        return oldItem;
    }
}

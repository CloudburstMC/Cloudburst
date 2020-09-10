package org.cloudburstmc.server.event.entity;

import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.item.ItemStack;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class EntityInventoryChangeEvent extends EntityEvent implements Cancellable {

    private final ItemStack oldItem;
    private ItemStack newItem;
    private final int slot;

    public EntityInventoryChangeEvent(Entity entity, ItemStack oldItem, ItemStack newItem, int slot) {
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

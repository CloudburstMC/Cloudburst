package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.inventory.Inventory;

/**
 * author: boybook
 * Nukkit Project
 */
public class InventoryClickEvent extends InventoryEvent implements Cancellable {

    private final int slot;
    private final ItemStack sourceItem;
    private final ItemStack heldItem;
    private final Player player;

    public InventoryClickEvent(Player who, Inventory inventory, int slot, ItemStack sourceItem, ItemStack heldItem) {
        super(inventory);
        this.slot = slot;
        this.sourceItem = sourceItem;
        this.heldItem = heldItem;
        this.player = who;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getSourceItem() {
        return sourceItem;
    }

    public ItemStack getHeldItem() {
        return heldItem;
    }

    public Player getPlayer() {
        return player;
    }
}
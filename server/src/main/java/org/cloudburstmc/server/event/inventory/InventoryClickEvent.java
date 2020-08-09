package org.cloudburstmc.server.event.inventory;

import org.cloudburstmc.server.event.Cancellable;
import org.cloudburstmc.server.event.HandlerList;
import org.cloudburstmc.server.inventory.Inventory;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.player.Player;

/**
 * author: boybook
 * Nukkit Project
 */
public class InventoryClickEvent extends InventoryEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final int slot;
    private final Item sourceItem;
    private final Item heldItem;
    private final Player player;

    public InventoryClickEvent(Player who, Inventory inventory, int slot, Item sourceItem, Item heldItem) {
        super(inventory);
        this.slot = slot;
        this.sourceItem = sourceItem;
        this.heldItem = heldItem;
        this.player = who;
    }

    public int getSlot() {
        return slot;
    }

    public Item getSourceItem() {
        return sourceItem;
    }

    public Item getHeldItem() {
        return heldItem;
    }

    public Player getPlayer() {
        return player;
    }
}
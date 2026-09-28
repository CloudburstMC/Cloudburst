package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called before a pick action selects or moves an item in the player's inventory.
 * Cancelling leaves the inventory and hotbar selection unchanged. Missing items
 * are created only in creative mode.
 */
public abstract class PlayerPickItemEvent extends PlayerEvent implements Cancellable {
    private final boolean includeData;
    private ItemStack item;
    private int targetSlot;
    private int sourceSlot;

    /**
     * Creates a pick action with its resolved item.
     *
     * @param player      the player performing the pick action
     * @param item        the item to select or create
     * @param includeData whether supported target-specific data was requested in creative mode
     * @param targetSlot  the destination hotbar slot, from 0 to 8
     * @param sourceSlot  the source inventory slot, from 0 to 35, or -1 to create the item
     */
    protected PlayerPickItemEvent(Player player, ItemStack item, boolean includeData, int targetSlot, int sourceSlot) {
        super(Objects.requireNonNull(player, "player"));
        this.item = Objects.requireNonNull(item, "item");
        this.includeData = includeData;
        this.targetSlot = requireTargetSlot(targetSlot);
        this.sourceSlot = requireSourceSlot(sourceSlot);
    }

    /**
     * Returns the resolved picked item. This is the stack created when the source
     * slot is -1. An overridden source slot may move a different existing stack.
     *
     * @return the picked item
     */
    public ItemStack getItem() {
        return this.item;
    }

    /**
     * Changes the item the pick action will search for or create.
     * Recalculates the source slot using the player's current inventory while keeping
     * the target slot unchanged. Set the source slot afterwards to override that search.
     *
     * @param item the picked item, or {@link ItemStack#EMPTY} to suppress selection
     */
    public void setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
        this.sourceSlot = item.isEmpty() ? -1 : this.getPlayer().getInventory().first(item);
    }

    /**
     * Returns whether the player requested supported target-specific data.
     * The request is honored only in creative mode and does not guarantee that
     * the target has additional data to include.
     *
     * @return whether additional data was requested
     */
    public boolean isIncludeData() {
        return this.includeData;
    }

    /**
     * Returns the hotbar slot to select after applying the pick action.
     *
     * @return the destination slot, from 0 to 8
     */
    public int getTargetSlot() {
        return this.targetSlot;
    }

    /**
     * Changes the destination hotbar slot. An existing source stack is swapped
     * with this slot. A newly created item displaces its contents into an empty
     * inventory slot when one is available. In a full creative inventory, it
     * replaces the target stack.
     *
     * @param targetSlot the destination slot, from 0 to 8
     * @throws IllegalArgumentException if the slot is outside the hotbar
     */
    public void setTargetSlot(int targetSlot) {
        this.targetSlot = requireTargetSlot(targetSlot);
    }

    /**
     * Returns the inventory slot whose stack will be moved into the hotbar.
     *
     * @return the source slot, from 0 to 35, or -1 to create the picked item
     */
    public int getSourceSlot() {
        return this.sourceSlot;
    }

    /**
     * Changes which existing stack is moved into the hotbar. The selected source
     * may contain a different item from {@link #getItem()}. A source of -1 creates
     * the picked item only in creative mode. An empty source suppresses the action.
     *
     * @param sourceSlot the inventory slot, from 0 to 35, or -1 to create the item
     * @throws IllegalArgumentException if the slot is outside the inventory range
     */
    public void setSourceSlot(int sourceSlot) {
        this.sourceSlot = requireSourceSlot(sourceSlot);
    }

    private static int requireTargetSlot(int targetSlot) {
        if (targetSlot < 0 || targetSlot >= 9) {
            throw new IllegalArgumentException("Target slot must be between 0 and 8");
        }
        return targetSlot;
    }

    private static int requireSourceSlot(int sourceSlot) {
        if (sourceSlot < -1 || sourceSlot >= 36) {
            throw new IllegalArgumentException("Source slot must be between -1 and 35");
        }
        return sourceSlot;
    }
}

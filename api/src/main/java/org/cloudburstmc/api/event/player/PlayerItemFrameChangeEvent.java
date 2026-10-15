package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.blockentity.ItemFrame;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Fired before a player inserts, rotates, or removes a displayed item.
 * Removal includes hits from projectiles owned by the player.
 * Cancelling leaves the frame and held item unchanged.
 */
public class PlayerItemFrameChangeEvent extends PlayerEvent implements Cancellable {

    private final ItemFrame itemFrame;
    private final ItemFrameChangeAction action;
    private ItemStack itemStack;

    /**
     * @param player    the interacting player
     * @param itemFrame the affected frame
     * @param itemStack the item being inserted, rotated, or removed
     * @param action    the proposed change
     */
    public PlayerItemFrameChangeEvent(Player player, ItemFrame itemFrame, ItemStack itemStack, ItemFrameChangeAction action) {
        super(player);
        this.itemFrame = Objects.requireNonNull(itemFrame, "itemFrame");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
        this.action = Objects.requireNonNull(action, "action");
    }

    /**
     * @return the affected frame
     */
    public ItemFrame getItemFrame() {
        return this.itemFrame;
    }

    /**
     * @return the proposed change
     */
    public ItemFrameChangeAction getAction() {
        return this.action;
    }

    /**
     * @return the item being inserted, rotated, or removed
     */
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    /**
     * Changes the displayed item for placement or rotation, or the dropped item for removal.
     * Placement still consumes one held item outside creative mode.
     *
     * @param itemStack the replacement item, or an empty stack
     */
    public void setItemStack(ItemStack itemStack) {
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
    }
}

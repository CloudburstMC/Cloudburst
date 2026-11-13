package org.cloudburstmc.api.event.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.block.BlockExpEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called when a player takes items from a furnace output slot.
 */
public class FurnaceExtractEvent extends BlockExpEvent {

    private final Player player;
    private final ItemStack itemStack;

    public FurnaceExtractEvent(Player player, Block block, ItemStack itemStack, int experience) {
        super(block, experience);
        this.player = Objects.requireNonNull(player, "player");
        this.itemStack = Objects.requireNonNull(itemStack, "itemStack");
    }

    /**
     * @return the player taking the items
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * @return the type taken from the output slot
     */
    public ItemType getItemType() {
        return this.itemStack.getType();
    }

    /**
     * @return the exact stack taken from the output slot
     */
    public ItemStack getItemStack() {
        return this.itemStack;
    }

    /**
     * @return the number of items taken
     */
    public int getItemAmount() {
        return this.itemStack.getCount();
    }
}

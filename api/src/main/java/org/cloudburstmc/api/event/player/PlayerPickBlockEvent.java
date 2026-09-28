package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.Objects;

/**
 * Called when a player picks a block with an item representation.
 */
public class PlayerPickBlockEvent extends PlayerPickItemEvent {
    private final Block block;

    /**
     * Creates a block pick action before its inventory changes are applied.
     *
     * @param player      the player performing the pick action
     * @param block       the block being picked
     * @param item        the resolved item
     * @param includeData whether supported block data was requested in creative mode
     * @param targetSlot  the destination hotbar slot, from 0 to 8
     * @param sourceSlot  the source inventory slot, from 0 to 35, or -1 to create the item
     */
    public PlayerPickBlockEvent(Player player, Block block, ItemStack item, boolean includeData, int targetSlot, int sourceSlot) {
        super(player, item, includeData, targetSlot, sourceSlot);
        this.block = Objects.requireNonNull(block, "block");
    }

    /**
     * Returns the block being picked.
     *
     * @return the picked block
     */
    public Block getBlock() {
        return this.block;
    }
}

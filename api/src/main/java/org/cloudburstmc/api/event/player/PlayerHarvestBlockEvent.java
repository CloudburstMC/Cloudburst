package org.cloudburstmc.api.event.player;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Called when a player harvests items from a block without breaking it.
 */
public class PlayerHarvestBlockEvent extends PlayerEvent implements Cancellable {

    private final Block harvestedBlock;
    private final List<ItemStack> itemsHarvested;

    /**
     * Creates a block-harvest event.
     *
     * @param player         the player harvesting the block
     * @param harvestedBlock the block being harvested
     * @param itemsHarvested the items that will be produced
     */
    public PlayerHarvestBlockEvent(Player player, Block harvestedBlock, List<ItemStack> itemsHarvested) {
        super(player);
        this.harvestedBlock = Objects.requireNonNull(harvestedBlock, "harvestedBlock");
        this.itemsHarvested = new ArrayList<>(Objects.requireNonNull(itemsHarvested, "itemsHarvested"));
    }

    /**
     * Returns the block being harvested.
     *
     * @return the harvested block
     */
    public Block getHarvestedBlock() {
        return this.harvestedBlock;
    }

    /**
     * Returns the mutable list of items that will be produced.
     *
     * @return the harvested items
     */
    public List<ItemStack> getItemsHarvested() {
        return this.itemsHarvested;
    }
}

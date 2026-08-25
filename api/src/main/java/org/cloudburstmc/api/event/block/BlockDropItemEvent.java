package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.entity.misc.DroppedItem;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Called after a player breaks a block and its item drops have been resolved.
 */
public class BlockDropItemEvent extends BlockEvent implements Cancellable {

    private final Player player;
    private final BlockState blockState;
    private final List<DroppedItem> items;

    /**
     * Creates an event for the resolved item drops of a broken block.
     *
     * @param block the block at the broken position
     * @param blockState the state that was broken
     * @param player the player that broke the block
     * @param items the mutable item drops
     */
    public BlockDropItemEvent(Block block, BlockState blockState, Player player, List<DroppedItem> items) {
        super(block);
        this.blockState = requireNonNull(blockState, "blockState");
        this.player = requireNonNull(player, "player");
        this.items = requireNonNull(items, "items");
    }

    /**
     * Returns the block state before it was broken.
     *
     * @return the state that was broken
     */
    public BlockState getBlockState() {
        return this.blockState;
    }

    /**
     * Returns the player that broke the block.
     *
     * @return the player that broke the block
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns the mutable item drops caused by the block break.
     *
     * @return the mutable item drops
     */
    public List<DroppedItem> getItems() {
        return this.items;
    }
}

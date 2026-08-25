package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Called when a player breaks a block.
 */
public class BlockBreakEvent extends BlockExpEvent implements Cancellable {

    private final Player player;
    private boolean dropItems = true;

    /**
     * Creates an event for a player breaking a block.
     *
     * @param block the block being broken
     * @param player the player breaking the block
     */
    public BlockBreakEvent(Block block, Player player) {
        super(block, 0);
        this.player = requireNonNull(player, "player");
    }

    /**
     * Returns the player breaking the block.
     *
     * @return the player breaking the block
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Returns whether the block's normal item drops will be produced.
     * When this is {@code false}, {@link BlockDropItemEvent} is not fired.
     *
     * @return whether normal item drops will be produced
     */
    public boolean isDropItems() {
        return this.dropItems;
    }

    /**
     * Sets whether the block's normal item drops should be produced.
     * When set to {@code false}, {@link BlockDropItemEvent} is not fired.
     *
     * @param dropItems whether normal item drops should be produced
     */
    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }
}

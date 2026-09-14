package org.cloudburstmc.api.event.block;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockChange;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Called before changes caused by fertilizing a block are applied.
 */
public class BlockFertilizeEvent extends BlockEvent implements Cancellable {

    private final @Nullable Player player;
    private final List<BlockChange> blocks;

    /**
     * Creates a fertilization event.
     *
     * @param block  the block being fertilized
     * @param player the player that caused the fertilization, or {@code null}
     * @param blocks the proposed block changes
     */
    public BlockFertilizeEvent(Block block, @Nullable Player player, List<BlockChange> blocks) {
        super(block);
        this.player = player;
        this.blocks = new ArrayList<>(Objects.requireNonNull(blocks, "blocks"));
    }

    /**
     * Gets the player that caused the fertilization.
     *
     * @return the player, or {@code null} for a non-player cause
     */
    public @Nullable Player getPlayer() {
        return this.player;
    }

    /**
     * Gets the mutable list of proposed block changes.
     *
     * @return the proposed block changes
     */
    public List<BlockChange> getBlocks() {
        return this.blocks;
    }
}

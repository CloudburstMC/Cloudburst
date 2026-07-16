package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.Cancellable;

import java.util.List;

/**
 * Called before a sponge removes liquid from nearby blocks. The level has not yet been changed.
 */
public final class SpongeAbsorbEvent extends BlockEvent implements Cancellable {
    private final List<Block> affectedBlocks;

    public SpongeAbsorbEvent(Block sponge, List<Block> affectedBlocks) {
        super(sponge);
        this.affectedBlocks = affectedBlocks;
    }

    /**
     * Returns the mutable list of blocks from which liquid will be removed. Removing a block from
     * this list prevents its liquid from being removed. Added blocks are also considered.
     *
     * @return the affected blocks
     */
    public List<Block> getAffectedBlocks() {
        return this.affectedBlocks;
    }
}

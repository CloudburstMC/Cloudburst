package org.cloudburstmc.api.event.block;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockReplacement;
import org.cloudburstmc.api.event.Cancellable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fired before a sponge absorbs nearby water. Cancellation prevents all proposed changes.
 */
public class SpongeAbsorbEvent extends BlockEvent implements Cancellable {

    private final List<BlockReplacement> blocks;

    /**
     * @param sponge the sponge absorbing water
     * @param blocks the proposed states after absorption
     */
    public SpongeAbsorbEvent(Block sponge, List<BlockReplacement> blocks) {
        super(sponge);
        this.blocks = new ArrayList<>(Objects.requireNonNull(blocks, "blocks"));
    }

    /**
     * Returns the mutable list of proposed replacements. The level still contains the
     * original states. Removing an entry prevents that change. Replacing an entry changes
     * the states applied at that position.
     *
     * @return the proposed replacements
     */
    public List<BlockReplacement> getBlocks() {
        return this.blocks;
    }
}

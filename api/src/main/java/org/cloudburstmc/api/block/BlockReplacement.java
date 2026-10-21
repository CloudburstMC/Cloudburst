package org.cloudburstmc.api.block;

import java.util.Objects;

/**
 * A proposed replacement of both states at a block position.
 * Creating a replacement does not change the level.
 *
 * @param block          the block before the change
 * @param primaryState   the proposed primary state
 * @param secondaryState the proposed coexisting state, or air when none remains
 */
public record BlockReplacement(Block block, BlockState primaryState, BlockState secondaryState) {

    public BlockReplacement {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(primaryState, "primaryState");
        Objects.requireNonNull(secondaryState, "secondaryState");
    }
}

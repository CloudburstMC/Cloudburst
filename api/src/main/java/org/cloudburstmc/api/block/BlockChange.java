package org.cloudburstmc.api.block;

import java.util.Objects;

/**
 * Describes a proposed change to a block.
 *
 * @param block    the block to change
 * @param newState the state to apply
 */
public record BlockChange(Block block, BlockState newState) {

    public BlockChange {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(newState, "newState");
    }
}

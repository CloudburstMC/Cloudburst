package org.cloudburstmc.api.block;

/**
 * Describes what happens when flowing liquid enters a block state.
 */
public enum LiquidReaction {
    /**
     * Removes the block without drops.
     */
    BROKEN,
    /**
     * Breaks the block and produces its normal drops.
     */
    POPPED,
    /**
     * Prevents flowing liquid from entering the block.
     */
    BLOCKING,
    /**
     * Allows flowing liquid to enter without changing the block state.
     */
    NO_REACTION;

    /**
     * @return whether liquid flow removes the block
     */
    public boolean removesBlock() {
        return this == BROKEN || this == POPPED;
    }

    /**
     * @return whether flowing liquid may enter the block
     */
    public boolean allowsFlow() {
        return this == NO_REACTION;
    }
}

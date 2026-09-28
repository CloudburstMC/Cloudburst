package org.cloudburstmc.api.block;

/**
 * Identifies one of the block states stored at a level position.
 */
public enum BlockLayer {
    /**
     * The main block state at the position.
     */
    PRIMARY,
    /**
     * A state that coexists with the primary state, such as contained liquid.
     */
    SECONDARY
}

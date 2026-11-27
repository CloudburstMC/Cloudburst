package org.cloudburstmc.api.event.player;

/**
 * How a player interacted in a {@link PlayerInteractEvent}.
 */
public enum PlayerInteractAction {
    /**
     * The player attacked a block.
     */
    LEFT_CLICK_BLOCK,
    /**
     * The player used a block or an item on a block.
     */
    RIGHT_CLICK_BLOCK,
    /**
     * The player swung at nothing.
     */
    LEFT_CLICK_AIR,
    /**
     * The player used an item without targeting a block.
     */
    RIGHT_CLICK_AIR,
    /**
     * The player stepped on or into a block, such as farmland or a turtle egg.
     */
    PHYSICAL
}

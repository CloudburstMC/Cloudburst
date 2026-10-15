package org.cloudburstmc.api.event.inventory;

/**
 * The action performed during an inventory interaction.
 */
public enum ActionType {
    /**
     * Takes some or all of a stack from a slot.
     */
    TAKE,
    /**
     * Places some or all of a stack into a slot.
     */
    PLACE,
    /**
     * Swaps the contents of two slots.
     */
    SWAP,
    /**
     * Drops items from a slot into the world.
     */
    DROP,
    /**
     * Destroys items in creative mode.
     */
    DESTROY,
    /**
     * Selects an item from the creative inventory.
     */
    CRAFT_CREATIVE,
    /**
     * An action not described by the other values.
     */
    UNKNOWN
}

package org.cloudburstmc.api.event.inventory;

/**
 * The kind of stack transfer performed during an inventory interaction.
 * Describes the amount transferred, not a particular mouse button or input device.
 */
public enum ClickType {
    /**
     * Takes the entire source stack.
     */
    TAKE_ALL,
    /**
     * Takes half the source stack, rounded down.
     */
    TAKE_HALF,
    /**
     * Takes an amount other than the entire stack or half of it.
     */
    TAKE_SOME,
    /**
     * Places the entire source stack.
     */
    PLACE_ALL,
    /**
     * Places a single item from a larger stack.
     */
    PLACE_ONE,
    /**
     * Places more than one item but less than the entire source stack.
     */
    PLACE_SOME,
    /**
     * The interaction is not a take or place transfer, or its kind is unknown.
     */
    UNKNOWN
}

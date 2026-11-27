package org.cloudburstmc.api.event.inventory;

/**
 * How items move in an {@link InventoryMoveItemEvent}.
 */
public enum InventoryMoveItemAction {
    /**
     * Items move between two inventories, such as a hopper pushing or pulling.
     * One side is {@code null} when items move to or from an item entity in the world.
     */
    TRANSFER,
    /**
     * Items are dispensed out of a dispenser or dropper into the world.
     */
    DISPENSE
}

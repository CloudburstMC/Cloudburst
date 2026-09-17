package org.cloudburstmc.api.item.data;

/**
 * Controls how an item stack may be moved within a player's inventory.
 */
public enum ItemLockMode {
    /**
     * Prevents the stack from being moved out of its current slot.
     */
    LOCK_IN_SLOT,
    /**
     * Allows the stack to move between inventory slots but prevents it from leaving the inventory.
     */
    LOCK_IN_INVENTORY
}

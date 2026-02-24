package org.cloudburstmc.api.inventory.view;

/**
 * Represents the 3x3 crafting grid of a crafter block container.
 * Items craft automatically via redstone signal.
 */
public interface CrafterView extends GridView {

    /**
     * Returns whether the given slot (0–8) is disabled (blocked from receiving items).
     *
     * @param slot the grid slot index, 0–8 (row-major: slot 0 = top-left, slot 8 = bottom-right)
     * @return {@code true} if the slot is disabled
     * @throws IndexOutOfBoundsException if {@code slot} is not 0–8
     */
    boolean isSlotDisabled(int slot);

    /**
     * Enables or disables the given slot (0–8).
     *
     * @param slot     the grid slot index, 0–8
     * @param disabled {@code true} to disable the slot, {@code false} to enable it
     * @throws IndexOutOfBoundsException if {@code slot} is not 0–8
     */
    void setSlotDisabled(int slot, boolean disabled);

    /**
     * Returns whether the crafter is currently powered by a redstone signal.
     *
     * @return {@code true} if powered
     */
    boolean isPowered();
}

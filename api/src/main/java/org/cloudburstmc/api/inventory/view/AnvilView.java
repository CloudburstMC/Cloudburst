package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the item slots of an anvil container.
 * Exposes the two input slots, the result slot, the repair cost, the rename text,
 * and the maximum repair cost.
 */
public interface AnvilView extends SlotGroup {

    ItemStack getInput();

    void setInput(ItemStack item);

    ItemStack getMaterial();

    void setMaterial(ItemStack item);

    /**
     * Returns the item currently in the anvil result slot.
     *
     * <p>This slot is computed by the client from the two inputs and the rename text.
     * The server can read or override the result by setting this slot directly.</p>
     *
     * @return the result item
     */
    ItemStack getResult();

    /**
     * Sets the item in the anvil result slot.
     *
     * @param item the result item to place
     */
    void setResult(ItemStack item);

    /**
     * Returns the experience level cost to perform the repair or rename shown in the anvil output slot.
     *
     * @return the repair cost in experience levels
     */
    int getRepairCost();

    /**
     * Sets the experience level cost to perform the repair or rename shown in the anvil output slot.
     *
     * @param cost the repair cost in experience levels
     */
    void setRepairCost(int cost);

    /**
     * Returns the rename text the player has typed into the anvil text field,
     * or {@code null} if the player has not entered any rename text.
     *
     * @return the rename text, or {@code null}
     */
    @Nullable String getRenameText();

    /**
     * Sets the rename text shown in the anvil text field.
     * Pass {@code null} to clear the rename text.
     *
     * @param text the rename text, or {@code null} to clear
     */
    void setRenameText(@Nullable String text);

    /**
     * Returns the maximum experience level cost the player is allowed to pay for an anvil operation.
     * Operations that exceed this limit will be shown as "Too Expensive!" in vanilla.
     *
     * <p>Defaults to 39 (vanilla cap).</p>
     *
     * @return the maximum repair cost in experience levels
     */
    int getMaximumRepairCost();

    /**
     * Sets the maximum experience level cost the player is allowed to pay for an anvil operation.
     *
     * @param cost the maximum repair cost in experience levels; must be non-negative
     * @throws IllegalArgumentException if {@code cost} is negative
     */
    void setMaximumRepairCost(int cost);
}

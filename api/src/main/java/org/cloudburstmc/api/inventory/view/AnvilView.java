package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents the slots and costs of an anvil container.
 */
public interface AnvilView extends SlotGroup {

    /**
     * Returns the item in the left input slot.
     *
     * @return the input item
     */
    ItemStack getInput();

    /**
     * Sets the item in the left input slot.
     *
     * @param item the input item
     */
    void setInput(ItemStack item);

    /**
     * Returns the item in the right material slot.
     *
     * @return the material item
     */
    ItemStack getMaterial();

    /**
     * Sets the item in the right material slot.
     *
     * @param item the material item
     */
    void setMaterial(ItemStack item);

    /**
     * Returns the item currently in the anvil result slot.
     *
     * <p>This slot is server-authoritative. Implementations may recompute it from the input,
     * material, and rename text.</p>
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
     * Returns how many items from the material slot will be consumed when the result is taken.
     *
     * @return the material repair item count
     */
    int getRepairItemCountCost();

    /**
     * Sets how many items from the material slot will be consumed when the result is taken.
     *
     * @param count the material repair item count; must be non-negative
     * @throws IllegalArgumentException if {@code count} is negative
     */
    void setRepairItemCountCost(int count);

    /**
     * Returns the rename text the player has typed into the anvil text field,
     * or {@code null} if the player has not entered any rename text.
     *
     * @return the rename text, or {@code null}
     */
    @Nullable
    String getRenameText();

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
     * <p>Defaults to 40.</p>
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

    /**
     * Returns whether enchantments may be applied above their normal maximum level.
     *
     * @return {@code true} if maximum enchantment levels are ignored
     */
    boolean bypassesEnchantmentLevelRestriction();

    /**
     * Sets whether enchantments may be applied above their normal maximum level.
     *
     * @param bypass {@code true} to ignore maximum enchantment levels
     */
    void bypassEnchantmentLevelRestriction(boolean bypass);
}

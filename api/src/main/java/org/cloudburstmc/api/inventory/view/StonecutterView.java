package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.crafting.StonecuttingRecipe;
import org.cloudburstmc.api.item.ItemStack;

import java.util.List;

/**
 * The input and prepared result of a stonecutter.
 */
public interface StonecutterView extends SlotGroup {

    /**
     * Returns the item in the input slot, or an empty stack.
     */
    ItemStack getInput();

    /**
     * Sets the input item and clears the selection if the input changes.
     *
     * @param item the new input
     */
    void setInput(ItemStack item);

    /**
     * Returns the prepared output for one cut, or an empty stack.
     */
    ItemStack getResult();

    /**
     * Returns the recipes available for the current input.
     */
    List<StonecuttingRecipe> getRecipes();

    /**
     * Returns the accepted recipe selection, or null if no recipe is selected.
     */
    @Nullable
    StonecuttingRecipe getSelectedRecipe();
}

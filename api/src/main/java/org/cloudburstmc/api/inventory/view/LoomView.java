package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.data.BannerPattern.Type;

/**
 * The input and prepared result of a loom.
 */
public interface LoomView extends SlotGroup {

    /**
     * Returns the banner input.
     */
    ItemStack getBanner();

    /**
     * Sets the banner input.
     */
    void setBanner(ItemStack item);

    /**
     * Returns the dye input.
     */
    ItemStack getDye();

    /**
     * Sets the dye input.
     */
    void setDye(ItemStack item);

    /**
     * Returns the pattern input.
     */
    ItemStack getPattern();

    /**
     * Sets the pattern input.
     */
    void setPattern(ItemStack item);

    /**
     * Returns the prepared banner, or an empty stack.
     */
    ItemStack getResult();

    /**
     * Returns the selected pattern, or null if none is selected.
     */
    @Nullable
    Type getSelectedPattern();
}

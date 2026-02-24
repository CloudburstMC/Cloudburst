package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.inventory.view.BlockCrafterView;
import org.cloudburstmc.api.inventory.view.CreatedOutputView;
import org.cloudburstmc.api.item.ItemStack;

/**
 * Represents an open crafter block screen.
 * The crafter holds a 3x3 crafting grid that fires automatically via redstone.
 */
public interface CrafterScreen extends ContainerScreen {

    /**
     * Returns the crafter block's 3x3 crafting grid section.
     *
     * @return the crafter view
     */
    BlockCrafterView getCrafter();

    /**
     * Returns the output slot view for the crafted result.
     *
     * <p>The output slot is a transient network-side slot that holds the item the
     * crafter would produce given the current grid contents. It is only meaningful
     * while this screen is open; the crafter block entity itself has no persistent
     * output slot.</p>
     *
     * @return the created-output slot group, never {@code null}
     */
    CreatedOutputView getOutputSlot();

    /**
     * Returns the item currently shown in the crafting result slot, or
     * {@link ItemStack#EMPTY} if the current grid contents do not match any recipe.
     *
     * <p>Equivalent to {@code getOutputSlot().getItem(0)}.</p>
     *
     * @return the crafting result item, never {@code null}
     */
    default ItemStack getResult() {
        return getOutputSlot().getItem(0);
    }
}

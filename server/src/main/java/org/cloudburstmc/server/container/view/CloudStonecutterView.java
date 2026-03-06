package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.inventory.view.StonecutterView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link StonecutterView}.
 * Wraps a 2-slot ephemeral container: slot 0 = input, slot 1 = result.
 * This is an ephemeral section with no block entity backing.
 */
public class CloudStonecutterView extends CloudSlotGroupBase implements StonecutterView {

    private int selectedRecipeIndex = -1;

    public CloudStonecutterView(CloudContainer container) {
        super(SlotGroupTypes.STONECUTTER, container);
    }

    @Override
    public ItemStack getInput() {
        return getItem(0);
    }

    @Override
    public void setInput(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getResult() {
        return getItem(1);
    }

    @Override
    public void setResult(ItemStack item) {
        setItem(1, item);
    }

    @Override
    public int getSelectedRecipeIndex() {
        return selectedRecipeIndex;
    }

    @Override
    public void setSelectedRecipeIndex(int index) {
        this.selectedRecipeIndex = index;
    }
}

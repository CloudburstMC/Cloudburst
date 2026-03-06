package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.GrindstoneView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link GrindstoneView}.
 * Wraps a 3-slot ephemeral container: slot 0 = input, slot 1 = additional, slot 2 = result.
 * This is an ephemeral section with no block entity backing.
 */
public class CloudGrindstoneView extends CloudSlotGroupBase implements GrindstoneView {

    public CloudGrindstoneView(CloudContainer container) {
        super(SlotGroupTypes.GRINDSTONE, container);
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
    public ItemStack getAdditional() {
        return getItem(1);
    }

    @Override
    public void setAdditional(ItemStack item) {
        setItem(1, item);
    }

    @Override
    public ItemStack getResult() {
        return getItem(2);
    }

    @Override
    public void setResult(ItemStack item) {
        setItem(2, item);
    }
}

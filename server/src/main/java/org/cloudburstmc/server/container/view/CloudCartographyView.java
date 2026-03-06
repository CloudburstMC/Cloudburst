package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.CartographyView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link CartographyView}.
 * Wraps a 3-slot ephemeral container:
 * slot 0 = map input, slot 1 = paper (additional), slot 2 = result.
 * This is an ephemeral section with no block entity backing.
 */
public class CloudCartographyView extends CloudSlotGroupBase implements CartographyView {

    public CloudCartographyView(CloudContainer container) {
        super(SlotGroupTypes.CARTOGRAPHY, container);
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

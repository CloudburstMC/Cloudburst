package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.inventory.view.SmithingView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link SmithingView}.
 * Wraps a 4-slot ephemeral container:
 * slot 0 = template, slot 1 = base, slot 2 = addition, slot 3 = result.
 * This is an ephemeral section — it has no block entity backing.
 */
public class CloudSmithingView extends CloudSlotGroupBase implements SmithingView {

    public CloudSmithingView(CloudContainer container) {
        super(SlotGroupTypes.SMITHING, container);
    }

    @Override
    public ItemStack getTemplate() {
        return getItem(0);
    }

    @Override
    public void setTemplate(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getBase() {
        return getItem(1);
    }

    @Override
    public void setBase(ItemStack item) {
        setItem(1, item);
    }

    @Override
    public ItemStack getAddition() {
        return getItem(2);
    }

    @Override
    public void setAddition(ItemStack item) {
        setItem(2, item);
    }

    @Override
    public ItemStack getResult() {
        return getItem(3);
    }

    @Override
    public void setResult(ItemStack item) {
        setItem(3, item);
    }
}

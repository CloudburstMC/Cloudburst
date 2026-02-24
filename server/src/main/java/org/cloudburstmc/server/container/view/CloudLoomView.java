package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.LoomView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link LoomView}.
 * Wraps a 4-slot ephemeral container:
 * slot 0 = banner, slot 1 = dye, slot 2 = pattern, slot 3 = result.
 * This is an ephemeral section — it has no block entity backing.
 */
public class CloudLoomView extends CloudSlotGroupBase implements LoomView {

    private int selectedPatternIndex = -1;

    public CloudLoomView(CloudContainer container) {
        super(SlotGroupTypes.LOOM, container);
    }

    @Override
    public ItemStack getBanner() {
        return getItem(0);
    }

    @Override
    public void setBanner(ItemStack item) {
        setItem(0, item);
    }

    @Override
    public ItemStack getDye() {
        return getItem(1);
    }

    @Override
    public void setDye(ItemStack item) {
        setItem(1, item);
    }

    @Override
    public ItemStack getPattern() {
        return getItem(2);
    }

    @Override
    public void setPattern(ItemStack item) {
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

    @Override
    public int getSelectedPatternIndex() {
        return selectedPatternIndex;
    }

    @Override
    public void setSelectedPatternIndex(int index) {
        this.selectedPatternIndex = index;
    }
}

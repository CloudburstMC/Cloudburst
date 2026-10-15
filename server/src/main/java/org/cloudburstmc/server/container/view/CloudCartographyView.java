package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.CartographyView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudCartographyView extends CloudSlotGroupBase implements CartographyView {

    private final Runnable inputChanged;

    public CloudCartographyView(CloudContainer container, Runnable inputChanged) {
        super(SlotGroupTypes.CARTOGRAPHY, container);
        this.inputChanged = inputChanged;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        super.setItem(slot, item);
        if (slot < 2) {
            this.inputChanged.run();
        }
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

package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.GrindstoneView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudGrindstoneView extends CloudSlotGroupBase implements GrindstoneView {

    public static final int INPUT_SLOT = 0;
    public static final int ADDITIONAL_SLOT = 1;
    public static final int RESULT_SLOT = 2;

    private final Runnable inputChanged;

    public CloudGrindstoneView(Runnable inputChanged) {
        super(SlotGroupTypes.GRINDSTONE, new CloudContainer(RESULT_SLOT + 1));
        this.inputChanged = inputChanged;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        if (slot == RESULT_SLOT) {
            throw new IllegalArgumentException("Cannot modify a grindstone result directly");
        }

        ItemStack previous = this.getItem(slot);
        super.setItem(slot, item);
        if (!previous.equals(this.getItem(slot))) {
            CloudContainer.afterSlotWrites(this.inputChanged);
        }
    }

    @Override
    public ItemStack getInput() {
        return this.getItem(INPUT_SLOT);
    }

    @Override
    public void setInput(ItemStack item) {
        this.setItem(INPUT_SLOT, item);
    }

    @Override
    public ItemStack getAdditional() {
        return this.getItem(ADDITIONAL_SLOT);
    }

    @Override
    public void setAdditional(ItemStack item) {
        this.setItem(ADDITIONAL_SLOT, item);
    }

    @Override
    public ItemStack getResult() {
        return this.getItem(RESULT_SLOT);
    }

    public void updateResult(ItemStack item) {
        super.setItem(RESULT_SLOT, item);
    }
}

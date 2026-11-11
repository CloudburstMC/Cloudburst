package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.inventory.view.SmithingView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudSmithingView extends CloudSlotGroupBase implements SmithingView {

    public static final int TEMPLATE_SLOT = 0;
    public static final int BASE_SLOT = 1;
    public static final int ADDITION_SLOT = 2;
    public static final int RESULT_SLOT = 3;

    private final Runnable inputChanged;

    public CloudSmithingView(Runnable inputChanged) {
        super(SlotGroupTypes.SMITHING, new CloudContainer(RESULT_SLOT + 1));
        this.inputChanged = inputChanged;
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        if (slot == RESULT_SLOT) {
            throw new IllegalArgumentException("Cannot modify a smithing result directly");
        }

        ItemStack previous = this.getItem(slot);
        super.setItem(slot, item);
        if (!previous.equals(item)) {
            CloudContainer.afterSlotWrites(this.inputChanged);
        }
    }

    @Override
    public ItemStack getTemplate() {
        return getItem(TEMPLATE_SLOT);
    }

    @Override
    public void setTemplate(ItemStack item) {
        setItem(TEMPLATE_SLOT, item);
    }

    @Override
    public ItemStack getBase() {
        return getItem(BASE_SLOT);
    }

    @Override
    public void setBase(ItemStack item) {
        setItem(BASE_SLOT, item);
    }

    @Override
    public ItemStack getAddition() {
        return getItem(ADDITION_SLOT);
    }

    @Override
    public void setAddition(ItemStack item) {
        setItem(ADDITION_SLOT, item);
    }

    @Override
    public ItemStack getResult() {
        return getItem(RESULT_SLOT);
    }

    public void updateResult(ItemStack item) {
        super.setItem(RESULT_SLOT, item);
    }
}

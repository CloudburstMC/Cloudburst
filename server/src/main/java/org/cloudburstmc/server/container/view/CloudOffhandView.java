package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.CloudEntity;

/**
 * Slot group implementation for the single offhand slot, backed by an entity's offhand container.
 */
public class CloudOffhandView extends CloudEntityContainerView implements OffhandView {

    public CloudOffhandView(CloudEntity holder, CloudContainer container) {
        super(SlotGroupTypes.OFFHAND, holder, container);
    }

    @Override
    public ItemStack getOffhandItem() {
        return getItem(0);
    }

    @Override
    public void setOffhandItem(ItemStack item) {
        setItem(0, item);
    }
}

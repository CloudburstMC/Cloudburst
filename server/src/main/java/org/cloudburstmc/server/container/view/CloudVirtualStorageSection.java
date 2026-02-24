package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * A virtual {@link StorageView} backed by a {@link CloudContainer} with no block-entity counterpart.
 * Used by {@link org.cloudburstmc.server.container.screen.CloudVirtualChestScreen} and
 * {@link org.cloudburstmc.server.container.screen.CloudVirtualDoubleChestScreen}.
 *
 * <p>The {@code slotGroupType} must be passed by the caller so the correct type
 * ({@link org.cloudburstmc.api.inventory.view.SlotGroupTypes#VIRTUAL_CHEST} or
 * {@link org.cloudburstmc.api.inventory.view.SlotGroupTypes#VIRTUAL_DOUBLE_CHEST}) is reported.</p>
 */
public class CloudVirtualStorageSection extends CloudSlotGroupBase implements StorageView {

    public CloudVirtualStorageSection(SlotGroupType<? extends StorageView> slotGroupType, CloudContainer container) {
        super(slotGroupType, container);
    }
}

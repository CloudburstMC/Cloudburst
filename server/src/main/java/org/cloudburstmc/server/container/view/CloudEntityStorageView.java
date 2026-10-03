package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.inventory.view.StorageView;
import org.cloudburstmc.server.container.CloudContainer;

public class CloudEntityStorageView extends CloudSlotGroupBase implements StorageView {

    public CloudEntityStorageView(CloudContainer container) {
        super(SlotGroupTypes.ENTITY_STORAGE, container);
    }
}

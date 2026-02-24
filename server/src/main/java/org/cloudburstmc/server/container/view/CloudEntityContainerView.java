package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.EntitySlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.entity.CloudEntity;

/**
 * Base slot group implementation for slot groups owned by an entity. Adds a {@link CloudEntity} holder
 * reference; used as the parent for armor and offhand views.
 */
public class CloudEntityContainerView extends CloudSlotGroupBase implements EntitySlotGroup {

    protected final CloudEntity holder;

    protected CloudEntityContainerView(SlotGroupType<?> type, CloudEntity holder, CloudContainer container) {
        super(type, container);
        this.holder = holder;
    }

    protected CloudEntityContainerView(SlotGroupType<?> type, CloudEntity holder, CloudContainer container, int size, int offset) {
        super(type, container, size, offset);
        this.holder = holder;
    }

    @Override
    public CloudEntity getHolder() {
        return holder;
    }
}

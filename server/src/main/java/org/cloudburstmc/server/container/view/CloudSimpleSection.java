package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * A concrete, general-purpose slot group backed by a {@link CloudContainer} slice.
 * Used wherever a named slot group type with any view class is needed.
 */
public class CloudSimpleSection extends CloudSlotGroupBase {

    public CloudSimpleSection(SlotGroupType<?> type, CloudContainer container) {
        super(type, container);
    }

    public CloudSimpleSection(SlotGroupType<?> type, CloudContainer container, int size, int offset) {
        super(type, container, size, offset);
    }
}

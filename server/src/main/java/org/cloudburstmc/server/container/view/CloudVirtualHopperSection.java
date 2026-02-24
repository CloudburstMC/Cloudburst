package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.HopperView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * A virtual {@link HopperView} backed by a {@link CloudContainer} with no block-entity counterpart.
 * Used by {@link org.cloudburstmc.server.container.screen.CloudVirtualHopperScreen}.
 */
public class CloudVirtualHopperSection extends CloudSlotGroupBase implements HopperView {

    public CloudVirtualHopperSection(CloudContainer container) {
        super(SlotGroupTypes.VIRTUAL_HOPPER, container);
    }
}

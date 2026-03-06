package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.inventory.view.CraftingTableView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * Server-side implementation of {@link CraftingTableView} for the crafting table.
 * Wraps a 9-slot ephemeral container representing the 3×3 crafting grid.
 * This is an ephemeral section with no block entity backing.
 */
public class CloudCraftingTableView extends CloudSlotGroupBase implements CraftingTableView {

    public CloudCraftingTableView(CloudContainer container) {
        super(SlotGroupTypes.CRAFTING_TABLE, container);
    }
}

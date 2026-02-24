package org.cloudburstmc.api.blockentity;

import org.cloudburstmc.api.inventory.view.BlockCrafterView;

/**
 * A crafter block entity with a 9-slot crafting grid and per-slot disable flags for automated crafting.
 */
public interface Crafter extends BlockEntity, BlockCrafterView {
}
